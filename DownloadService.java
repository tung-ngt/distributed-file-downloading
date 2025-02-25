import java.io.BufferedInputStream;

public interface DownloadService {
  BufferedInputStream downloadPart(String fileName, DownloadPart downalodPart) throws Exception;
}
