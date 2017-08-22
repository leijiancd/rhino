/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.security.SecureRandom;

public class Hashing {

  /*
   * Seed the hash using SecureRandom, using the default implementation for this JVM.
   * This is the best possible seed, as the seed will be used for the lifetime of the JVM.
   * If this proves to be too slow in some environments, then we will investigate
   * some alternatives.
   */
  private static final int HASH_SEED = new SecureRandom().nextInt();

  public static int secureHash(final Object o) {
    if (o instanceof CharSequence) {
      return secureStringHash((String)o);
    }
    // Use built-in hash code, likely a Symbol object
    return o.hashCode();
  }

  /*
   * The MurmurHash3 algorithm was created by Austin Appleby and placed in the public domain.
   * This java port was authored by Yonik Seeley and also placed into the public domain.
   * The author hereby disclaims copyright to this source code.
   */
  public  static int secureStringHash(final CharSequence data) {
    final int c1 = 0xcc9e2d51;
    final int c2 = 0x1b873593;

    int h1 = HASH_SEED;

    int pos = 0;
    int end = data.length();
    int k1 = 0;
    int k2 = 0;
    int shift = 0;
    int bits = 0;
    int nBytes = 0;

    while (pos < end) {
      int code = data.charAt(pos++);
      if (code < 0x80) {
        k2 = code;
        bits = 8;
      }
      else if (code < 0x800) {
        k2 = (0xC0 | (code >> 6))
            | ((0x80 | (code & 0x3F)) << 8);
        bits = 16;
      }
      else if (code < 0xD800 || code > 0xDFFF || pos>=end) {
        // we check for pos>=end to encode an unpaired surrogate as 3 bytes.
        k2 = (0xE0 | (code >> 12))
            | ((0x80 | ((code >> 6) & 0x3F)) << 8)
            | ((0x80 | (code & 0x3F)) << 16);
        bits = 24;
      } else {
        // surrogate pair
        // int utf32 = pos < end ? (int) data.charAt(pos++) : 0;
        int utf32 = (int) data.charAt(pos++);
        utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
        k2 = (0xff & (0xF0 | (utf32 >> 18)))
            | ((0x80 | ((utf32 >> 12) & 0x3F))) << 8
            | ((0x80 | ((utf32 >> 6) & 0x3F))) << 16
            |  (0x80 | (utf32 & 0x3F)) << 24;
        bits = 32;
      }


      k1 |= k2 << shift;

      // int used_bits = 32 - shift;  // how many bits of k2 were used in k1.
      // int unused_bits = bits - used_bits; //  (bits-(32-shift)) == bits+shift-32  == bits-newshift

      shift += bits;
      if (shift >= 32) {
        // mix after we have a complete word

        k1 *= c1;
        k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
        k1 *= c2;

        h1 ^= k1;
        h1 = (h1 << 13) | (h1 >>> 19);  // ROTL32(h1,13);
        h1 = h1*5+0xe6546b64;

        shift -= 32;
        // unfortunately, java won't let you shift 32 bits off, so we need to check for 0
        if (shift != 0) {
          k1 = k2 >>> (bits-shift);   // bits used == bits - newshift
        } else {
          k1 = 0;
        }
        nBytes += 4;
      }

    } // inner

    // handle tail
    if (shift > 0) {
      nBytes += shift >> 3;
      k1 *= c1;
      k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
      k1 *= c2;
      h1 ^= k1;
    }

    // finalization
    h1 ^= nBytes;

    // fmix(h1);
    h1 ^= h1 >>> 16;
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;

    return h1;
  }
}
