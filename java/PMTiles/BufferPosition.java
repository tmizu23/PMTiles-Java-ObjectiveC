package PMTiles;

import java.nio.ByteBuffer;

public class BufferPosition {

  public ByteBuffer buf;
  public int pos;

  public BufferPosition(ByteBuffer buf, int pos) {
    this.buf = buf;
    this.pos = pos;
  }
}
