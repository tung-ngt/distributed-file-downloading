import java.io.Serializable;
import java.util.List;

public class FileInfo implements Serializable {
  private String fileName;
  private long fileSize;
  private long pieceSize;
  private List<String> checkSums;
  private Host originalOwner;

  public FileInfo(String fileName, long fileSize, long pieceSize, List<String> checkSums, Host originalOwner) {
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.pieceSize = pieceSize;
    this.checkSums = checkSums;
    this.originalOwner = originalOwner;

  }

  public long getFileSize() {
    return fileSize;
  }

  public List<String> getCheckSums() {
    return checkSums;
  }

  public String getFileName() {
    return fileName;
  }

  public Host getOriginalOwner() {
    return originalOwner;
  }

  public long getPieceSize() {
    return pieceSize;
  }
}
