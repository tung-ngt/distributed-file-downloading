import java.io.Serializable;
import java.util.List;

public class DownloadSource implements Serializable {
  private Host host;
  private List<Long> pieceIndices;

  public DownloadSource(Host host, List<Long> pieceIndices) {
    this.host = host;
    this.pieceIndices = pieceIndices;
  }

  public Host getHost() {
    return host;
  }

  public List<Long> getPieceIndices() {
    return pieceIndices;
  }
}
