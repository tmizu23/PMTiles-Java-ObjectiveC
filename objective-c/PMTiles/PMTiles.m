#import "PMTiles.h"
#import "Decompress.h"
#import "FetchSource.h"
// #import "SharedCache.h"
#import "VarintHelper.h"
#import <Foundation/Foundation.h>
@implementation PMTiles

- (instancetype)initWithURL:(NSString *)urlString {
  self = [super init];
  if (self) {
    self.urlString = urlString;
    self.fetchSource = [[FetchSource alloc] initWithURL:urlString];
    self.sharedCache = [[SharedCache alloc] init];
  }
  return self;
}

const uint8_t TILETYPE_UNKNOWN = 0x0;
const uint8_t TILETYPE_MVT = 0x1;
const uint8_t TILETYPE_PNG = 0x2;
const uint8_t TILETYPE_JPEG = 0x3;
const uint8_t TILETYPE_WEBP = 0x4;

const uint8_t COMPRESSION_UNKNOWN = 0x0;
const uint8_t COMPRESSION_NONE = 0x1;
const uint8_t COMPRESSION_GZIP = 0x2;
const uint8_t COMPRESSION_BROTLI = 0x3;
const uint8_t COMPRESSION_ZSTD = 0x4;

ZXY ZXYMake(uint8_t z, uint32_t x, uint32_t y) {
  ZXY result;
  result.z = z;
  result.x = x;
  result.y = y;
  return result;
}
EntryV3 EntryV3Make(uint64_t tile_id, uint64_t offset, uint32_t length,
                    uint32_t run_length) {
  EntryV3 result;
  result.tile_id = tile_id;
  result.offset = offset;
  result.length = length;
  result.run_length = run_length;
  return result;
}

EntryZXY EntryZXYMake(uint8_t z, uint32_t x, uint32_t y, uint64_t offset,
                      uint32_t length) {
  EntryZXY result;
  result.z = z;
  result.x = x;
  result.y = y;
  result.offset = offset;
  result.length = length;
  return result;
}

NS_INLINE NSComparisonResult EntryV3Compare(EntryV3 a, EntryV3 b) {
  if (a.tile_id < b.tile_id) {
    return NSOrderedAscending;
  } else if (a.tile_id > b.tile_id) {
    return NSOrderedDescending;
  } else {
    return NSOrderedSame;
  }
}

NS_INLINE NSComparisonResult EntryZXYCompare(EntryZXY a, EntryZXY b) {
  if (a.z != b.z) {
    return a.z < b.z ? NSOrderedAscending : NSOrderedDescending;
  }
  if (a.x != b.x) {
    return a.x < b.x ? NSOrderedAscending : NSOrderedDescending;
  }
  if (a.y != b.y) {
    return a.y < b.y ? NSOrderedAscending : NSOrderedDescending;
  }
  return NSOrderedSame;
}

+ (ZXY)tileIDToZXY:(uint64_t)tileID {
  uint64_t acc = 0;
  for (uint8_t t_z = 0; t_z < 32; t_z++) {
    uint64_t num_tiles = (1LL << t_z) * (1LL << t_z);
    if (acc + num_tiles > tileID) {
      return [VarintHelper t_on_level:t_z position:tileID - acc];
    }
    acc += num_tiles;
  }
  @throw [NSException exceptionWithName:NSRangeException
                                 reason:@"Tile zoom exceeds 64-bit limit"
                               userInfo:nil];
}

+ (uint64_t)zxyToTileID:(uint8_t)z x:(uint32_t)x y:(uint32_t)y {
  if (z > 31) {
    @throw [NSException exceptionWithName:NSGenericException
                                   reason:@"Tile zoom exceeds 64-bit limit"
                                 userInfo:nil];
  }
  if (x > (1 << z) - 1 || y > (1 << z) - 1) {
    @throw [NSException exceptionWithName:NSGenericException
                                   reason:@"Tile x/y outside zoom level bounds "
                                 userInfo:nil];
  }
  uint64_t acc = 0;
  for (uint8_t t_z = 0; t_z < z; t_z++) {
    acc += (1LL << t_z) * (1LL << t_z);
  }
  int64_t n = 1LL << z;
  int64_t rx, ry, s, d = 0;
  int64_t tx = x;
  int64_t ty = y;
  for (s = n / 2; s > 0; s /= 2) {
    rx = (tx & s) > 0;
    ry = (ty & s) > 0;
    d += s * s * ((3LL * rx) ^ ry);
    [VarintHelper rotate:s x:&tx y:&ty rx:rx ry:ry];
  }
  return acc + d;
}

+ (NSString *)serializeDirectory:(NSArray<NSValue *> *)entries {
  NSMutableString *data = [NSMutableString string];

  [VarintHelper writeVarint:data value:entries.count];

  uint64_t lastID = 0;
  for (NSValue *entryValue in entries) {
    EntryV3 entry;
    [entryValue getValue:&entry];
    [VarintHelper writeVarint:data value:(entry.tile_id - lastID)];
    lastID = entry.tile_id;
  }

  for (NSValue *entryValue in entries) {
    EntryV3 entry;
    [entryValue getValue:&entry];
    [VarintHelper writeVarint:data value:entry.run_length];
  }

  for (NSValue *entryValue in entries) {
    EntryV3 entry;
    [entryValue getValue:&entry];
    [VarintHelper writeVarint:data value:entry.length];
  }

  for (NSUInteger i = 0; i < entries.count; i++) {
    EntryV3 entry, prevEntry;
    [entries[i] getValue:&entry];
    if (i > 0) {
      [entries[i - 1] getValue:&prevEntry];
    }
    if (i > 0 && entry.offset == prevEntry.offset + prevEntry.length) {
      [VarintHelper writeVarint:data value:0];
    } else {
      [VarintHelper writeVarint:data value:(entry.offset + 1)];
    }
  }

  return [NSString stringWithString:data];
}

+ (NSArray<NSValue *> *)deserializeDirectory:(NSData *)decompressed {
  const char *t = (const char *)decompressed.bytes;
  const char *end = t + decompressed.length;

  uint64_t numEntries = [VarintHelper decodeVarint:&t end:end];

  NSMutableArray<NSValue *> *result =
      [NSMutableArray arrayWithCapacity:numEntries];

  uint64_t lastID = 0;
  for (NSUInteger i = 0; i < numEntries; i++) {
    uint64_t tileID = lastID + [VarintHelper decodeVarint:&t end:end];
    EntryV3 entry = {tileID, 0, 0, 0};
    [result addObject:[NSValue valueWithBytes:&entry
                                     objCType:@encode(EntryV3)]];
    lastID = tileID;
  }

  for (NSUInteger i = 0; i < numEntries; i++) {
    EntryV3 entry;
    [result[i] getValue:&entry];
    entry.run_length = [VarintHelper decodeVarint:&t end:end];
    result[i] = [NSValue valueWithBytes:&entry objCType:@encode(EntryV3)];
  }

  for (NSUInteger i = 0; i < numEntries; i++) {
    EntryV3 entry;
    [result[i] getValue:&entry];
    entry.length = [VarintHelper decodeVarint:&t end:end];
    result[i] = [NSValue valueWithBytes:&entry objCType:@encode(EntryV3)];
  }

  for (NSUInteger i = 0; i < numEntries; i++) {
    uint64_t tmp = [VarintHelper decodeVarint:&t end:end];
    EntryV3 entry, prevEntry;
    [result[i] getValue:&entry];
    if (i > 0) {
      [result[i - 1] getValue:&prevEntry];
    }
    if (i > 0 && tmp == 0) {
      entry.offset = prevEntry.offset + prevEntry.length;
    } else {
      entry.offset = tmp - 1;
    }
    result[i] = [NSValue valueWithBytes:&entry objCType:@encode(EntryV3)];
  }

  // assert the directory has been fully consumed
  if (t != end) {
    fprintf(stderr, "Error: malformed pmtiles directory\n");
    exit(EXIT_FAILURE);
  }

  return [NSArray arrayWithArray:result];
}

+ (void)collectEntries:(NSData * (^)(NSData *, uint8_t))decompress
           tileEntries:(NSMutableArray<NSValue *> *)tileEntries
            pmtilesMap:(const char *)pmtilesMap
                header:(headerv3)h
             dirOffset:(uint64_t)dirOffset
             dirLength:(uint64_t)dirLength {
  NSData *dirData = [NSData dataWithBytes:pmtilesMap + dirOffset
                                   length:dirLength];

  NSData *decompressedData = decompress(dirData, h.internal_compression);

  NSArray<NSValue *> *dirEntries =
      [PMTiles deserializeDirectory:decompressedData];
  for (NSValue *entryValue in dirEntries) {
    EntryV3 entry;
    [entryValue getValue:&entry];
    if (entry.run_length == 0) {
      [PMTiles collectEntries:decompress
                  tileEntries:tileEntries
                   pmtilesMap:pmtilesMap
                       header:h
                    dirOffset:h.leaf_dirs_offset + entry.offset
                    dirLength:entry.length];
    } else {
      for (uint64_t i = entry.tile_id; i < entry.tile_id + entry.run_length;
           i++) {
        ZXY zxy = [PMTiles tileIDToZXY:i];
        EntryZXY entryZXY =
            EntryZXYMake(zxy.z, zxy.x, zxy.y, h.tile_data_offset + entry.offset,
                         entry.length);
        [tileEntries addObject:[NSValue valueWithBytes:&entryZXY
                                              objCType:@encode(EntryZXY)]];
      }
    }
  }
}

+ (NSArray<NSValue *> *)entriesTMS:(NSData * (^)(NSData *, uint8_t))decompress
                        pmtilesMap:(const char *)pmtilesMap {
  NSData *headerData = [NSData dataWithBytes:pmtilesMap length:127];
  headerv3 header = [PMTiles deserializeHeader:headerData];
  NSMutableArray<NSValue *> *tileEntries = [NSMutableArray array];
  [PMTiles collectEntries:decompress
              tileEntries:tileEntries
               pmtilesMap:pmtilesMap
                   header:header
                dirOffset:header.root_dir_offset
                dirLength:header.root_dir_bytes];
  NSArray<NSValue *> *sortedTileEntries = [tileEntries
      sortedArrayUsingComparator:^NSComparisonResult(NSValue *a, NSValue *b) {
        EntryZXY entryA, entryB;
        [a getValue:&entryA];
        [b getValue:&entryB];
        if (entryA.z != entryB.z) {
          return entryA.z < entryB.z ? NSOrderedAscending : NSOrderedDescending;
        }
        if (entryA.x != entryB.x) {
          return entryA.x < entryB.x ? NSOrderedAscending : NSOrderedDescending;
        }
        return entryA.y > entryB.y ? NSOrderedAscending : NSOrderedDescending;
      }];

  return sortedTileEntries;
}

+ (headerv3)deserializeHeader:(NSData *)bytes {
  headerv3 header;
  const uint8_t *dataPtr = bytes.bytes;

  uint8_t spec_version = dataPtr[7];
  if (spec_version > 3) {
    [NSException raise:@"InvalidArchiveException"
                format:@"Archive is spec version %d but this library supports "
                       @"up to spec version 3",
                       spec_version];
  }

  header.root_dir_offset = [self getUint64:dataPtr atOffset:8];
  header.root_dir_bytes = [self getUint64:dataPtr atOffset:16];
  header.json_metadata_offset = [self getUint64:dataPtr atOffset:24];
  header.json_metadata_bytes = [self getUint64:dataPtr atOffset:32];
  header.leaf_dirs_offset = [self getUint64:dataPtr atOffset:40];
  header.leaf_dirs_bytes = [self getUint64:dataPtr atOffset:48];
  header.tile_data_offset = [self getUint64:dataPtr atOffset:56];
  header.tile_data_bytes = [self getUint64:dataPtr atOffset:64];
  header.addressed_tiles_count = [self getUint64:dataPtr atOffset:72];
  header.tile_entries_count = [self getUint64:dataPtr atOffset:80];
  header.tile_contents_count = [self getUint64:dataPtr atOffset:88];
  header.clustered = dataPtr[96] == 1;
  header.internal_compression = dataPtr[97];
  header.tile_compression = dataPtr[98];
  header.tile_type = dataPtr[99];
  header.min_zoom = dataPtr[100];
  header.max_zoom = dataPtr[101];
  header.min_lon_e7 = [self getInt32:dataPtr atOffset:102 isLittleEndian:YES];
  header.min_lat_e7 = [self getInt32:dataPtr atOffset:106 isLittleEndian:YES];
  header.max_lon_e7 = [self getInt32:dataPtr atOffset:110 isLittleEndian:YES];
  header.max_lat_e7 = [self getInt32:dataPtr atOffset:114 isLittleEndian:YES];
  header.center_zoom = dataPtr[118];
  header.center_lon_e7 = [self getInt32:dataPtr
                               atOffset:119
                         isLittleEndian:YES];
  header.center_lat_e7 = [self getInt32:dataPtr
                               atOffset:123
                         isLittleEndian:YES];

  return header;
}

+ (uint64_t)getUint64:(const uint8_t *)data atOffset:(int)offset {
  uint64_t value;
  memcpy(&value, &data[offset], sizeof(value));
  return CFSwapInt64LittleToHost(value);
}

+ (int32_t)getInt32:(const uint8_t *)data
           atOffset:(int)offset
     isLittleEndian:(BOOL)isLittleEndian {
  int32_t value;
  memcpy(&value, &data[offset], sizeof(value));
  if (isLittleEndian) {
    return CFSwapInt32LittleToHost(value);
  } else {
    return CFSwapInt32BigToHost(value);
  }
}

+ (NSString *)serializeHeader:(headerv3)header {
  NSMutableData *data = [NSMutableData data];

  [data appendData:[@"PMTiles" dataUsingEncoding:NSUTF8StringEncoding]];
  uint8_t version = 3;
  [data appendBytes:&version length:1];
  [data appendBytes:&header.root_dir_offset
             length:sizeof(header.root_dir_offset)];
  [data appendBytes:&header.root_dir_bytes
             length:sizeof(header.root_dir_bytes)];
  [data appendBytes:&header.json_metadata_offset
             length:sizeof(header.json_metadata_offset)];
  [data appendBytes:&header.json_metadata_bytes
             length:sizeof(header.json_metadata_bytes)];
  [data appendBytes:&header.leaf_dirs_offset
             length:sizeof(header.leaf_dirs_offset)];
  [data appendBytes:&header.leaf_dirs_bytes
             length:sizeof(header.leaf_dirs_bytes)];
  [data appendBytes:&header.tile_data_offset
             length:sizeof(header.tile_data_offset)];
  [data appendBytes:&header.tile_data_bytes
             length:sizeof(header.tile_data_bytes)];
  [data appendBytes:&header.addressed_tiles_count
             length:sizeof(header.addressed_tiles_count)];
  [data appendBytes:&header.tile_entries_count
             length:sizeof(header.tile_entries_count)];
  [data appendBytes:&header.tile_contents_count
             length:sizeof(header.tile_contents_count)];

  uint8_t clustered_val = header.clustered ? 0x1 : 0x0;
  [data appendBytes:&clustered_val length:1];
  [data appendBytes:&header.internal_compression length:1];
  [data appendBytes:&header.tile_compression length:1];
  [data appendBytes:&header.tile_type length:1];
  [data appendBytes:&header.min_zoom length:1];
  [data appendBytes:&header.max_zoom length:1];
  [data appendBytes:&header.min_lon_e7 length:sizeof(header.min_lon_e7)];
  [data appendBytes:&header.min_lat_e7 length:sizeof(header.min_lat_e7)];
  [data appendBytes:&header.max_lon_e7 length:sizeof(header.max_lon_e7)];
  [data appendBytes:&header.max_lat_e7 length:sizeof(header.max_lat_e7)];
  [data appendBytes:&header.center_zoom length:1];
  [data appendBytes:&header.center_lon_e7 length:sizeof(header.center_lon_e7)];
  [data appendBytes:&header.center_lat_e7 length:sizeof(header.center_lat_e7)];

  return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

- (NSData *)getTileWithZ:(uint8_t)z x:(uint32_t)x y:(uint32_t)y {
  uint64_t tileID = [PMTiles zxyToTileID:z x:x y:y];
  headerv3 header = [self.sharedCache getHeaderWithSource:self.fetchSource];
  uint64_t dirOffset = header.root_dir_offset;
  uint32_t dirLength = header.root_dir_bytes;
  for (int depth = 0; depth <= 3; depth++) {
    NSData *dirData = [self.sharedCache getDirectoryWithSource:self.fetchSource
                                                        offset:dirOffset
                                                        length:dirLength];

    NSData *decompressedData =
        [Decompress decompress:dirData compression:header.internal_compression];

    NSArray<NSValue *> *dirEntries =
        [PMTiles deserializeDirectory:decompressedData];
    EntryV3 entry = [VarintHelper findTile:dirEntries withTileID:tileID];

    if (entry.length > 0) {
      if (entry.run_length > 0) {
        return [self.sharedCache
            fetchTileWithSource:self.fetchSource
                         offset:header.tile_data_offset + entry.offset
                         length:entry.length];
      } else {
        dirOffset = header.leaf_dirs_offset + entry.offset;
        dirLength = entry.length;
      }
    } else {
      return nil;
    }
  }

  return nil;
}

@end

@implementation PMTilesMagicNumberException

- (NSString *)what {
  return @"pmtiles magic number exception";
}

@end
@implementation PMTilesVersionException

- (NSString *)what {
  return @"pmtiles version: must be 3";
}

@end

@implementation VarIntTooLongException

- (NSString *)what {
  return @"varint too long exception";
}

@end

@implementation EndOfBufferException

- (NSString *)what {
  return @"end of buffer exception";
}

@end
