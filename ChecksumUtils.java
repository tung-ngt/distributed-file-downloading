import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class ChecksumUtils {

  public static String getCheckSum(byte[] fragment) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] result = md.digest(fragment);
      StringBuilder finalString = new StringBuilder("");
      for (byte b : result) {
        finalString.append(String.format("%02x", b));
      }
      return finalString.toString();
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }

  }

  public static List<String> getFileCheckSums(String fileName, long pieceSize) {
    try {
      File file = new File(fileName);
      FileInputStream fileStream = new FileInputStream(file);
      long fileSize = file.length();

      List<String> checkSums = new ArrayList<>();

      int currentSize = (int) pieceSize;

      
      for (long i = 0; i < fileSize; i += pieceSize) {
        if (i + pieceSize > fileSize) {
          currentSize = (int) (fileSize - i);
        }

        byte[] buffer = new byte[currentSize];
        
        int bytesRead = 0;
        int accBytesRead = 0;
        while (accBytesRead < currentSize) {

          int noBytesRead = fileStream.read(buffer, 0, currentSize - accBytesRead);

          accBytesRead += noBytesRead;
        }

        String checkSum = getCheckSum(buffer);
        checkSums.add(checkSum);

      }
      fileStream.close();
      return checkSums;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
