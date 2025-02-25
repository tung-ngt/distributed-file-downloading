public interface DaemonService {
  void listen(Host host);
  void connect();
  void disconnect();
}
