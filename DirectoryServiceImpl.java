import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {
  Set<Host> onlineHost;
  Map<Host, Integer> hostConnections;
  Map<String, Set<Host>> sources;
  Map<String, FileInfo> files;
  Map<Host, HealthCheckCallback> hostHealthCheckCallbacks;

  public DirectoryServiceImpl() throws RemoteException {
    onlineHost = new HashSet<>();
    hostConnections = new HashMap<>();
    sources = new HashMap<>();
    files = new HashMap<>();
    hostHealthCheckCallbacks = new HashMap<>();
  }

  public List<Host> getAvailableHosts(String fileName) {
    return sources.get(fileName)
        .stream()
        .filter(onlineHost::contains)
        .sorted((a, b) -> hostConnections.get(a).compareTo(hostConnections.get(b)))
        .toList();
  };

  public List<DownloadSource> getDownloadSources(List<Host> availableHosts, int noSource, List<Long> piecesIndices) {
    int noPieces = piecesIndices.size();
    int piecesPerSource = Math.floorDiv(noPieces, noSource);

    List<DownloadSource> downloadSources = new ArrayList<>();
    for (int i = 0; i < noSource; i++) {

      List<Long> pieces;

      if (i == (noSource - 1)) {
        pieces = new ArrayList<Long>(piecesIndices.subList(i * piecesPerSource, noPieces));
      } else {

        pieces = new ArrayList<Long>(piecesIndices.subList(i * piecesPerSource, (i + 1) * piecesPerSource));
      }

      downloadSources.add(new DownloadSource(
          availableHosts.get(0),
          pieces));
    }

    return downloadSources;
  }

  @Override
  public DownloadInfo downloadInfo(String fileName, int favorableNoSource) throws RemoteException {
    if (!files.containsKey(fileName)) {
      throw new RemoteException("No available file");
    }

    FileInfo fileInfo = files.get(fileName);

    System.out.println(onlineHost.size());
    System.out.println(sources.get(fileName).size());

    List<Host> availableHosts = getAvailableHosts(fileName);

    if (availableHosts.size() == 0) {
      throw new RemoteException("All host offline for file");
    }

    int noSource = Integer.min(favorableNoSource, availableHosts.size());
    int noPieces = fileInfo.getCheckSums().size();

    List<Long> piecesIndices = LongStream.range(0, noPieces).boxed().toList();
    List<DownloadSource> downloadSources = getDownloadSources(availableHosts, noSource, piecesIndices);

    DownloadInfo downloadInfo = new DownloadInfo(
        fileInfo,
        downloadSources);
    return downloadInfo;
  }

  @Override
  public List<DownloadSource> failedSources(String fileName, List<DownloadSource> sources, int favorableNoSource)
      throws RemoteException {

    List<Long> failedPieces = new ArrayList<>();
    for (DownloadSource source : sources) {
      Host host = source.getHost();
      failedPieces.addAll(source.getPieceIndices());
      try {
        hostHealthCheckCallbacks.get(host).healthCheck();
      } catch (Exception e) {
        // Falied health check, mark the host as disconnected
        System.out.println(e);
        System.out.println(host.toString() + " failed healthCheck, marking disconnected");
        try {
          disconnect(host);
        } catch (Exception ex) {
          System.err.println(ex);
        }
      }
    }

    List<Host> availableHosts = getAvailableHosts(fileName);

    if (availableHosts.size() == 0) {
      throw new RemoteException("All host offline for file");
    }

    int noSource = Integer.min(favorableNoSource, availableHosts.size());
    return getDownloadSources(availableHosts, noSource, failedPieces);
  }

  @Override
  public void registerHealthCheckCallback(Host host, HealthCheckCallback callback) throws RemoteException {
    hostHealthCheckCallbacks.put(host, callback);
  }

  @Override
  public void newTransfer(Host host) throws RemoteException {
    int count = hostConnections.get(host);
    hostConnections.put(host, count + 1);
  }

  @Override
  public void finishTransfer(Host host) throws RemoteException {
    int count = hostConnections.get(host);
    hostConnections.put(host, count - 1);
  }

  @Override
  public void connect(Host host) throws RemoteException {
    onlineHost.add(host);
    hostConnections.put(host, 0);
    System.out.println(host.toString() + " connected");
  }

  @Override
  public void disconnect(Host host) throws RemoteException {
    onlineHost.remove(host);
    hostConnections.remove(host);
    System.out.println(host.toString() + " disconnected");
  }

  @Override
  public void hostFiles(Host host, List<String> fileNames) throws RemoteException {
    for (String fileName : fileNames) {
      sources.get(fileName).add(host);
    }
  }

  @Override
  public void addNewFile(Host host, String fileName, long fileSize, long pieceSize, List<String> checkSums)
      throws RemoteException {
    if (files.containsKey(fileName)) {
      // Do not allow adding a files with the same name. Files are immutable
      throw new RemoteException("file already exist");
    }

    int noPieces = (int) Math.ceil((double) fileSize / pieceSize);

    if (checkSums.size() != noPieces) {
      throw new RemoteException("Number of pieces does not match number of checksums");
    }

    files.put(fileName, new FileInfo(fileName, fileSize, pieceSize, checkSums, host));
    Set<Host> source = new HashSet<>();
    source.add(host);
    sources.put(fileName, source);
  }

  public static void main(String[] args) {
    try {
      if (args.length != 3) {
        throw new Exception("Invalid arguments");
      }

      ConfigLoader dirConfig = new ConfigLoader();
      dirConfig.load(args[0]);

      String address = dirConfig.get("DIR_ADDRESS");
      int port = Integer.parseInt(dirConfig.get("DIR_PORT"));
      String name = dirConfig.get("DIR_NAME");

      LocateRegistry.createRegistry(port);

      DirectoryService directoryService = new DirectoryServiceImpl();

      String URL = "//" + address + ":" + port + "/" + name;

      Naming.rebind(URL, directoryService);
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
