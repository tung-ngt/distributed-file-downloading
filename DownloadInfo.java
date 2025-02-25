import java.util.List;

public interface DownloadInfo {
  String getFileChecksum();
  long getFileSize();
  List<DownloadPart> getParts();
};
