import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;


public class PiecesUploader implements Runnable {
  private Socket socket;
  private DirectoryService directoryService;
  private Host host;

  public PiecesUploader(Socket socket, DirectoryService directoryService, Host host) {

    this.socket = socket;
    this.directoryService = directoryService;
    this.host = host;
  }

  @Override
  public void run() {
    try {

      directoryService.newTransfer(host);
    } catch (Exception e) {
      System.err.println(e);
    }

    System.out.println("got connect");
    try {

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(socket.getInputStream()));

      OutputStream out = socket.getOutputStream();

      String fileName = reader.readLine();

    System.out.println("req " + fileName);
      RandomAccessFile file = new RandomAccessFile(fileName, "r");

      String range = reader.readLine();
      while (range != null) {
        String[] rangePart = range.split(",");

        long startIndex = Long.parseLong(rangePart[0].trim());
        long stopIndex = Long.parseLong(rangePart[1].trim());

        int bufferSize = 8092;
        byte[] buffer = new byte[bufferSize];
        file.seek(startIndex);


        long currentByteIndex = startIndex;
        int i = 0;
        while (currentByteIndex < stopIndex) {

          int byteSent = file.read(buffer, 0, (int) Math.min(bufferSize, stopIndex - currentByteIndex));
          out.write(buffer, 0, byteSent);
          currentByteIndex += byteSent;
          i++;
        }
        out.flush();
        range = reader.readLine();
      }

      reader.close();
      out.close();
      file.close();

    } catch (Exception e) {
      System.err.println(e);
    }
    try {
      directoryService.finishTransfer(host);
    } catch (Exception e) {
      System.err.println(e);
    }

  }
}
