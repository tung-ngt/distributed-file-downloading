import java.rmi.*;
import java.util.List;


public interface DirectoryService extends Remote {
  DownloadInfo downloadInfo(String fileName, int favorableNoSource) throws RemoteException;
  List<DownloadSource> failedSources(String fileName, List<DownloadSource> sources) throws RemoteException;

  void connect(Host host) throws RemoteException;
  void disconnect(Host host) throws RemoteException;
  void hostFiles(Host host, List<String> fileNames) throws RemoteException;
  void addNewFile(Host host, String fileName, long fileSize, long pieceSize, List<String> checkSums) throws RemoteException;
}
