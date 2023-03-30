package PMTiles;

import java.util.concurrent.CompletableFuture;

public interface Source {
  CompletableFuture<RangeResponse> getBytes(long offset, long length);

  String getKey();
}
