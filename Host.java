import java.io.Serializable;

public class Host implements Serializable {
  private String address;
  private int port;

  public Host(String address, int port) {
    this.address = address;
    this.port = port;
  }

  public String getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }
}
