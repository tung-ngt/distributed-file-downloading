import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class TestClient {
  public static DirectoryService directoryService;

  public static void main(String[] args) throws Exception {
    Host host = new Host("localhost", 4444);
    String fileName = "abc.txt";
    long fileSize = 500_000;
    long pieceSize = 1000;
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        directoryService.disconnect(host);
      } catch (Exception e) {
        System.err.println(e);
        e.printStackTrace();
      }
    }));

    try {
      if (args.length != 3) {
        throw new Exception("Invalid arguments");
      }

      String address = args[0];
      int port = Integer.parseInt(args[1]);
      String name = args[2];

      String URL = "//" + address + ":" + port + "/" + name;

      directoryService = (DirectoryService) Naming.lookup(URL);

      List<String> checkSums = new ArrayList<>();
      for (int i = 0; i < Math.ceil((double) fileSize / pieceSize); i++) {
        checkSums.add("");
      }
      directoryService.connect(host);
      HealthCheckCallback callback = new HealthCheckCallbackImpl();
      directoryService.registerHealthCheckCallback(host, callback);
      // directoryService.hostFiles(host, List.of(fileName));
      directoryService.addNewFile(host, fileName, fileSize, pieceSize, checkSums);

      List<DownloadSource> downloadSources = directoryService.failedSources(fileName, List.of(
          new DownloadSource(
              host,
              List.of((long) 10, (long) 20, (long) 30))),
          1);
      System.out.println(downloadSources.get(0).getPieceIndices());
      // DownloadInfo downloadInfo = directoryService.downloadInfo(fileName, 3);
      // System.out.println(downloadInfo.getSources().size());
      // System.out.println(downloadInfo.getSources().get(0).getPieceIndices());
      // System.out.println(downloadInfo.getSources().get(1).getPieceIndices());

    } catch (Exception e) {
      System.err.println(e);
      e.printStackTrace();
      throw new Exception();
    }
  }

}
