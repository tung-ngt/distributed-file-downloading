public class ConnectionCountMonitor {
  private int noConnection;
  public ConnectionCountMonitor() {
    noConnection = 0;
  }

  public synchronized void increment() {
    noConnection++;
  }

  public synchronized void decrement() {
    noConnection--;
  }

  public synchronized int get() {
    return noConnection;
  }
}
