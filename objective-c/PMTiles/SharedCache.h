#import "FetchSource.h"
#import "Header.h"
#import <Foundation/Foundation.h>
@interface SharedCache : NSObject

@property(nonatomic) headerv3 header;
@property(nonatomic) BOOL isHeaderSet;
@property(nonatomic, strong) NSMutableDictionary<NSString *, NSData *> *cache;
- (headerv3)getHeaderWithSource:(FetchSource *)source;
- (headerv3)fetchHeaderWithSource:(FetchSource *)source;
- (NSData *)getDirectoryWithSource:(FetchSource *)source
                            offset:(NSUInteger)offset
                            length:(NSUInteger)length;
- (NSData *)fetchTileWithSource:(FetchSource *)source
                         offset:(NSUInteger)offset
                         length:(NSUInteger)length;
@end
