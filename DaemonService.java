import java.util.List;

public interface DaemonService {
  void listen();
  void addNewFile(String fileName, long pieceSize);
  void hostFiles(List<String> fileNames);
}
