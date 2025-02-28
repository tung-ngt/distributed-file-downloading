import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class HealthCheckCallbackImpl extends UnicastRemoteObject implements HealthCheckCallback {
  public HealthCheckCallbackImpl() throws RemoteException {

  }

  @Override
  public void healthCheck() throws RemoteException {
    System.out.println("Still alive");
  }
}
