package PMTiles;

public enum Compression {
  Unknown(0),
  None(1),
  Gzip(2),
  Brotli(3),
  Zstd(4);

  private final int value;

  Compression(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
