package PMTiles;

//import android.util.Log;
import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class PMTiles {

  private final Source source;
  private final Cache cache;
  private final DecompressFunc decompress;

  public PMTiles(Object source, Cache cache, DecompressFunc decompress) {
    if (source instanceof String) {
      this.source = new FetchSource((String) source);
    } else {
      this.source = (Source) source;
    }
    if (decompress != null) {
      this.decompress = decompress;
    } else {
      this.decompress =
        (data, compression) -> {
          try {
            return Decompress.decompress(data, compression);
          } catch (IOException e) {
            throw new RuntimeException("Decompression failed", e);
          }
        };
    }
    if (cache != null) {
      this.cache = cache;
    } else {
      this.cache = new SharedPromiseCache(100, true, this.decompress);
    }
  }

  public CompletableFuture<Header> getHeader() {
    return cache.getHeader(source, null);
  }

  public CompletableFuture<Optional<RangeResponse>> getZxyAttempt(
    int z,
    int x,
    int y
  ) {
    long tileId = PMTilesUtils.zxyToTileId(z, x, y);
    //Log.v("MainActivity", "tileId: " + tileId + " " + z + " " + x + " " + y);
    //System.out.println("tileId: " + tileId + " " + z + " " + x + " " + y);
    CompletableFuture<Header> headerFuture = cache.getHeader(source, null);

    return headerFuture.thenCompose(header -> {
      if (z < header.getMinZoom() || z > header.getMaxZoom()) {
        return CompletableFuture.completedFuture(Optional.empty());
      }

      long directoryOffset = header.getRootDirectoryOffset();
      long directoryLength = header.getRootDirectoryLength();
      //   System.out.println("directoryOffset: " + directoryOffset);
      //   System.out.println("directoryLength: " + directoryLength);
      Optional<RangeResponse> r = searchDirectoryForTile(
        tileId,
        header,
        directoryOffset,
        directoryLength
      );
      return CompletableFuture.completedFuture(r);
    });
  }

  private Optional<RangeResponse> searchDirectoryForTile(
    long tileId,
    Header header,
    long directoryOffset,
    long directoryLength
  ) {
    //System.out.println("searchDirectoryForTile: " + tileId);
    for (int depth = 0; depth <= 3; depth++) {
      //System.out.println("depth: " + depth);
      List<Entry> directory = cache
        .getDirectory(source, directoryOffset, directoryLength, header)
        .join(); // Assuming getDirectory returns CompletableFuture<Directory>
      //System.out.println("directory: " + directory);
      Entry entry = PMTilesUtils.findTile(directory, tileId);

      if (entry != null) {
        if (entry.getRunLength() > 0) {
          RangeResponse resp = source
            .getBytes(
              header.getTileDataOffset() + entry.getOffset(),
              entry.getLength()
            )
            .join(); // Assuming getBytes returns CompletableFuture<Resp>

          if (header.getEtag() != null && !header.getEtag().equals(resp.etag)) {
            throw new EtagMismatch(resp.etag);
          }

          RangeResponse rangeResponse = new RangeResponse(
            this.decompress.decompress(resp.data, header.getTileCompression()), // Assuming decompress returns byte[]
            null,
            resp.cacheControl,
            resp.expires
          );
          //System.out.println("found entry: " + entry);
          return Optional.of(rangeResponse);
        } else {
          directoryOffset = header.getLeafDirectoryOffset() + entry.getOffset();
          directoryLength = entry.getLength();
          //System.out.println("next directoryOffset: " + directoryOffset);
        }
      } else {
        return Optional.empty();
      }
    }
    throw new RuntimeException("Maximum directory depth exceeded");
  }

  public CompletableFuture<Optional<RangeResponse>> getZxy(
    int z,
    int x,
    int y
  ) {
    return getZxyAttempt(z, x, y)
      .exceptionally(ex -> {
        if (ex.getCause() instanceof EtagMismatch) {
          cache.invalidate(source, ex.getMessage());
          return getZxyAttempt(z, x, y).join();
        } else {
          System.out.println("Exception: " + ex);
          throw new RuntimeException(ex);
        }
      });
  }

  public CompletableFuture<Object> getMetadataAttempt() {
    return cache
      .getHeader(source, null)
      .thenApply(header -> {
        RangeResponse resp;
        try {
          resp =
            source
              .getBytes(
                header.getJsonMetadataOffset(),
                header.getJsonMetadataLength()
              )
              .get();
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }

        if (header.getEtag() != null && !header.getEtag().equals(resp.etag)) {
          return CompletableFuture.failedFuture(new EtagMismatch(resp.etag));
        }

        ByteBuffer decompressed =
          this.decompress.decompress(
              resp.data,
              header.getInternalCompression()
            );
        CharsetDecoder decoder = Charset
          .forName("UTF-8")
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
          return decoder.decode(decompressed).toString();
        } catch (CharacterCodingException e) {
          throw new RuntimeException(e);
        }
      });
  }

  public CompletableFuture<Object> getMetadata() {
    return getMetadataAttempt()
      .exceptionally(ex -> {
        if (ex.getCause() instanceof EtagMismatch) {
          cache.invalidate(source, ex.getMessage());
          return getMetadataAttempt().join();
        } else {
          throw new RuntimeException(ex);
        }
      });
  }
}
