import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadInfo implements Serializable {
  private String checkSum;
  private long fileSize;
  private ArrayList<DownloadPart> parts;

  public DownloadInfo(String checkSum, long fileSize, ArrayList<DownloadPart> parts) {
    this.checkSum = checkSum;
    this.fileSize = fileSize;
    this.parts = parts;
  }

  public String getFileChecksum() {
    return checkSum;
  }

  public long getFileSize() {
    return fileSize;
  }

  public List<DownloadPart> getParts() {
    return parts;
  }
}
