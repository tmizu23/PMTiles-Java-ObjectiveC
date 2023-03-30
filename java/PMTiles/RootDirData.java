package PMTiles;

import java.util.List;

public class RootDirData {

  private String dirKey;
  private int length;
  private List<Entry> entries;

  public RootDirData(String dirKey, int length, List<Entry> entries) {
    this.dirKey = dirKey;
    this.length = length;
    this.entries = entries;
  }

  public String getDirKey() {
    return dirKey;
  }

  public int getLength() {
    return length;
  }

  public List<Entry> getEntries() {
    return entries;
  }
}
