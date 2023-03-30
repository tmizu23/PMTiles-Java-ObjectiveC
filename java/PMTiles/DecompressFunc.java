package PMTiles;

import java.nio.ByteBuffer;

public interface DecompressFunc {
  ByteBuffer decompress(ByteBuffer buf, int compression);
}
