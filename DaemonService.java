import java.util.List;

public interface DaemonService {
  // void listen(Host host);
  void connect();
  void addNewFile(String fileName, long pieceSize);
  void hostFiles(List<String> fileNames);
  void disconnect();
}
