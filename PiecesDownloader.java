import java.io.DataInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PiecesDownloader implements Runnable {

  private DownloadSource downloadSource;
  private FileInfo fileInfo;
  private Map<Long, byte[]> pieces;

  public PiecesDownloader(FileInfo fileInfo, DownloadSource downloadSource) {
    this.fileInfo = fileInfo;
    this.downloadSource = downloadSource;
    this.pieces = new HashMap<>();
  }

  @Override
  public void run() {
    try {

      Host host = downloadSource.getHost();
      Socket socket = new Socket(host.getAddress(), host.getPort());

      DataInputStream in = new DataInputStream(socket.getInputStream());
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

      List<Long> pieceIndices = downloadSource.getPieceIndices();

      writer.println(fileInfo.getFileName());

      long pieceSize = fileInfo.getPieceSize();

      List<String> checksums = fileInfo.getCheckSums();
      for (long pieceIndex : pieceIndices) {
        long startIndex = pieceIndex * pieceSize;
        long stopIndex = Math.min(startIndex + pieceSize, fileInfo.getFileSize());

        long currentPieceSize = Math.min(pieceSize, stopIndex - startIndex);

        writer.println(startIndex + "," + stopIndex);

        byte[] buffer = new byte[(int) currentPieceSize];

        int bytesRead = 0;
        int accBytesRead = 0;

        while (accBytesRead < currentPieceSize
            && (bytesRead = in.read(buffer, accBytesRead, (int) (currentPieceSize - accBytesRead))) != -1) {
          accBytesRead += bytesRead;
        }

        String receivedChecksum = ChecksumUtils.getCheckSum(buffer);

        if (!receivedChecksum.equals(checksums.get((int) pieceIndex))) {
          System.out.println("Error: Checksum mismatch for piece " + pieceIndex);
        }
        pieces.put(pieceIndex, buffer);

      }
      System.out.println(host.toString() + " source fin");
      socket.close();
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  public Map<Long, byte[]> getPieces() {
    return pieces;
  }
}
