import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {
  Map<String, List<DownloadPart>> files;

  public DirectoryServiceImpl() throws RemoteException {

  }

  @Override
  public DownloadInfo downloadInfo(String fileName, int favorableNoSource) throws RemoteException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<DownloadPart> failedParts(String fileName, List<DownloadPart> parts) throws RemoteException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void connect(Host host) throws RemoteException {
    // TODO Auto-generated method stub

  }

  @Override
  public void disconnect(Host host) throws RemoteException {
    // TODO Auto-generated method stub

  }

  @Override
  public void addFragments(Host host, List<Fragment> fragments) throws RemoteException {
  }

  @Override
  public void addNewFile(Host host, String fileName, long fileSize, String checkSum) throws RemoteException {
    // TODO Auto-generated method stub

  }

}
