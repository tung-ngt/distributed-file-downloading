import java.io.BufferedInputStream;

public interface DownloadService {
  BufferedInputStream downloadPart(String fileName, DownloadPart downaloadPart) throws Exception;
}
