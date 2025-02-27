import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DaemonServiceImpl implements DaemonService, Runnable {
    private String directoryServiceURL = "//localhost:8888/test";
    private Socket s;
    private DirectoryService obj;
    private Host host;

    public DaemonServiceImpl(int port, Socket s) throws UnknownHostException{
        String ip = InetAddress.getLocalHost().getHostAddress();
        this.host = new Host(ip, port);
        this.s = s;
    }

    @Override
    public void run(){
        try {
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            OutputStream out = s.getOutputStream();
            String resp = reader.readLine();

            String[] fragment_info = resp.split(",");
            
            String fileName = fragment_info[0].trim();
            long startIndex = Long.parseLong(fragment_info[1].trim());
            long stopIndex = Long.parseLong(fragment_info[2].trim());

            RandomAccessFile file = new RandomAccessFile(fileName, "r");
            int bufferSize = 8092;
            byte[] buffer = new byte[bufferSize];
            file.seek(startIndex);

            for (long i = startIndex; i < stopIndex; i += bufferSize){
                if (i + bufferSize <= stopIndex){
                    file.read(buffer, 0, bufferSize);
                    out.write(buffer);
                }
                else{
                    int remainBytes = (int) (stopIndex - i);
                    file.read(buffer, 0, remainBytes);
                    out.write(buffer, 0, remainBytes);
                }
                out.flush();
            }

            reader.close();
            out.close();
            file.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void connect(){
        try{
            obj = (DirectoryService) Naming.lookup(directoryServiceURL);
            obj.connect(host);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect(){
        try{
            obj.disconnect(host);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void addNewFile(String fileName, long pieceSize){
        try{
            File file = new File(fileName);
            FileInputStream fileStream = new FileInputStream(file);
            long fileSize = file.length();

            List<String> checkSums = new ArrayList<>();

            int currentSize = (int) pieceSize;
            for (long i = 0; i < fileSize; i += pieceSize){
                if (i + pieceSize > fileSize){
                    currentSize = (int) (fileSize - i);
                }

                byte[] buffer = new byte[currentSize];
                fileStream.read(buffer);
                String checkSum = getCheckSum(buffer);
                checkSums.add(checkSum);

            }
            obj.addNewFile(host, fileName, fileSize, pieceSize, checkSums);
            fileStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void hostFiles(List<String> fileNames){
        try{
            obj.hostFiles(host, fileNames);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public static String getCheckSum(byte[] fragment) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] result = md.digest(fragment);

        StringBuilder finalString = new StringBuilder("");
        for (byte b: result){
            finalString.append(String.format("%02x", b));
        }
        return finalString.toString();

    }

    public static void main(String args[]) throws IOException{
        int port = Integer.parseInt(args[0]);
        ServerSocket s = new ServerSocket(port);
        System.out.println("Daemon started on port " + port);

        new Thread(() -> {
            try {
                while (true) {
                    Socket socket = s.accept();

                    DaemonServiceImpl daemon = new DaemonServiceImpl(port, socket);
                    daemon.connect();

                    new Thread(daemon).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        DaemonServiceImpl daemon = new DaemonServiceImpl(port, null);
        daemon.connect();
        daemon.addNewFile("TCPProto.txt", 1024);
    }
}
