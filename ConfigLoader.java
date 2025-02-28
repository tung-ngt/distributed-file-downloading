import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {

  private Map<String, String> envMap;

  public ConfigLoader() {
    envMap = new HashMap<>();
  }

  public void load(String filePath) {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty() || line.startsWith("#")) {
          continue; // Skip empty lines and comments
        }
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
          envMap.put(parts[0].trim(), parts[1].trim());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String get(String key) {
    return envMap.get(key);
  }
}
