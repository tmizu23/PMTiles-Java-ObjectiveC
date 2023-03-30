#import <Foundation/Foundation.h>
#import <zlib.h>

@interface Decompress : NSObject

+ (NSData *)decompress:(NSData *)data compression:(NSUInteger)compression;

@end