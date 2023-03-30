#import "PMTiles/PMTiles.h"
#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>

int main(int argc, const char *argv[]) {
  @autoreleasepool {

    NSString *urlString =
        @"https://tmizu23.github.io/PMTiles-Java-ObjectiveC/sample.pmtiles";
    PMTiles *pmtiles = [[PMTiles alloc] initWithURL:urlString];
    NSLog(@"getTileWithZ:19 x:467278 y:201856");
    NSData *tileData = [pmtiles getTileWithZ:19 x:467278 y:201856];

    // Convert the tile data to a PNG image
    NSImage *image = [[NSImage alloc] initWithData:tileData];
    NSData *imageData = [image TIFFRepresentation];
    NSBitmapImageRep *imageRep = [NSBitmapImageRep imageRepWithData:imageData];
    NSData *pngData = [imageRep representationUsingType:NSBitmapImageFileTypePNG
                                             properties:@{}];
    // Save the PNG data to a file
    NSString *outputPath = @"output_image.png";
    [pngData writeToFile:outputPath atomically:YES];

    NSLog(@"Image saved to: %@", outputPath);
  }
  return 0;
}
