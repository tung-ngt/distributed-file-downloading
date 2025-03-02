import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class PiecesDownloader implements Runnable {

  private DownloadSource downloadSource;
  private FileInfo fileInfo;
  private Set<Long> successFullPieces;
  private Set<Long> failedPieces;
  private String downloadFolder;

  public PiecesDownloader(FileInfo fileInfo, DownloadSource downloadSource, String downloadFolder) {
    this.fileInfo = fileInfo;
    this.downloadSource = downloadSource;
    this.successFullPieces = new HashSet<>();
    this.failedPieces = new HashSet<>(downloadSource.getPieceIndices());
    this.downloadFolder = downloadFolder;
  }

  @Override
  public void run() {
    Host host = downloadSource.getHost();

    try {

      RandomAccessFile file = new RandomAccessFile(downloadFolder + "/" + fileInfo.getFileName() + ".temp", "rw");
      FileChannel fileChannel = file.getChannel();

      for (int attempt = 0; attempt < 3; attempt++) {
        if (failedPieces.size() == 0) {
          break;
        }

        System.out.println(host.toString() + " attempt " + attempt);
        try {
          Socket socket = new Socket(host.getAddress(), host.getPort());
          DataInputStream in = new DataInputStream(socket.getInputStream());
          PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

          writer.println(fileInfo.getFileName());

          long pieceSize = fileInfo.getPieceSize();

          failedPieces.size();
          List<String> checksums = fileInfo.getCheckSums();

          int noRemaining = failedPieces.size();
          Iterator<Long> iter = failedPieces.iterator();
          for (int i = 0; i < noRemaining; i++) {
            if (!iter.hasNext())
              break;
            long pieceIndex = iter.next();
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
              continue;
            }

            iter.remove();
            fileChannel.write(ByteBuffer.wrap(buffer), startIndex);
            successFullPieces.add(pieceIndex);
          }
          socket.close();
        } catch (Exception e) {
          System.out.println("Error: " + host.toString() + " disconnected");
        }
      }

      if (successfull()) {
        System.out.println(host.toString() + " source fin");
      } else {
        System.out.println(host.toString() + "failed some");
      }

      file.close();
    } catch (Exception e) {
      System.out.println("Cannot close file");
    }

  }

  public Set<Long> successfullPieces() {
    return successFullPieces;
  }

  public boolean successfull() {
    return failedPieces.size() == 0;
  }

  public DownloadSource failedPieces() {

    return new DownloadSource(downloadSource.getHost(),
        new ArrayList<>(failedPieces));
  }

}
