import java.io.BufferedInputStream;

public interface DownloadService{
  BufferedInputStream downloadSource(String fileName, int favorableNoSources) throws Exception;
}
