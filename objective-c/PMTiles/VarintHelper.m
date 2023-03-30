#import "VarintHelper.h"

static const int8_t maxVarintLength = sizeof(uint64_t) * 8 / 7 + 1;

@implementation VarintHelper

+ (uint64_t)decodeVarintImpl:(const char **)data end:(const char *)end {

  const int8_t *begin = (const int8_t *)(*data);
  const int8_t *iend = (const int8_t *)(end);
  const int8_t *p = begin;
  uint64_t val = 0;

  if (iend - begin >= maxVarintLength) { // fast path
    do {
      int64_t b = *p++;
      val = ((uint64_t)(b)&0x7fU);
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 7U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 14U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 21U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 28U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 35U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 42U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 49U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x7fU) << 56U;
      if (b >= 0) {
        break;
      }
      b = *p++;
      val |= ((uint64_t)(b)&0x01U) << 63U;
      if (b >= 0) {
        break;
      }
      @throw [VarIntTooLongException new];
    } while (false);
  } else {
    unsigned int shift = 0;
    while (p != iend && *p < 0) {
      val |= ((uint64_t)(*p++) & 0x7fU) << shift;
      shift += 7;
    }
    if (p == iend) {
      @throw [EndOfBufferException new];
    }
    val |= (uint64_t)(*p++) << shift;
  }

  *data = (const char *)(p);
  return val;
}

+ (uint64_t)decodeVarint:(const char **)data end:(const char *)end {
  // If this is a one-byte varint, decode it here.
  if (end != *data && (((uint64_t)(**data) & 0x80U) == 0)) {
    const uint64_t val = (uint64_t)(**data);
    ++(*data);
    return val;
  }
  // If this varint is more than one byte, defer to complete implementation.
  return [VarintHelper decodeVarintImpl:data end:end];
}

+ (void)rotate:(int64_t)n
             x:(int64_t *)x
             y:(int64_t *)y
            rx:(int64_t)rx
            ry:(int64_t)ry {
  if (ry == 0) {
    if (rx == 1) {
      *x = n - 1 - *x;
      *y = n - 1 - *y;
    }
    int64_t t = *x;
    *x = *y;
    *y = t;
  }
}

+ (ZXY)t_on_level:(uint8_t)z position:(uint64_t)pos {
  int64_t n = 1LL << z;
  int64_t rx, ry, s, t = pos;
  int64_t tx = 0;
  int64_t ty = 0;
  ZXY result;

  for (s = 1; s < n; s *= 2) {
    rx = 1LL & (t / 2);
    ry = 1LL & (t ^ rx);
    [VarintHelper rotate:s x:&tx y:&ty rx:rx ry:ry];
    tx += s * rx;
    ty += s * ry;
    t /= 4;
  }
  result.z = z;
  result.x = (uint32_t)tx;
  result.y = (uint32_t)ty;

  return result;
}

+ (NSInteger)writeVarint:(NSMutableString *)data value:(uint64_t)value {
  NSInteger n = 1;

  while (value >= 0x80U) {
    char byte = (char)((value & 0x7fU) | 0x80U);
    NSString *appendStr = [NSString stringWithFormat:@"%c", byte];
    [data appendString:appendStr];
    value >>= 7U;
    ++n;
  }
  char byte = (char)value;
  NSString *appendStr = [NSString stringWithFormat:@"%c", byte];
  [data appendString:appendStr];

  return n;
}

+ (NSComparisonResult)compareEntryZXY:(EntryZXY)a withEntryZXY:(EntryZXY)b {
  if (a.z != b.z) {
    return (a.z < b.z) ? NSOrderedAscending : NSOrderedDescending;
  }
  if (a.x != b.x) {
    return (a.x < b.x) ? NSOrderedAscending : NSOrderedDescending;
  }
  return (a.y > b.y) ? NSOrderedAscending : NSOrderedDescending;
}

+ (EntryV3)findTile:(NSArray<NSValue *> *)entries withTileID:(uint64_t)tileID {
  NSInteger m = 0;
  NSInteger n = [entries count] - 1;
  while (m <= n) {
    NSInteger k = (n + m) >> 1;
    EntryV3 entryK;
    [entries[k] getValue:&entryK];
    int64_t cmp = tileID - entryK.tile_id;
    if (cmp > 0) {
      m = k + 1;
    } else if (cmp < 0) {
      n = k - 1;
    } else {
      return entryK;
    }
  }

  if (n >= 0) {
    EntryV3 entryN;
    [entries[n] getValue:&entryN];
    if (entryN.run_length == 0) {
      return entryN;
    }
    if (tileID - entryN.tile_id < entryN.run_length) {
      return entryN;
    }
  }

  EntryV3 nullEntry = {0, 0, 0, 0};
  return nullEntry;
}

@end