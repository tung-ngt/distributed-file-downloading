import java.io.BufferedInputStream;

public interface DownloadService {
  BufferedInputStream downloadSource(String fileName, DownloadSource downaloadSource) throws Exception;
}
