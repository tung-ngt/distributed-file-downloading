import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DownloadServiceImpl implements DownloadService {
  private String directoryServiceURL;
  private DirectoryService directoryService;

  private DownloadServiceImpl(String directoryServiceURL) {
    this.directoryServiceURL = directoryServiceURL;
    try {
      directoryService = (DirectoryService) Naming.lookup(this.directoryServiceURL);
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  @Override
  public void download(String fileName, int favorableNoSources, boolean sequential) {
    // Get file info and download sources first
    try {

      DownloadInfo downloadInfo = directoryService.downloadInfo(fileName, favorableNoSources);

      FileInfo fileInfo = downloadInfo.getFileInfo();
      int noPieces = fileInfo.getCheckSums().size();

      if (fileInfo == null) {
        throw new IllegalStateException("FileInfo cannot be null!");
      }

      Set<Long> successfullPieces = new HashSet<>();
      List<DownloadSource> downloadSources = downloadInfo.getSources();
      List<DownloadSource> failedSources = new ArrayList<>();
      int noFailedPieces = 0;
      int attempt = 0;
      while (attempt < 3) {
        PiecesDownloader[] downloaders = new PiecesDownloader[downloadSources.size()];
        Thread[] threads = new Thread[downloadSources.size()];

        System.out.println("starting download asking directory no " + attempt);

        for (int i = 0; i < downloadSources.size(); i++) {
          downloaders[i] = new PiecesDownloader(fileInfo, downloadSources.get(i));
          threads[i] = new Thread(downloaders[i]);
          threads[i].start();

          if (sequential) {
            threads[i].join();
          }
        }

        for (int i = 0; i < downloadSources.size(); i++) {
          threads[i].join();
          successfullPieces.addAll(downloaders[i].successfullPieces());
          if (!downloaders[i].successfull()) {
            DownloadSource failedSource = downloaders[i].failedPieces();
            failedSources.add(failedSource);
            noFailedPieces += failedSource.getPieceIndices().size();
          }
        }

        if (successfullPieces.size() == noPieces) {
          break;
        }

        int newFavorableNoSources = (int) Math.max(favorableNoSources * ((double) noFailedPieces / noPieces), 1);
        downloadSources = directoryService.failedSources(fileName, failedSources, newFavorableNoSources);
        failedSources.clear();
        noFailedPieces = 0;
        System.out.println("some source failed asking directory again with number of source " + newFavorableNoSources);
        attempt++;
      }

      if (successfullPieces.size() == noPieces) {
        Files.move(Paths.get("download/" + fileName + ".temp"), Paths.get("download/" + fileName),
            StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Download successfull");
      } else {
        Files.delete(Paths.get("download/" + fileName + ".temp"));
        System.err.println("Failed download after 3 attempts");
      }

    } catch (Exception e) {
      System.err.println(e);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.out.println(
          "use the program like this\njava DownloadServiceImpl <path-to-dir-config> <file-to-download> <no-favorable-source> <seq|para>");
      throw new RuntimeException("missing or invalid args");
    }
    Properties dirConfig = new Properties();
    try {
      dirConfig.load(new FileInputStream(args[0]));
    } catch (Exception e) {
      System.err.println(e);
      throw new RuntimeException("invalid directory config file");
    }

    String directoryServiceURL;
    try {
      directoryServiceURL = "//" + dirConfig.getProperty("DIR_ADDRESS") + ":" + dirConfig.getProperty("DIR_PORT") + "/"
          + dirConfig.getProperty("DIR_NAME");
    } catch (Exception e) {
      System.err.println(e);
      throw new RuntimeException("missing config");
    }
    DownloadService downloadService = new DownloadServiceImpl(directoryServiceURL);

    String fileName = args[1];
    int favorableNoSources = Integer.parseInt(args[2]);
    boolean sequential = args[3].equals("seq");
    downloadService.download(fileName, favorableNoSources, sequential);
  }
}
