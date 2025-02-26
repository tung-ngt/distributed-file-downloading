import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HealthCheckCallback extends Remote, Serializable {
  void healthCheck() throws RemoteException;
}
