package PMTiles;

public class Header {

  private int specVersion;
  private long rootDirectoryOffset;
  private long rootDirectoryLength;
  private long jsonMetadataOffset;
  private long jsonMetadataLength;
  private long leafDirectoryOffset;
  private long leafDirectoryLength;
  private long tileDataOffset;
  private long tileDataLength;
  private long numAddressedTiles;
  private long numTileEntries;
  private long numTileContents;
  private boolean clustered;
  private int internalCompression;
  private int tileCompression;
  private int tileType;
  private int minZoom;
  private int maxZoom;
  private double minLon;
  private double minLat;
  private double maxLon;
  private double maxLat;
  private int centerZoom;
  private double centerLon;
  private double centerLat;
  private String etag;

  public int getSpecVersion() {
    return specVersion;
  }

  public void setSpecVersion(int specVersion) {
    this.specVersion = specVersion;
  }

  public long getRootDirectoryOffset() {
    return rootDirectoryOffset;
  }

  public void setRootDirectoryOffset(long rootDirectoryOffset) {
    this.rootDirectoryOffset = rootDirectoryOffset;
  }

  public long getRootDirectoryLength() {
    return rootDirectoryLength;
  }

  public void setRootDirectoryLength(long rootDirectoryLength) {
    this.rootDirectoryLength = rootDirectoryLength;
  }

  public long getJsonMetadataOffset() {
    return jsonMetadataOffset;
  }

  public void setJsonMetadataOffset(long jsonMetadataOffset) {
    this.jsonMetadataOffset = jsonMetadataOffset;
  }

  public long getJsonMetadataLength() {
    return jsonMetadataLength;
  }

  public void setJsonMetadataLength(long jsonMetadataLength) {
    this.jsonMetadataLength = jsonMetadataLength;
  }

  public long getLeafDirectoryOffset() {
    return leafDirectoryOffset;
  }

  public void setLeafDirectoryOffset(long leafDirectoryOffset) {
    this.leafDirectoryOffset = leafDirectoryOffset;
  }

  public long getLeafDirectoryLength() {
    return leafDirectoryLength;
  }

  public void setLeafDirectoryLength(long leafDirectoryLength) {
    this.leafDirectoryLength = leafDirectoryLength;
  }

  public long getTileDataOffset() {
    return tileDataOffset;
  }

  public void setTileDataOffset(long tileDataOffset) {
    this.tileDataOffset = tileDataOffset;
  }

  public long getTileDataLength() {
    return tileDataLength;
  }

  public void setTileDataLength(long tileDataLength) {
    this.tileDataLength = tileDataLength;
  }

  public long getNumAddressedTiles() {
    return numAddressedTiles;
  }

  public void setNumAddressedTiles(long numAddressedTiles) {
    this.numAddressedTiles = numAddressedTiles;
  }

  public long getNumTileEntries() {
    return numTileEntries;
  }

  public void setNumTileEntries(long numTileEntries) {
    this.numTileEntries = numTileEntries;
  }

  public long getNumTileContents() {
    return numTileContents;
  }

  public void setNumTileContents(long numTileContents) {
    this.numTileContents = numTileContents;
  }

  public boolean isClustered() {
    return clustered;
  }

  public void setClustered(boolean clustered) {
    this.clustered = clustered;
  }

  public int getInternalCompression() {
    return internalCompression;
  }

  public void setInternalCompression(int internalCompression) {
    this.internalCompression = internalCompression;
  }

  public int getTileCompression() {
    return tileCompression;
  }

  public void setTileCompression(int tileCompression) {
    this.tileCompression = tileCompression;
  }

  public int getTileType() {
    return tileType;
  }

  public void setTileType(int tileType) {
    this.tileType = tileType;
  }

  public int getMinZoom() {
    return minZoom;
  }

  public void setMinZoom(int minZoom) {
    this.minZoom = minZoom;
  }

  public int getMaxZoom() {
    return maxZoom;
  }

  public void setMaxZoom(int maxZoom) {
    this.maxZoom = maxZoom;
  }

  public double getMinLon() {
    return minLon;
  }

  public void setMinLon(double minLon) {
    this.minLon = minLon;
  }

  public double getMinLat() {
    return minLat;
  }

  public void setMinLat(double minLat) {
    this.minLat = minLat;
  }

  public double getMaxLon() {
    return maxLon;
  }

  public void setMaxLon(double maxLon) {
    this.maxLon = maxLon;
  }

  public double getMaxLat() {
    return maxLat;
  }

  public void setMaxLat(double maxLat) {
    this.maxLat = maxLat;
  }

  public int getCenterZoom() {
    return centerZoom;
  }

  public void setCenterZoom(int centerZoom) {
    this.centerZoom = centerZoom;
  }

  public double getCenterLon() {
    return centerLon;
  }

  public void setCenterLon(double centerLon) {
    this.centerLon = centerLon;
  }

  public double getCenterLat() {
    return centerLat;
  }

  public void setCenterLat(double centerLat) {
    this.centerLat = centerLat;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }
}
