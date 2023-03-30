package PMTiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

public class Decompress {

  public static ByteBuffer decompress(ByteBuffer data, int compression)
    throws IOException {
    if (compression == 1 || compression == 0) {
      return data;
    } else if (compression == 2) {
      return decompressGzip(data);
    } else {
      throw new IllegalArgumentException("Compression method not supported");
    }
  }

  private static ByteBuffer decompressGzip(ByteBuffer data) throws IOException {
    //System.out.println("Decompressing GZIP");
    //System.out.println("Data length: " + data);
    byte[] inputData = new byte[data.remaining()];
    data.get(inputData);

    try (
      ByteArrayInputStream bais = new ByteArrayInputStream(inputData);
      GZIPInputStream gzipInputStream = new GZIPInputStream(bais);
      ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ) {
      byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }

      return ByteBuffer.wrap(baos.toByteArray());
    }
  }
}
