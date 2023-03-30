#import "FetchSource.h"

@implementation FetchSource

- (instancetype)initWithURL:(NSString *)url {
    self = [super init];
    if (self) {
        self.url = url;
    }
    return self;
}

- (NSString *)getKey {
    return self.url;
}

- (void)getBytesWithOffset:(NSUInteger)offset
                    length:(NSUInteger)length
                    signal:(NSProgress *)signal
                completion:(void (^)(NSDictionary *rangeResponse, NSError *error))completion {
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self.url]];
    [request setValue:[NSString stringWithFormat:@"bytes=%lu-%lu", (unsigned long)offset, (unsigned long)(offset + length - 1)] forHTTPHeaderField:@"Range"];
    
    if (signal) {
        [request setAllowsCellularAccess:NO];
        [signal addObserver:self
                 forKeyPath:@"cancelled"
                    options:NSKeyValueObservingOptionNew
                    context:nil];
    }
    
    NSURLSessionDataTask *dataTask = [[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        if (error) {
            completion(nil, error);
            return;
        }
        
        NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
        if (httpResponse.statusCode >= 300) {
            completion(nil, [NSError errorWithDomain:@"FetchSourceErrorDomain" code:httpResponse.statusCode userInfo:@{NSLocalizedDescriptionKey: [NSString stringWithFormat:@"Bad response code: %ld", (long)httpResponse.statusCode]}]);
            return;
        }
        
        NSString *contentLength = httpResponse.allHeaderFields[@"Content-Length"];
        if (httpResponse.statusCode == 200 && (!contentLength || contentLength.integerValue > length)) {
            if (signal) {
                [signal removeObserver:self forKeyPath:@"cancelled"];
            }
            completion(nil, [NSError errorWithDomain:@"FetchSourceErrorDomain" code:0 userInfo:@{NSLocalizedDescriptionKey: @"Server returned no content-length header or content-length exceeding request. Check that your storage backend supports HTTP Byte Serving."}]);
            return;
        }
        
        NSDictionary *rangeResponse = @{
            @"data": data,
            @"etag": httpResponse.allHeaderFields[@"ETag"] ?: [NSNull null],
            @"cacheControl": httpResponse.allHeaderFields[@"Cache-Control"] ?: [NSNull null],
            @"expires": httpResponse.allHeaderFields[@"Expires"] ?: [NSNull null],
        };
        completion(rangeResponse, nil);
    }];
    
    [dataTask resume];
}

@end
