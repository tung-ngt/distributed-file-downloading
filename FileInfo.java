public class FileInfo {
  private String fileName;
  private long fileSize;
  private String checkSum;
  private Host originalOwner;

  public FileInfo(String fileName, long fileSize, String checkSum, Host originalOwner) {
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.checkSum = checkSum;
    this.originalOwner = originalOwner;

  }

  public long getFileSize() {
    return fileSize;
  }

  public String getCheckSum() {
    return checkSum;
  }

  public String getFileName() {
    return fileName;
  }

  public Host getOriginalOwner() {
    return originalOwner;
  }

}
