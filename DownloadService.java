public interface DownloadService {
  boolean download(String fileName, int favorableNoSources, boolean sequential, String downloadFolder);
}
