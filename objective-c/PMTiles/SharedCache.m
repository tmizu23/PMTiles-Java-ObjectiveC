#import "SharedCache.h"
#import "PMTiles.h"
@implementation SharedCache

- (instancetype)init {
  self = [super init];
  self.cache = [NSMutableDictionary dictionary];
  return self;
}

- (headerv3)getHeaderWithSource:(FetchSource *)source {
  if (!self.isHeaderSet) {
    self.header = [self fetchHeaderWithSource:source];
    self.isHeaderSet = YES;
  }
  return self.header;
}

- (headerv3)fetchHeaderWithSource:(FetchSource *)source {

  dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
  __block headerv3 header;
  [source getBytesWithOffset:0
                      length:127
                      signal:[NSProgress new]
                  completion:^(NSDictionary *rangeResponse, NSError *error) {
                    if (error) {
                      NSLog(@"Error: %@", error.localizedDescription);
                    } else {
                      header =
                          [PMTiles deserializeHeader:rangeResponse[@"data"]];
                    }
                    dispatch_semaphore_signal(semaphore);
                  }];

  // 結果が返ってくるまで待機
  dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
  return header;
}

- (NSData *)getDirectoryWithSource:(FetchSource *)source
                            offset:(NSUInteger)offset
                            length:(NSUInteger)length {
  NSString *key =
      [NSString stringWithFormat:@"%@-%lu-%lu", source.url, offset, length];
  if (self.cache[key]) {
    return self.cache[key];
  } else {
    self.cache[key] = [self fetchTileWithSource:source
                                         offset:offset
                                         length:length];
    return self.cache[key];
  }
}
- (NSData *)fetchTileWithSource:(FetchSource *)source
                         offset:(NSUInteger)offset
                         length:(NSUInteger)length {

  dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
  __block NSDictionary *rangeResponseResult;
  [source getBytesWithOffset:offset
                      length:length
                      signal:[NSProgress new]
                  completion:^(NSDictionary *rangeResponse, NSError *error) {
                    if (!error)
                      rangeResponseResult = [rangeResponse copy];
                    dispatch_semaphore_signal(semaphore);
                  }];

  // 結果が返ってくるまで待機
  dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
  NSData *tileData = rangeResponseResult[@"data"];
  // 取得したデータを処理する
  //   NSLog(@"ETag: %@", rangeResponseResult[@"etag"]);
  //   NSLog(@"Cache-Control: %@", rangeResponseResult[@"cacheControl"]);
  //   NSLog(@"Expires: %@", rangeResponseResult[@"expires"]);
  //   NSLog(@"Data: %@", tileData);
  return tileData;
}
@end
