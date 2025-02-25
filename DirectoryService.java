import java.rmi.*;
import java.util.List;


public interface DirectoryService extends Remote {
  DownloadInfo downloadInfo(String fileName, int favorableNoSource) throws RemoteException;
  List<DownloadPart> failedParts(String fileName, List<DownloadPart> parts) throws RemoteException;

  void connect(Host host) throws RemoteException;
  void disconnect(Host host) throws RemoteException;
  void addFragments(Host host, List<Fragment> fragments) throws RemoteException;
  void addNewFile(Host host, String fileName, long fileSize, String checkSum) throws RemoteException;
}
