package PMTiles;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class PMTilesUtils {

  static final int HEADER_SIZE_BYTES = 127;

  public static Header bytesToHeader(ByteBuffer bytes, String etag) {
    bytes.order(ByteOrder.LITTLE_ENDIAN);
    // for (int i = 0; i < bytes.limit(); i++) {
    //   System.out.printf("%X ", bytes.get(i));
    // }
    int specVersion = bytes.get(7);
    //System.out.println("specVersion: " + specVersion);
    if (specVersion > 3) {
      throw new RuntimeException(
        "Archive is spec version " + specVersion + " but this library supports up to spec version 3"
      );
    }

    Header header = new Header();
    header.setSpecVersion(specVersion);
    header.setRootDirectoryOffset(getUint64(bytes, 8));
    header.setRootDirectoryLength(getUint64(bytes, 16));
    header.setJsonMetadataOffset(getUint64(bytes, 24));
    header.setJsonMetadataLength(getUint64(bytes, 32));
    header.setLeafDirectoryOffset(getUint64(bytes, 40));
    header.setLeafDirectoryLength(getUint64(bytes, 48));
    header.setTileDataOffset(getUint64(bytes, 56));
    header.setTileDataLength(getUint64(bytes, 64));
    header.setNumAddressedTiles(getUint64(bytes, 72));
    header.setNumTileEntries(getUint64(bytes, 80));
    header.setNumTileContents(getUint64(bytes, 88));
    header.setClustered(bytes.get(96) == 1);
    header.setInternalCompression(bytes.get(97));
    header.setTileCompression(bytes.get(98));
    header.setTileType(bytes.get(99));
    header.setMinZoom(bytes.get(100));
    header.setMaxZoom(bytes.get(101));
    header.setMinLon(bytes.getInt(102) / 10000000.0);
    header.setMinLat(bytes.getInt(106) / 10000000.0);
    header.setMaxLon(bytes.getInt(110) / 10000000.0);
    header.setMaxLat(bytes.getInt(114) / 10000000.0);
    header.setCenterZoom(bytes.get(118));
    header.setCenterLon(bytes.getInt(119) / 10000000.0);
    header.setCenterLat(bytes.getInt(123) / 10000000.0);
    header.setEtag(etag);

    return header;
  }

  private static long getUint64(ByteBuffer buffer, int offset) {
    long wh = (long) buffer.getInt(offset + 4) & 0xFFFFFFFFL;
    long wl = (long) buffer.getInt(offset + 0) & 0xFFFFFFFFL;
    return wh * (long) Math.pow(2, 32) + wl;
  }

  static long toNum(int low, int high) {
    long result = (((long) high) << 32) | (((long) low) & 0xffffffffL);
    //System.out.println("toNum: " + low + " " + high + " " + result);
    return result;
  }

  static long readVarintRemainder(int l, BufferPosition p) {
    ByteBuffer buf = p.buf;
    int h, b;
    b = buf.get(p.pos++) & 0xFF;
    h = (b & 0x70) >> 4;
    if (b < 0x80) return toNum(l, h);
    b = buf.get(p.pos++) & 0xFF;
    h |= (b & 0x7f) << 3;
    if (b < 0x80) return toNum(l, h);
    b = buf.get(p.pos++) & 0xFF;
    h |= (b & 0x7f) << 10;
    if (b < 0x80) return toNum(l, h);
    b = buf.get(p.pos++) & 0xFF;
    h |= (b & 0x7f) << 17;
    if (b < 0x80) return toNum(l, h);
    b = buf.get(p.pos++) & 0xFF;
    h |= (b & 0x7f) << 24;
    if (b < 0x80) return toNum(l, h);
    b = buf.get(p.pos++) & 0xFF;
    h |= (b & 0x01) << 31;
    if (b < 0x80) return toNum(l, h);
    throw new RuntimeException("Expected varint not more than 10 bytes");
  }

  static long readVarint(BufferPosition p) {
    ByteBuffer buf = p.buf;
    int val, b;

    b = buf.get(p.pos++) & 0xFF;
    val = b & 0x7f;
    if (b < 0x80) return val;
    b = buf.get(p.pos++) & 0xFF;
    val |= (b & 0x7f) << 7;
    if (b < 0x80) return val;
    b = buf.get(p.pos++) & 0xFF;
    val |= (b & 0x7f) << 14;
    if (b < 0x80) return val;
    b = buf.get(p.pos++) & 0xFF;
    val |= (b & 0x7f) << 21;
    if (b < 0x80) return val;
    b = buf.get(p.pos) & 0xFF;
    val |= (b & 0x0f) << 28;
    //System.out.println("readVarintRemainder: " + val);
    //System.out.println("p pos: " + p.pos);
    return readVarintRemainder(val, p);
  }

  public static List<Entry> deserializeIndex(ByteBuffer buffer) {
    BufferPosition p = new BufferPosition(buffer, 0);
    //System.out.println("buffer length (capacity): " + p.buf.capacity());
    int numEntries = (int) readVarint(p);
    //System.out.println("numEntries: " + numEntries);
    List<Entry> entries = new ArrayList<>();
    // System.out.println("buffer position: " + p.pos);
    // for (int i = 0; i < p.buf.limit(); i++) {
    //   System.out.printf("%d ", p.buf.get(i) & 0xFF);
    // }
    long lastId = 0;
    for (int i = 0; i < numEntries; i++) {
      long v = readVarint(p);
      //System.out.println("tileId:" + (lastId + v));
      entries.add(new Entry(lastId + v, 0, 0, 1));
      lastId += v;
    }

    for (int i = 0; i < numEntries; i++) {
      entries.get(i).runLength = (int) readVarint(p);
    }

    for (int i = 0; i < numEntries; i++) {
      entries.get(i).length = (int) readVarint(p);
    }

    for (int i = 0; i < numEntries; i++) {
      int v = (int) readVarint(p);
      if (v == 0 && i > 0) {
        entries.get(i).offset = entries.get(i - 1).offset + entries.get(i - 1).length;
      } else {
        entries.get(i).offset = v - 1;
      }
      //System.out.println("tileId:" + entries.get(i).tileId);
      //System.out.println("runLength:" + entries.get(i).runLength);
    }

    return entries;
  }

  public static CompletableFuture<SimpleEntry<Header, Optional<RootDirData>>> getHeaderAndRoot(
    Source source,
    boolean prefetch,
    String currentEtag
  ) {
    try {
      //System.out.println("Fetching header from " + source.getKey());
      RangeResponse resp = source.getBytes(0, 16384).get();

      //System.out.println(resp);
      ByteBuffer buffer = resp.data;
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      if (buffer.getShort(0) != 0x4d50) {
        throw new RuntimeException("Wrong magic number for PMTiles archive");
      }

      ByteBuffer headerData = (ByteBuffer) buffer.slice().limit(HEADER_SIZE_BYTES);
      String respEtag = resp.etag;
      if (currentEtag != null && !resp.etag.equals(currentEtag)) {
        System.err.println(
          "ETag conflict detected; your HTTP server might not support content-based ETag headers. ETags disabled for " +
          source.getKey()
        );
        respEtag = null;
      }
      // System.out.println("respEtag: " + respEtag);
      Header header = bytesToHeader(headerData, respEtag);
      //System.out.println("header: " + header);
      if (prefetch) {
        //System.out.println(header.getRootDirectoryOffset());
        int rootDirOffset = (int) header.getRootDirectoryOffset();
        int rootDirLength = (int) header.getRootDirectoryLength();
        //System.out.println("rootDirOffset: " + rootDirOffset);
        //System.out.println("rootDirLength: " + rootDirLength);
        ByteBuffer rootDirData = (ByteBuffer) buffer.position(rootDirOffset);
        rootDirData = (ByteBuffer) rootDirData.slice().limit(rootDirLength);

        String dirKey =
          source.getKey() +
          "|" +
          (header.getEtag() != null ? header.getEtag() : "") +
          "|" +
          rootDirOffset +
          "|" +
          rootDirLength;

        ByteBuffer deserialized = Decompress.decompress(rootDirData, header.getInternalCompression());
        //deserialized.order(ByteOrder.LITTLE_ENDIAN);
        // for (int i = 0; i < deserialized.limit(); i++) {
        //   System.out.printf("%d ", deserialized.get(i) & 0xFF);
        // }
        List<Entry> rootDir = deserializeIndex(deserialized);
        //System.out.println("dirKey: " + dirKey);
        //System.out.println("rootDirSize: " + rootDir.size());
        //System.out.println("rootDir: " + rootDir);

        return CompletableFuture.completedFuture(
          new SimpleEntry<>(header, Optional.of(new RootDirData(dirKey, rootDir.size(), rootDir)))
        );
      }

      return CompletableFuture.completedFuture(new SimpleEntry<>(header, Optional.empty()));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static CompletableFuture<List<Entry>> getDirectory(
    Source source,
    DecompressFunc decompress,
    long offset,
    long length,
    Header header
  ) {
    //System.out.println("offset: " + offset);
    //System.out.println("length: " + length);
    return source
      .getBytes(offset, length)
      .thenApply(resp -> {
        if (header.getEtag() != null && !header.getEtag().equals(resp.etag)) {
          throw new EtagMismatch(resp.etag);
        }

        return decompress.decompress(resp.data, header.getInternalCompression());
      })
      .thenApply(data -> {
        List<Entry> directory = deserializeIndex(data);
        if (directory.isEmpty()) {
          throw new RuntimeException("Empty directory is invalid");
        }

        return directory;
      });
  }

  private static final long[] tzValues = {
    0,
    1,
    5,
    21,
    85,
    341,
    1365,
    5461,
    21845,
    87381,
    349525,
    1398101,
    5592405,
    22369621,
    89478485,
    357913941,
    1431655765,
    5726623061L,
    22906492245L,
    91625968981L,
    366503875925L,
    1466015503701L,
    5864062014805L,
    23456248059221L,
    93824992236885L,
    375299968947541L,
    1501199875790165L,
  };

  public static void rotate(long n, long[] xy, long rx, long ry) {
    if (ry == 0) {
      if (rx == 1) {
        xy[0] = n - 1 - xy[0];
        xy[1] = n - 1 - xy[1];
      }
      long t = xy[0];
      xy[0] = xy[1];
      xy[1] = t;
    }
  }

  public static int[] idOnLevel(int z, long pos) {
    int n = (int) Math.pow(2, z);
    long rx = pos;
    long ry = pos;
    long t = pos;
    long[] xy = { 0, 0 };
    long s = 1;
    while (s < n) {
      rx = 1 & (t / 2);
      ry = 1 & (t ^ rx);
      rotate(s, xy, rx, ry);
      xy[0] += s * rx;
      xy[1] += s * ry;
      t = t / 4;
      s *= 2;
    }
    return new int[] { z, (int) xy[0], (int) xy[1] };
  }

  public static long zxyToTileId(int z, int x, int y) {
    if (z > 26) {
      throw new Error("Tile zoom level exceeds max safe number limit (26)");
    }
    if (x > Math.pow(2, z) - 1 || y > Math.pow(2, z) - 1) {
      throw new Error("tile x/y outside zoom level bounds");
    }

    long acc = tzValues[z];
    long n = (long) Math.pow(2, z);
    long rx = 0;
    long ry = 0;
    long d = 0;
    long[] xy = { x, y };
    long s = n / 2;
    while (s > 0) {
      rx = (xy[0] & s) > 0 ? 1 : 0;
      ry = (xy[1] & s) > 0 ? 1 : 0;
      d += s * s * ((3 * rx) ^ ry);
      rotate(s, xy, rx, ry);
      s = s / 2;
    }
    return acc + d;
  }

  public static int[] tileIdToZxy(long i) {
    long acc = 0;
    int z;

    for (z = 0; z < 27; z++) {
      long num_tiles = (long) Math.pow(2, z) * (long) Math.pow(2, z);
      if (acc + num_tiles > i) {
        return idOnLevel(z, i - acc);
      }
      acc += num_tiles;
    }

    throw new Error("Tile zoom level exceeds max safe number limit (26)");
  }

  public static Entry findTile(List<Entry> entries, long tileId) {
    int m = 0;
    int n = entries.size() - 1;
    //System.out.println("entries.size() = " + entries.size());
    //System.out.println("tileId = " + tileId);
    while (m <= n) {
      int k = (n + m) >> 1;
      //System.out.println("k = " + k);
      //System.out.println("entries.get(k).tileId = " + entries.get(k).tileId);
      // System.out.println(
      //   "entries.get(k).runLength = " + entries.get(k).runLength
      // );
      // System.out.println("entries.get(k).offset = " + entries.get(k).offset);

      int cmp = Long.compare(tileId, entries.get(k).tileId);
      if (cmp > 0) {
        m = k + 1;
      } else if (cmp < 0) {
        n = k - 1;
      } else {
        //System.out.println("found");
        return entries.get(k);
      }
    }
    //System.out.println("n = " + n);
    // System.out.println(
    //   "entries.get(n).runLength = " + entries.get(n).runLength
    // );
    // at this point, m > n
    if (n >= 0) {
      if (entries.get(n).runLength == 0) {
        return entries.get(n);
      }
      if (tileId - entries.get(n).tileId < entries.get(n).runLength) {
        return entries.get(n);
      }
    }
    return null;
  }
}
