import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class DownloadServiceImpl extends Thread implements DownloadService {
    private final Host host;
    private DownloadSource source;
    private final DirectoryService obj;
    private final FileInfo fileInfo;
    private final String fileName;

    private DownloadServiceImpl(Builder builder) {
        this.host = builder.host;
        this.source = builder.source;
        this.obj = builder.obj;
        this.fileInfo = builder.fileInfo;
        this.fileName = builder.fileName;
    }

    public static class Builder {
        private Host host;
        private DownloadSource source;
        private DirectoryService obj;
        private FileInfo fileInfo;
        private String fileName;

        public Builder setHost(Host host) { this.host = host; return this; }
        public Builder setSource(DownloadSource source) { this.source = source; return this; }
        public Builder setDirectoryService(DirectoryService obj) { this.obj = obj; return this; }
        public Builder setFileInfo(FileInfo fileInfo) { this.fileInfo = fileInfo; return this; }
        public Builder setFileName(String fileName) { this.fileName = fileName; return this; }

        public DownloadServiceImpl build() {
            return new DownloadServiceImpl(this);
        }
    }

    @Override
    public BufferedInputStream downloadSource(String fileName, int favorableNoSources) throws Exception {
        // Get file info and download sources first
        DownloadInfo downloadInfo = obj.downloadInfo(fileName, favorableNoSources);
        List<DownloadSource> downloadSources = downloadInfo.getSources();
        FileInfo fileInfo = downloadInfo.getFileInfo();

        if (fileInfo == null) {
            throw new IllegalStateException("FileInfo cannot be null!");
        }

        Thread[] threads = new Thread[downloadSources.size()];
        for (int i = 0; i < downloadSources.size(); i++) {
            threads[i] = new Thread(new DownloadServiceImpl.Builder()
                .setHost(host)
                .setSource(downloadSources.get(i))
                .setDirectoryService(obj)
                .setFileInfo(fileInfo)
                .setFileName(fileName)
                .build()
            );
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return null;
    }

    public void run() {
        while (true) {
            try {
                obj.newTransfer(source.getHost());
                Socket socket = new Socket(source.getHost().getAddress(), source.getHost().getPort());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                RandomAccessFile file = new RandomAccessFile("test_" + fileName, "rw");
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                long pieceSize = fileInfo.getPieceSize();
                List<String> checkSums = fileInfo.getCheckSums();
                List<Long> pieces = source.getPieceIndices();

                for (long piece : pieces) {
                    long startIndex = piece * pieceSize;
                    long stopIndex = Math.min(startIndex + pieceSize - 1, fileInfo.getFileSize());

                    String message = fileName + "," + startIndex + "," + stopIndex;
                    writer.println(message);
                    
                    byte[] buffer = new byte[(int) pieceSize];
                    file.seek(startIndex);

                    int bytesRead, accBytesRead = 0;
                    while (accBytesRead < pieceSize && (bytesRead = in.read(buffer, accBytesRead, (int) (pieceSize - accBytesRead))) != -1) {
                        accBytesRead += bytesRead;
                    }

                    String receivedChecksum = DaemonServiceImpl.getCheckSum(buffer);
                    if (!receivedChecksum.equals(checkSums.get((int) piece))) {
                        System.out.println("Error: Checksum mismatch for piece " + piece);
                    }

                    file.write(buffer, 0, accBytesRead);
                }

                file.close();
                in.close();
                writer.close();
                socket.close();
                obj.finishTransfer(source.getHost());
                return;

            } catch (IOException e) {
                System.out.println("Source " + source.getHost().getAddress() + " disconnected. Retrying...");
                try {
                    List<DownloadSource> newSources = obj.failedSources(fileName, List.of(source), 1);
                    if (newSources.isEmpty()) {
                        System.out.println("No new sources available. Aborting...");
                        break;
                    }
                    source = newSources.get(0);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                    break;
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(args[0]);
        String fileName = args[1];

        DirectoryService obj = (DirectoryService) Naming.lookup("//localhost:8888/test");
        DownloadInfo downloadInfo = obj.downloadInfo(fileName, 1);
        FileInfo fileInfo = downloadInfo.getFileInfo();


        DownloadServiceImpl service = new DownloadServiceImpl.Builder()
            .setHost(new Host(InetAddress.getLocalHost().getHostAddress(), port))
            .setDirectoryService(obj)
            .setFileInfo(fileInfo)
            .setFileName(fileName)
            .build();

        service.downloadSource(fileName, 1);
    }
}
