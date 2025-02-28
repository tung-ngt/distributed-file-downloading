import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;

public class DaemonServiceImpl implements DaemonService {
  private String directoryServiceURL;
  private DirectoryService directoryService;
  private Host host;
  private ServerSocket serverSocket;

  public DaemonServiceImpl(Host host, String directoryServiceURL) {
    this.directoryServiceURL = directoryServiceURL;
    this.host = host;
    try {
      directoryService = (DirectoryService) Naming.lookup(this.directoryServiceURL);
      directoryService.connect(host);
      directoryService.registerHealthCheckCallback(host, new HealthCheckCallbackImpl());
    } catch (Exception e) {
      System.out.println("what");
      System.err.println(e);
      e.printStackTrace();
    }
  }

  @Override
  public void listen() {
    try {
      serverSocket = new ServerSocket(this.host.getPort());
    } catch (Exception e) {
      System.err.println(e);
      System.out.println("cannot listen");
      e.printStackTrace();
    }

    while (true) {
      try {
        Socket socket = serverSocket.accept();
        PiecesUploader piecesUploader = new PiecesUploader(socket, directoryService, host);
        new Thread(piecesUploader).start();
      } catch (Exception e) {
        System.err.println(e);
      }
    }

  }

  @Override
  public void addNewFile(String fileName, long pieceSize) {
    try {
      File file = new File(fileName);
      long fileSize = file.length();
      List<String> checkSums = ChecksumUtils.getFileCheckSums(fileName, pieceSize);
      directoryService.addNewFile(host, fileName, fileSize, pieceSize, checkSums);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void hostFiles(List<String> fileNames) {
    try {
      directoryService.hostFiles(host, fileNames);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) {
    ConfigLoader daemonConfig = new ConfigLoader();
    daemonConfig.load(args[0]);
    

    int port = Integer.parseInt(daemonConfig.get("DAEMON_PORT"));
    Host host = new Host(daemonConfig.get("DAEMON_PORT"), port);
    DaemonService daemonService = new DaemonServiceImpl(host, daemonConfig.get("DIR_URL"));
    daemonService.hostFiles(List.of("im.jpg"));
    //daemonService.addNewFile("im.jpg", 1024 * 1024);
    daemonService.listen();

  }
}
