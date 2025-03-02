public class ShutdownHook implements Runnable {
  private DirectoryService directoryService;
  private Host host;

  public ShutdownHook(DirectoryService directoryService, Host host) {
    this.directoryService = directoryService;
    this.host = host;
  }

  @Override
  public void run() {
    try {
      directoryService.disconnect(host);
    } catch (Exception e) {
      System.err.println(e);
      System.out.println("cannot tell directory that current host disconnected");
    }
  }
}
