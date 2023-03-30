#import "Decompress.h"

@implementation Decompress

+ (NSData *)decompress:(NSData *)data compression:(NSUInteger)compression {
  if (compression == 1 || compression == 0) {
    return data;
  } else if (compression == 2) {
    return [self decompressGzip:data];
  } else {
    @throw [NSException exceptionWithName:NSInvalidArgumentException
                                   reason:@"Compression method not supported"
                                 userInfo:nil];
  }
}

+ (NSData *)decompressGzip:(NSData *)data {
  if ([data length] == 0)
    return data;

  NSUInteger full_length = [data length];
  NSUInteger half_length = [data length] / 2;

  NSMutableData *decompressed =
      [NSMutableData dataWithLength:full_length + half_length];
  BOOL done = NO;
  int status;

  z_stream strm;
  strm.next_in = (Bytef *)[data bytes];
  strm.avail_in = (uInt)[data length];
  strm.total_out = 0;
  strm.zalloc = Z_NULL;
  strm.zfree = Z_NULL;

  if (inflateInit2(&strm, (15 + 32)) != Z_OK)
    return nil;

  while (!done) {
    if (strm.total_out >= [decompressed length]) {
      [decompressed increaseLengthBy:half_length];
    }

    strm.next_out = [decompressed mutableBytes] + strm.total_out;
    strm.avail_out = (uInt)([decompressed length] - strm.total_out);

    status = inflate(&strm, Z_SYNC_FLUSH);

    if (status == Z_STREAM_END) {
      done = YES;
    } else if (status != Z_OK) {
      break;
    }
  }

  if (inflateEnd(&strm) != Z_OK)
    return nil;

  if (done) {
    [decompressed setLength:strm.total_out];
    return [NSData dataWithData:decompressed];
  } else {
    return nil;
  }
}

@end