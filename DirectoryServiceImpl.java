import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
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
        .collect(Collectors.toCollection(ArrayList::new));
  };

  public List<DownloadSource> getDownloadSources(List<Host> availableHosts, int noSource, List<Long> piecesIndices) {
    int noPieces = piecesIndices.size();

    double totalWeight = 0.0;
    double[] weights = new double[noSource];

    for (int i = 0; i < noSource; i++) {
      Host host = availableHosts.get(i);
      weights[i] = 1.0 / (hostConnections.get(host) + 1);
      totalWeight += weights[i];
    }

    List<DownloadSource> downloadSources = new ArrayList<>();
    int currentIndex = 0;
    double normalizedWeight;
    int noUsePieces;

    for (int i = 0; i < noSource; i++) {
      List<Long> pieces;
      normalizedWeight = weights[i] / totalWeight;

      if (i == noSource - 1) {
        noUsePieces = noPieces - currentIndex;
      } else {
        noUsePieces = (int) Math.floor(noPieces * normalizedWeight);
      }

      Host host = availableHosts.get(i);
      System.out.println(host.toString() + " weight: " + normalizedWeight);
      pieces = new ArrayList<>(piecesIndices.subList(currentIndex, currentIndex + noUsePieces));
      currentIndex += noUsePieces;
      downloadSources.add(new DownloadSource(host, pieces));
    }

    return downloadSources;
  }

  @Override
  public DownloadInfo downloadInfo(String fileName, int favorableNoSource) throws RemoteException {

    System.out.println("get request for file " + fileName);
    if (!files.containsKey(fileName)) {
      throw new RemoteException("No available file");
    }

    FileInfo fileInfo = files.get(fileName);
    List<Host> availableHosts = getAvailableHosts(fileName);

    if (availableHosts.size() == 0) {
      throw new RemoteException("All host offline for file");
    }

    int noSource = Integer.min(favorableNoSource, availableHosts.size());
    int noPieces = fileInfo.getCheckSums().size();

    List<Long> piecesIndices = LongStream.range(0, noPieces).boxed().toList();
    List<DownloadSource> downloadSources = getDownloadSources(availableHosts, noSource, piecesIndices);
    System.out
        .println("asked for no fav sources " + favorableNoSource + ". Allocate no source " + downloadSources.size());
    System.out.println();
    DownloadInfo downloadInfo = new DownloadInfo(
        fileInfo,
        downloadSources);
    return downloadInfo;
  }

  @Override
  public List<DownloadSource> failedSources(String fileName, List<DownloadSource> sources, int favorableNoSource)
      throws RemoteException {

    System.out.println("failed sources retrying for file " + fileName);
    List<Long> failedPieces = new ArrayList<>();
    List<Host> failedHosts = new ArrayList<>();
    for (DownloadSource source : sources) {
      Host host = source.getHost();
      failedPieces.addAll(source.getPieceIndices());
      try {
        hostHealthCheckCallbacks.get(host).healthCheck();
        System.out.println(host.toString() + " healthCheck still alive moving to back of priority queue");
        failedHosts.add(host);

      } catch (Exception e) {
        // Falied health check, mark the host as disconnected
        System.out.println(host.toString() + " failed healthCheck, marking disconnected");
        try {
          disconnect(host);
        } catch (Exception ex) {
          System.err.println(ex);
        }
      }
    }

    List<Host> availableHosts = getAvailableHosts(fileName);

    // Moving failed hosts to the back of the queue to give less priority
    for (Host failedHost : failedHosts) {
      try {
        Host host = availableHosts.remove(availableHosts.indexOf(failedHost));
        availableHosts.add(host);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (availableHosts.size() == 0) {
      throw new RemoteException("All host offline for file");
    }

    int noSource = Integer.min(favorableNoSource, availableHosts.size());
    List<DownloadSource> downloadSources = getDownloadSources(availableHosts, noSource, failedPieces);
    System.out
        .println("asked for no fav sources " + favorableNoSource + ". Allocate no source " + downloadSources.size());
    System.out.println();
    return downloadSources;
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
    if (args.length != 1) {
      System.out.println("use the program like this\njava DirectoryServiceImpl <path-to-dir-config>");
      throw new RuntimeException("missing or invalid args");
    }
    try {
      Properties dirConfig = new Properties();
      dirConfig.load(new FileInputStream(args[0]));

      String address = dirConfig.getProperty("DIR_ADDRESS");
      int port = Integer.parseInt(dirConfig.getProperty("DIR_PORT"));
      String name = dirConfig.getProperty("DIR_NAME");

      System.setProperty("java.rmi.server.hostname", address);
      LocateRegistry.createRegistry(port);

      DirectoryService directoryService = new DirectoryServiceImpl();

      String URL = "//" + address + ":" + port + "/" + name;

      Naming.rebind(URL, directoryService);
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
