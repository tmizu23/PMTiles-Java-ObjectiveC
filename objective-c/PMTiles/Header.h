#import <Foundation/Foundation.h>

typedef struct {
  uint64_t root_dir_offset;
  uint64_t root_dir_bytes;
  uint64_t json_metadata_offset;
  uint64_t json_metadata_bytes;
  uint64_t leaf_dirs_offset;
  uint64_t leaf_dirs_bytes;
  uint64_t tile_data_offset;
  uint64_t tile_data_bytes;
  uint64_t addressed_tiles_count;
  uint64_t tile_entries_count;
  uint64_t tile_contents_count;
  BOOL clustered;
  uint8_t internal_compression;
  uint8_t tile_compression;
  uint8_t tile_type;
  uint8_t min_zoom;
  uint8_t max_zoom;
  int32_t min_lon_e7;
  int32_t min_lat_e7;
  int32_t max_lon_e7;
  int32_t max_lat_e7;
  uint8_t center_zoom;
  int32_t center_lon_e7;
  int32_t center_lat_e7;
} headerv3;
