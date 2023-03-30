// pmtiles.h

#import "FetchSource.h"
#import "Header.h"
#import "SharedCache.h"
#import <Foundation/Foundation.h>
@interface PMTiles : NSObject

@property(nonatomic, strong) NSString *urlString;
@property(nonatomic, strong) FetchSource *fetchSource;
@property(nonatomic, strong) SharedCache *sharedCache;

typedef struct {
  uint8_t z;
  uint32_t x;
  uint32_t y;
} ZXY;

typedef struct {
  uint64_t tile_id;
  uint64_t offset;
  uint32_t length;
  uint32_t run_length;
} EntryV3;

typedef struct {
  uint8_t z;
  uint32_t x;
  uint32_t y;
  uint64_t offset;
  uint32_t length;
} EntryZXY;

ZXY ZXYMake(uint8_t z, uint32_t x, uint32_t y);
EntryV3 EntryV3Make(uint64_t tile_id, uint64_t offset, uint32_t length,
                    uint32_t run_length);

EntryZXY EntryZXYMake(uint8_t z, uint32_t x, uint32_t y, uint64_t offset,
                      uint32_t length);

NS_INLINE NSComparisonResult EntryV3Compare(EntryV3 a, EntryV3 b);

NS_INLINE NSComparisonResult EntryZXYCompare(EntryZXY a, EntryZXY b);

+ (NSString *)serializeHeader:(headerv3)header;
+ (headerv3)deserializeHeader:(NSData *)bytes;
+ (ZXY)tileIDToZXY:(uint64_t)tileID;
+ (uint64_t)zxyToTileID:(uint8_t)z x:(uint32_t)x y:(uint32_t)y;
+ (NSString *)serializeDirectory:(NSArray<NSValue *> *)entries;
+ (NSArray<NSValue *> *)deserializeDirectory:(NSData *)decompressed;
+ (void)collectEntries:(NSData * (^)(NSData *, uint8_t))decompress
           tileEntries:(NSMutableArray<NSValue *> *)tileEntries
            pmtilesMap:(const char *)pmtilesMap
                header:(headerv3)h
             dirOffset:(uint64_t)dirOffset
             dirLength:(uint64_t)dirLength;
+ (NSArray<NSValue *> *)entriesTMS:(NSData * (^)(NSData *, uint8_t))decompress
                        pmtilesMap:(const char *)pmtilesMap;

+ (uint64_t)getUint64:(const uint8_t *)data atOffset:(int)offset;
+ (int32_t)getInt32:(const uint8_t *)data
           atOffset:(int)offset
     isLittleEndian:(BOOL)isLittleEndian;

- (instancetype)initWithURL:(NSString *)urlString;
- (NSData *)getTileWithZ:(uint8_t)z x:(uint32_t)x y:(uint32_t)y;

@end

@interface PMTilesMagicNumberException : NSException
- (NSString *)what;
@end

@interface PMTilesVersionException : NSException
- (NSString *)what;
@end

@interface VarIntTooLongException : NSException
@end

@interface EndOfBufferException : NSException
@end
