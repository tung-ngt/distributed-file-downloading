import java.util.List;

public interface DaemonService {
  void connect();
  void disconnect();
  void listen();
  void stopListening();
  void addNewFile(String fileName, long pieceSize);
  void hostFiles(List<String> fileNames);
}
