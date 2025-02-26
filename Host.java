import java.io.Serializable;
import java.util.Objects;

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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Host) || obj == null)
      return false;

    Host otherhHost = (Host) obj;
    return this.address.equals(otherhHost.address) && (this.port == otherhHost.port);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, port);
  }

  @Override
  public String toString() {
    return address + ":" + port;
  }
}
