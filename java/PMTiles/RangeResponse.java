package PMTiles;

import java.nio.ByteBuffer;

public class RangeResponse {

  public ByteBuffer data;
  public String etag;
  public String cacheControl;
  public String expires;

  public RangeResponse(
    ByteBuffer data,
    String etag,
    String cacheControl,
    String expires
  ) {
    this.data = data;
    this.etag = etag;
    this.cacheControl = cacheControl;
    this.expires = expires;
  }
}
