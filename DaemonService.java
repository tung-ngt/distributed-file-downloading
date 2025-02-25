public interface DaemonService {
  void listen(Host host);
  void connect();
  void addFragments();
  void addNewFile();
  void disconnect();
}
