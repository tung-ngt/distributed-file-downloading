import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Client {
  // ----------------------------------------------------------------------------------------------
  // Services
  // ----------------------------------------------------------------------------------------------
  private DaemonService daemonService;
  private DownloadService downloadService;

  // ----------------------------------------------------------------------------------------------
  // Config
  // ----------------------------------------------------------------------------------------------
  private Properties diretoryConfig;
  private Properties daemonConfig;
  private String dataFolder;

  private Scanner scanner;
  private boolean listening;

  public Client(Properties diretoryConfig, Properties daemonConfig) {
    this.diretoryConfig = diretoryConfig;
    this.daemonConfig = daemonConfig;
    daemonService = DaemonServiceImpl.fromConfig(diretoryConfig, daemonConfig);
    downloadService = DownloadServiceImpl.fromConfig(diretoryConfig);
    dataFolder = daemonConfig.getProperty("DAEMON_DATA_FOLDER");
  }

  public void help() {
    System.out.println(
        "commands:\n" +
            "daemon-connect\n" +
            "add-files-from-config\n" +
            "host-files-from-config\n" +
            "daemon-disconnect\n" +
            "daemon-listen\n" +
            "daemon-stop-listen\n" +
            "add-new-file\n" +
            "host-files\n" +
            "download\n" +
            "help\n" +
            "exit");
  }

  public void handleCommands() {
    help();
    scanner = new Scanner(System.in);
    String command;
    while (true) {
      System.out.print("> ");
      command = scanner.nextLine();

      switch (command.toLowerCase()) {
        case "add-files-from-config":
          addFilesFromConfig();
          break;
        case "host-files-from-config":
          hostFilesFromConfig();
          break;
        case "daemon-connect":
          daemonConnect();
          break;
        case "daemon-disconnect":
          daemonDisconnect();
          break;
        case "daemon-listen":
          daemonListen();
          break;
        case "daemon-stop-listen":
          daemonStopListening();
          break;
        case "add-new-file":
          addNewFile();
          break;
        case "download":
          downloadFile();
          break;
        case "host-files":
          hostFiles();
          break;
        case "exit":
          System.out.println("exiting");
          scanner.close();
          return;
        case "help":
          help();
          break;
        default:
          System.out.println("invalid command");
          System.out.println("use (help) to show commands ");
          break;
      }
    }

  }

  private void addFilesFromConfig() {
    DaemonServiceImpl.addFilesFromConfig(daemonService, daemonConfig);
  }

  private void hostFilesFromConfig() {
    DaemonServiceImpl.hostFilesFromConfig(daemonService, daemonConfig);
  }

  private void daemonConnect() {
    daemonService.connect();
  }

  private void daemonDisconnect() {
    daemonService.disconnect();
  }

  private void daemonListen() {
    if (!listening) {
      new Thread(() -> daemonService.listen()).start();
      listening = true;
      System.out.println("daemon listening");
    } else {
      System.out.println("already started");
    }
  }

  private void daemonStopListening() {
    if (listening) {
      daemonService.stopListening();
      listening = false;
      System.out.println("daemon stopping");
    } else {
      System.out.println("not yet started");
    }
  }

  private void addNewFile() {
    System.out.println("filename:");
    String fileName = scanner.nextLine();
    System.out.println("pieceSize (kb):");
    long pieceSize = Long.parseLong(scanner.nextLine());
    daemonService.addNewFile(fileName, pieceSize);
  }

  private void downloadFile() {
    System.out.println("filename:");
    String fileName = scanner.nextLine();
    System.out.println("number of favorable sources:");
    int noFavorableSources = Integer.parseInt(scanner.nextLine());
    System.out.println("sequential? (true|false):");
    boolean sequential = Boolean.parseBoolean(scanner.nextLine());
    System.out.println("allowHostFile? (true|false):");
    boolean allowHostFile = Boolean.parseBoolean(scanner.nextLine());

    boolean successfull = downloadService.download(fileName, noFavorableSources, sequential, dataFolder);
    if (successfull && allowHostFile) {
      System.out.println("hosting " + fileName);
      daemonService.hostFiles(List.of(fileName));
    }
  }

  private void hostFile(String fileName) {
    System.out.println("filename:");
    daemonService.hostFiles(List.of(fileName));
  }

  private void hostFiles() {
    System.out.println("filenames (separated by ;):");
    String[] fileNames = scanner.nextLine().split(";");
    daemonService.hostFiles(Arrays.asList(fileNames));
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("run the program like this\n" +
          "java Client <path-to-directory-config> <path-to-daemon-config>");
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

    Client client = new Client(dirConfig, daemonConfig);
    System.out.println("initialized client");
    client.handleCommands();
  }
}
