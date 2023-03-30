#import <Foundation/Foundation.h>

@interface FetchSource : NSObject

@property(nonatomic, strong) NSString *url;

- (instancetype)initWithURL:(NSString *)url;
- (NSString *)getKey;
- (void)getBytesWithOffset:(NSUInteger)offset
                    length:(NSUInteger)length
                    signal:(NSProgress *)signal
                completion:(void (^)(NSDictionary *rangeResponse,
                                     NSError *error))completion;

@end
