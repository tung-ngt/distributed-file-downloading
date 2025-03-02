import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

public class PiecesUploader implements Runnable {
  private Socket socket;
  private DirectoryService directoryService;
  private Host host;
  private String dataFolder;
  private double checksumFailPercentage;
  private double disconnectFailPercentage;
  private long networkLatency;
  private ConnectionCountMonitor connectionCountMonitor;
  private boolean linearSlowdown;

  public PiecesUploader(Socket socket, DirectoryService directoryService, Host host, String dataFolder,
      double checksumFailPercentage, double disconnectFailPercentage, long networkLatency, boolean linearSlowdown, ConnectionCountMonitor connectionCountMonitor) {
    this.socket = socket;
    this.directoryService = directoryService;
    this.host = host;
    this.dataFolder = dataFolder;
    this.checksumFailPercentage = checksumFailPercentage;
    this.disconnectFailPercentage = disconnectFailPercentage;
    this.networkLatency = networkLatency;
    this.connectionCountMonitor = connectionCountMonitor;
    this.linearSlowdown = linearSlowdown;
  }

  @Override
  public void run() {
    try {
      directoryService.newTransfer(host);
      connectionCountMonitor.increment();
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
      RandomAccessFile file = new RandomAccessFile(dataFolder + "/" + fileName, "r");

      String range = reader.readLine();
      while (range != null) {

        String[] rangePart = range.split(",");

        long startIndex = Long.parseLong(rangePart[0].trim());
        long stopIndex = Long.parseLong(rangePart[1].trim());

        int bufferSize = 8092;
        byte[] buffer = new byte[bufferSize];

        if (networkLatency > 0) {
          if (linearSlowdown) {
            Thread.sleep(networkLatency * connectionCountMonitor.get());
          } else {
            Thread.sleep(networkLatency);
          }
        }

        if (Math.random() < disconnectFailPercentage) {
          file.close();
          // simulate hard socket failed
          socket.setSoLinger(true, 0);
          socket.close();
          break;
        }

        if (Math.random() < checksumFailPercentage) {
          long currentByteIndex = startIndex;
          while (currentByteIndex < stopIndex) {

            int byteSent = (int) Math.min(bufferSize, stopIndex - currentByteIndex);
            out.write(buffer, 0, byteSent);
            currentByteIndex += byteSent;
          }
          out.flush();
          range = reader.readLine();
          continue;
        }

        file.seek(startIndex);

        long currentByteIndex = startIndex;
        while (currentByteIndex < stopIndex) {

          int byteSent = file.read(buffer, 0, (int) Math.min(bufferSize, stopIndex - currentByteIndex));
          out.write(buffer, 0, byteSent);
          currentByteIndex += byteSent;
        }
        out.flush();
        range = reader.readLine();
      }

      reader.close();
      out.close();
      file.close();

    } catch (Exception e) {
      System.err.println(e);
      try {
        socket.close();
      } catch (Exception ex) {
        System.err.println(ex);
        System.err.println("cannot close");
      }
    }
    try {
      directoryService.finishTransfer(host);
      connectionCountMonitor.decrement();
    } catch (Exception e) {
      System.err.println(e);
    }

  }
}
