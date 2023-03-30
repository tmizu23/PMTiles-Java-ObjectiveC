package PMTiles;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class SharedPromiseCacheValue<T> {

  private long lastUsed;
  private CompletableFuture<T> data;

  public SharedPromiseCacheValue(long lastUsed, CompletableFuture<T> data) {
    this.lastUsed = lastUsed;
    this.data = data;
  }

  public long getLastUsed() {
    return lastUsed;
  }

  public void setLastUsed(long lastUsed) {
    this.lastUsed = lastUsed;
  }

  public CompletableFuture<T> getData() {
    return data;
  }

  public void setData(CompletableFuture<T> data) {
    this.data = data;
  }
}

public class SharedPromiseCache implements Cache {

  private final Map<String, SharedPromiseCacheValue> cache;
  private final int maxCacheEntries;
  private long counter;
  private final boolean prefetch;
  private final DecompressFunc decompress;

  public SharedPromiseCache(
    int maxCacheEntries,
    boolean prefetch,
    DecompressFunc decompress
  ) {
    this.cache = new HashMap<String, SharedPromiseCacheValue>();
    this.maxCacheEntries = maxCacheEntries;
    this.counter = 1;
    this.prefetch = prefetch;
    this.decompress = decompress;
  }

  public CompletableFuture<Header> getHeader(
    Source source,
    String currentEtag
  ) {
    String cacheKey = source.getKey();
    if (cache.containsKey(cacheKey)) {
      cache.get(cacheKey).setLastUsed(counter++);
      return (CompletableFuture<Header>) cache.get(cacheKey).getData();
    }

    CompletableFuture<Header> promise = PMTilesUtils
      .getHeaderAndRoot(source, prefetch, currentEtag)
      .thenApply(res -> {
        if (res.getValue() != null) {
          Optional<RootDirData> rootDirDataOpt = res.getValue();

          cache.put(
            rootDirDataOpt.get().getDirKey(),
            new SharedPromiseCacheValue(
              counter++,
              CompletableFuture.completedFuture(
                rootDirDataOpt.get().getEntries()
              )
            )
          );
        }
        prune();
        return res.getKey();
      });

    cache.put(cacheKey, new SharedPromiseCacheValue(counter++, promise));

    return promise;
  }

  public CompletableFuture<List<Entry>> getDirectory(
    Source source,
    long offset,
    long length,
    Header header
  ) {
    String cacheKey =
      source.getKey() +
      "|" +
      (header.getEtag() != null ? header.getEtag() : "") +
      "|" +
      offset +
      "|" +
      length;
    //System.out.println("getDirectory: " + cacheKey);
    if (cache.containsKey(cacheKey)) {
      //System.out.println("getDirectory: " + cacheKey + " - cache hit");
      cache.get(cacheKey).setLastUsed(counter++);
      return (CompletableFuture<List<Entry>>) cache.get(cacheKey).getData();
    }

    CompletableFuture<List<Entry>> promise = PMTilesUtils.getDirectory(
      source,
      decompress,
      offset,
      length,
      header
    );

    //System.out.println("getDirectory: " + cacheKey);
    cache.put(cacheKey, new SharedPromiseCacheValue(counter++, promise));
    prune();
    //System.out.println("cache" + cache);
    return promise;
  }

  public CompletableFuture<ByteBuffer> getArrayBuffer(
    Source source,
    long offset,
    long length,
    Header header
  ) {
    String cacheKey =
      source.getKey() +
      "|" +
      (header.getEtag() != null ? header.getEtag() : "") +
      "|" +
      offset +
      "|" +
      length;
    if (cache.containsKey(cacheKey)) {
      cache.get(cacheKey).setLastUsed(counter++);
      return (CompletableFuture<ByteBuffer>) cache.get(cacheKey).getData();
    }

    CompletableFuture<ByteBuffer> promise = source
      .getBytes(offset, length)
      .thenApply(resp -> {
        if (header.getEtag() != null && !header.getEtag().equals(resp.etag)) {
          throw new EtagMismatch(resp.etag);
        }
        prune();
        return resp.data;
      });

    cache.put(cacheKey, new SharedPromiseCacheValue(counter++, promise));
    return promise;
  }

  private void prune() {
    if (cache.size() >= maxCacheEntries) {
      long minUsed = Long.MAX_VALUE;
      String minKey = null;
      for (Map.Entry<String, SharedPromiseCacheValue> entry : cache.entrySet()) {
        if (entry.getValue().getLastUsed() < minUsed) {
          minUsed = entry.getValue().getLastUsed();
          minKey = entry.getKey();
        }
      }
      if (minKey != null) {
        cache.remove(minKey);
      }
    }
  }

  public CompletableFuture<Void> invalidate(Source source, String currentEtag) {
    cache.remove(source.getKey());
    return getHeader(source, currentEtag).thenAccept(header -> {});
  }
}
