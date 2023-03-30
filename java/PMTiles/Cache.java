package PMTiles;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Cache {
  CompletableFuture<Header> getHeader(Source source, String currentEtag);

  CompletableFuture<List<Entry>> getDirectory(
    Source source,
    long offset,
    long length,
    Header header
  );

  CompletableFuture<ByteBuffer> getArrayBuffer(
    Source source,
    long offset,
    long length,
    Header header
  );

  CompletableFuture<Void> invalidate(Source source, String currentEtag);
}
