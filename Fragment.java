import java.io.Serializable;

public class Fragment implements Serializable {

  private String fileName;
  private long startIndex;
  private long stopIndex;

  public Fragment(String fileName, long startIndex, long stopIndex) {
    this.fileName = fileName;
    this.startIndex = startIndex;
    this.stopIndex = stopIndex;
  };

  public String getFileName() {
    return fileName;
  }

  public long getStopIndex() {
    return stopIndex;
  }

  public long getStartIndex() {
    return startIndex;
  }
}
