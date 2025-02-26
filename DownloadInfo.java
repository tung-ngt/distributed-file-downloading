import java.io.Serializable;
import java.util.List;

public class DownloadInfo implements Serializable {
  private FileInfo fileInfo;
  private List<DownloadSource> sources;

  public DownloadInfo(FileInfo fileInfo, List<DownloadSource> sources) {
    this.fileInfo = fileInfo;
    this.sources = sources;
  }

  public FileInfo getFileInfo() {
    return fileInfo;
  }

  public List<DownloadSource> getSources() {
    return sources;
  }
}
