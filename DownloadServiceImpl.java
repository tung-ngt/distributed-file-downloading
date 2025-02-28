import java.io.FileOutputStream;
import java.rmi.Naming;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadServiceImpl implements DownloadService {
  private String directoryServiceURL = "//192.168.1.55:8888/dir";
    private DirectoryService directoryService;

    private DownloadServiceImpl() {
      try {
        directoryService = (DirectoryService) Naming.lookup(directoryServiceURL);
      } catch (Exception e) {
        System.err.println(e);
      }
    }


    @Override
    public void download(String fileName, int favorableNoSources)  {
        // Get file info and download sources first
        try {
          
        DownloadInfo downloadInfo = directoryService.downloadInfo(fileName, favorableNoSources);

        List<DownloadSource> downloadSources = downloadInfo.getSources();

        FileInfo fileInfo = downloadInfo.getFileInfo();

        if (fileInfo == null) {
            throw new IllegalStateException("FileInfo cannot be null!");
        }


        PiecesDownloader[] downloaders = new PiecesDownloader[downloadSources.size()];
        Thread[] threads = new Thread[downloadSources.size()];


        System.out.println("starting download");

        for (int i = 0; i < downloadSources.size(); i++) {
            downloaders[i] = new PiecesDownloader(fileInfo, downloadSources.get(i));
            threads[i] = new Thread(downloaders[i]);
            threads[i].start();
        }

        Map<Long, byte[]> pieciesMap = new HashMap<>();
        for (int i = 0; i < downloadSources.size(); i++) {
            threads[i].join();
            pieciesMap.putAll(downloaders[i].getPieces());
        }



        int noPieces = fileInfo.getCheckSums().size();


        FileOutputStream fileOutputStream = new FileOutputStream("downloaded.jpg");
        for (int i = 0; i < noPieces; i++) {
          fileOutputStream.write(pieciesMap.get((long) i));
        }


        fileOutputStream.close();
        } catch (Exception e) {
          System.err.println(e);
        }
    }


    public static void main(String[] args) throws Exception {

        String fileName = args[0];


        DownloadService downloadService = new DownloadServiceImpl();
        downloadService.download(fileName, 2);
    }
}
