import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DaemonServiceImpl implements DaemonService {
  private boolean listening;
  private String directoryServiceURL;
  private DirectoryService directoryService;
  private Host host;
  private ServerSocket serverSocket;
  private String dataFolder;
  private double checksumFailPercentage;
  private double disconnectFailPercentage;
  private long networkLatency;
  private boolean linearSlowdown;
  private ConnectionCountMonitor connectionCountMonitor;

  private DaemonServiceImpl(Host host, String directoryServiceURL, String dataFolder, double checksumFailPercentage,
      double disconnectFailPercentage, long networkLatency, boolean linearSlowdown) {
    this.directoryServiceURL = directoryServiceURL;
    this.host = host;
    this.dataFolder = dataFolder;
    this.checksumFailPercentage = checksumFailPercentage;
    this.disconnectFailPercentage = disconnectFailPercentage;
    this.networkLatency = networkLatency;
    this.linearSlowdown = linearSlowdown;
    this.connectionCountMonitor = new ConnectionCountMonitor();
    try {
      directoryService = (DirectoryService) Naming.lookup(this.directoryServiceURL);
    } catch (Exception e) {
      System.err.println("Cannot lookup directory");
      System.err.println(e);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(directoryService, host)));
  }

  @Override
  public void connect() {
    try {
      directoryService.connect(host);
      directoryService.registerHealthCheckCallback(host, new HealthCheckCallbackImpl());
    } catch (Exception e) {
      System.err.println("Cannot connect to directory");
      System.err.println(e);
      e.printStackTrace();
    }

  }

  @Override
  public void disconnect() {
    try {
      directoryService.disconnect(host);
    } catch (Exception e) {
      System.err.println(e);
      System.out.println("cannot tell directory that current host disconnected");
    }
  }

  @Override
  public void listen() {
    try {
      serverSocket = new ServerSocket(this.host.getPort());
    } catch (Exception e) {
      System.err.println(e);
      System.out.println("cannot listen");
    }

    listening = true;
    while (listening) {
      try {
        Socket socket = serverSocket.accept();
        PiecesUploader piecesUploader = new PiecesUploader(socket, directoryService, host, dataFolder,
            checksumFailPercentage, disconnectFailPercentage, networkLatency, linearSlowdown, connectionCountMonitor);
        new Thread(piecesUploader).start();
      } catch (Exception e) {
        System.err.println(e);
      }
    }

  }

  @Override
  public void stopListening() {
    listening = false;
  }

  @Override
  public void addNewFile(String fileName, long pieceSize) {
    try {
      File file = new File(dataFolder + "/" + fileName);
      long fileSize = file.length();
      List<String> checkSums = ChecksumUtils.getFileCheckSums(dataFolder + "/" + fileName, pieceSize);
      directoryService.addNewFile(host, fileName, fileSize, pieceSize, checkSums);
    } catch (Exception e) {
      System.err.println("cannot add file");
      System.err.println(e);
    }
  }

  @Override
  public void hostFiles(List<String> fileNames) {
    try {
      directoryService.hostFiles(host, fileNames);
    } catch (Exception e) {
      System.err.println("cannot host files");
      System.err.println(e);
    }
  }

  public static DaemonServiceImpl fromConfig(Properties dirConfig, Properties daemonConfig) {
    // -------------------------------------------------------------------------------------------
    // PARSE CONFIGS
    // -------------------------------------------------------------------------------------------
    int port = Integer.parseInt(daemonConfig.getProperty("DAEMON_PORT"));
    Host host = new Host(daemonConfig.getProperty("DAEMON_ADDRESS"), port);

    String directoryServiceURL;
    try {
      directoryServiceURL = "//" + dirConfig.getProperty("DIR_ADDRESS") + ":" + dirConfig.getProperty("DIR_PORT") + "/"
          + dirConfig.getProperty("DIR_NAME");
    } catch (Exception e) {
      System.err.println(e);
      throw new RuntimeException("missing config");
    }

    double checksumFailPercentage = -1;
    if ((daemonConfig.getProperty("DAEMON_CHECKSUM_FAIL_PERCENTAGE") != null)
        && (!daemonConfig.getProperty("DAEMON_CHECKSUM_FAIL_PERCENTAGE").trim().isEmpty())) {
      checksumFailPercentage = Double.parseDouble(daemonConfig.getProperty("DAEMON_CHECKSUM_FAIL_PERCENTAGE"));
    }

    double disconnectFailPercentage = -1;
    if ((daemonConfig.getProperty("DAEMON_DISCONNECT_FAIL_PERCENTAGE") != null)
        && (!daemonConfig.getProperty("DAEMON_DISCONNECT_FAIL_PERCENTAGE").trim().isEmpty())) {
      disconnectFailPercentage = Double.parseDouble(daemonConfig.getProperty("DAEMON_DISCONNECT_FAIL_PERCENTAGE"));
    }

    long networkLatency = -1;
    if ((daemonConfig.getProperty("DAEMON_NETWORK_LATENCY") != null)
        && (!daemonConfig.getProperty("DAEMON_NETWORK_LATENCY").trim().isEmpty())) {
      networkLatency = Long.parseLong(daemonConfig.getProperty("DAEMON_NETWORK_LATENCY"));
    }

    boolean linearSlowdown = false;
    if ((daemonConfig.getProperty("DAEMON_LINEAR_SLOWDOWN") != null)
        && (!daemonConfig.getProperty("DAEMON_LINEAR_SLOWDOWN").trim().isEmpty())) {
      linearSlowdown = Boolean.parseBoolean(daemonConfig.getProperty("DAEMON_LINEAR_SLOWDOWN"));
    }

    // -------------------------------------------------------------------------------------------
    // Creating service
    // -------------------------------------------------------------------------------------------
    return new DaemonServiceImpl(host, directoryServiceURL,
        daemonConfig.getProperty("DAEMON_DATA_FOLDER"),
        checksumFailPercentage,
        disconnectFailPercentage,
        networkLatency,
        linearSlowdown);
  }

  public static void addFilesFromConfig(DaemonService daemonService, Properties daemonConfig) {
    if ((daemonConfig.getProperty("DAEMON_NEW_FILES") != null)
        && (!daemonConfig.getProperty("DAEMON_NEW_FILES").trim().isEmpty())) {
      String[] newFileConfigs = daemonConfig.getProperty("DAEMON_NEW_FILES").split(";");
      for (String newFileConfig : newFileConfigs) {
        String[] fileConfigs = newFileConfig.split(",");
        System.out.println("create new file " + fileConfigs[0].toString() + "," + fileConfigs[1].toString());
        daemonService.addNewFile(fileConfigs[0], Integer.parseInt(fileConfigs[1]) * 1024);
      }
    }
  }

  public static void hostFilesFromConfig(DaemonService daemonService, Properties daemonConfig) {
    if ((daemonConfig.getProperty("DAEMON_HOST_FILES") != null)
        && (!daemonConfig.getProperty("DAEMON_HOST_FILES").trim().isEmpty())) {
      List<String> hostFiles = Arrays.asList(daemonConfig.getProperty("DAEMON_HOST_FILES").trim().split(";"));
      if (hostFiles.size() > 0) {
        System.out.println(hostFiles.size() + " host files: " + hostFiles.toString());
        daemonService.hostFiles(hostFiles);
      }
    }

  }

  public static void main(String args[]) {
    if (args.length != 2) {
      System.out
          .println("use the program like this\njava DaemonServiceImpl <path-to-dir-config> <path-to-daemon-config>");
      throw new RuntimeException("missing or invalid args");
    }
    Properties dirConfig = new Properties();
    Properties daemonConfig = new Properties();
    try {
      dirConfig.load(new FileInputStream(args[0]));
    } catch (Exception e) {
      System.err.println("not found dir config");
    }

    try {
      daemonConfig.load(new FileInputStream(args[1]));
    } catch (Exception e) {
      System.err.println("not found daemon config");
    }

    // -------------------------------------------------------------------------------------------
    // Creating and connecting service
    // -------------------------------------------------------------------------------------------
    DaemonService daemonService = DaemonServiceImpl.fromConfig(dirConfig, daemonConfig);
    daemonService.connect();

    // -------------------------------------------------------------------------------------------
    // Add and host available files from config
    // -------------------------------------------------------------------------------------------
    DaemonServiceImpl.addFilesFromConfig(daemonService, daemonConfig);
    DaemonServiceImpl.hostFilesFromConfig(daemonService, daemonConfig);

    // -------------------------------------------------------------------------------------------
    // Listen to incomming downloader
    // -------------------------------------------------------------------------------------------
    daemonService.listen();
  }
}
