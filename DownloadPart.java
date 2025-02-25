import java.io.Serializable;

public class DownloadPart implements Serializable {
  private Host host;
  private long startIndex;
  private long stopIndex;

  public DownloadPart(Host host, long startIndex, long stopIndex) {
    this.host = host;
    this.startIndex = startIndex;
    this.stopIndex = stopIndex;
  }

  public Host getHost() {
    return host;
  }

  public long getStartIndex() {
    return startIndex;
  }

  public long getStopIndex() {
    return stopIndex;
  }
}
