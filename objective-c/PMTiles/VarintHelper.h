#import "PMTiles.h"
#import <Foundation/Foundation.h>
@interface VarintHelper : NSObject

+ (uint64_t)decodeVarintImpl:(const char **)data end:(const char *)end;
+ (uint64_t)decodeVarint:(const char **)data end:(const char *)end;

+ (void)rotate:(int64_t)n
             x:(int64_t *)x
             y:(int64_t *)y
            rx:(int64_t)rx
            ry:(int64_t)ry;
+ (ZXY)t_on_level:(uint8_t)z position:(uint64_t)pos;
+ (NSInteger)writeVarint:(NSMutableString *)data value:(uint64_t)value;

+ (NSComparisonResult)compareEntryZXY:(EntryZXY)a withEntryZXY:(EntryZXY)b;
+ (EntryV3)findTile:(NSArray<NSValue *> *)entries withTileID:(uint64_t)tileID;

@end
