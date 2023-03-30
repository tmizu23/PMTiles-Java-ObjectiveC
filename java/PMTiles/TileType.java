package PMTiles;

public enum TileType {
  Unknown(0),
  Mvt(1),
  Png(2),
  Jpeg(3),
  Webp(4);

  private final int value;

  TileType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
