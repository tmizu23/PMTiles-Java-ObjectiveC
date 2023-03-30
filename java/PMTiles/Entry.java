package PMTiles;

public class Entry {

  public long tileId;
  public long offset;
  public int length;
  public int runLength;

  public Entry(long tileId, long offset, int length, int runLength) {
    this.tileId = tileId;
    this.offset = offset;
    this.length = length;
    this.runLength = runLength;
  }

  // Getter and Setter methods
  public long getTileId() {
    return tileId;
  }

  public void setTileId(long tileId) {
    this.tileId = tileId;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public int getRunLength() {
    return runLength;
  }

  public void setRunLength(int runLength) {
    this.runLength = runLength;
  }

  @Override
  public String toString() {
    return (
      "Entry{" +
      "tileId=" +
      tileId +
      ", offset=" +
      offset +
      ", length=" +
      length +
      ", runLength=" +
      runLength +
      '}'
    );
  }
}
