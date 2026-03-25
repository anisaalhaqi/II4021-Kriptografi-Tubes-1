package com.steganography.crypto;

import java.util.Arrays;

public final class SHA256 {
    private static final int[] STATE = {0x6A09E667, 0xBB67AE85,0x3C6EF372, 0xA54FF53A, 0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19};
    private static final int[] K = {
        0x428A2F98, 0x71374491, 0xB5C0FBCF,
        0xE9B5DBA5, 0x3956C25B, 0x59F111F1, 0x923F82A4, 0xAB1C5ED5,
        0xD807AA98, 0x12835B01, 0x243185BE, 0x550C7DC3, 0x72BE5D74,
        0x80DEB1FE, 0x9BDC06A7, 0xC19BF174, 0xE49B69C1, 0xEFBE4786,
        0x0FC19DC6, 0x240CA1CC, 0x2DE92C6F, 0x4A7484AA, 0x5CB0A9DC,
        0x76F988DA, 0x983E5152, 0xA831C66D, 0xB00327C8, 0xBF597FC7,
        0xC6E00BF3, 0xD5A79147, 0x06CA6351, 0x14292967, 0x27B70A85,
        0x2E1B2138, 0x4D2C6DFC, 0x53380D13, 0x650A7354, 0x766A0ABB,
        0x81C2C92E, 0x92722C85, 0xA2BFE8A1, 0xA81A664B, 0xC24B8B70,
        0xC76C51A3, 0xD192E819, 0xD6990624, 0xF40E3585, 0x106AA070,
        0x19A4C116, 0x1E376C08, 0x2748774C, 0x34B0BCB5, 0x391C0CB3,
        0x4ED8AA4A, 0x5B9CCA4F, 0x682E6FF3, 0x748F82EE, 0x78A5636F,
        0x84C87814, 0x8CC70208, 0x90BEFFFA, 0xA4506CEB, 0xBEF9A3F7,
        0xC67178F2
        };

    private SHA256() {
    }

    public static byte[] digest(byte[] message) {
        int[] h = Arrays.copyOf(STATE, STATE.length);
        byte[] padded = pad(message);
        int[] w = new int[64];

        int j;
        for (int offset = 0; offset < padded.length; offset += 64) {
            for (int i = 0; i < 16; i++) {
                j = offset + (i * 4);
                w[i] = ((padded[j] & 0xFF) << 24) | ((padded[j + 1] & 0xFF) << 16) | ((padded[j + 2] & 0xFF) << 8) | (padded[j + 3] & 0xFF);
            }

            int s0, s1;
            for (int i = 16; i < 64; i++) {
                s0 = smallSigma0(w[i - 15]);
                s1 = smallSigma1(w[i - 2]);
                w[i] = w[i - 16] + s0 + w[i - 7] + s1;
            }
            int a, b, c, d, e, f, g, hh;
            a = h[0]; b = h[1]; c = h[2]; d = h[3]; e = h[4]; f = h[5]; g = h[6]; hh = h[7];

            int t1, t2;
            for (int i = 0; i < 64; i++) {
                t1 = hh + bigSigma1(e) + ch(e, f, g) + K[i] + w[i];
                t2 = bigSigma0(a) + maj(a, b, c);

                hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a;
                a = t1 + t2;
            }

            h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
        }

        byte[] out = new byte[32];
        int v, p;
        for (int i = 0; i < h.length; i++) {
            v = h[i];
            p = i * 4;
            out[p] = (byte) (v >>> 24);
            out[p + 1] = (byte) (v >>> 16);
            out[p + 2] = (byte) (v >>> 8);
            out[p + 3] = (byte) v;
        }
        return out;
    }

    private static byte[] pad(byte[] message) {
        long bitLen = (long) message.length * 8L;
        int pad0 = (56 - ((message.length + 1) % 64) + 64) % 64;
        int totalLen = message.length + 1 + pad0 + 8;

        byte[] padded = new byte[totalLen];
        System.arraycopy(message, 0, padded, 0, message.length);
        padded[message.length] = (byte) 0x80;

        for (int i = 0; i < 8; i++) {
            padded[totalLen - 1 - i] = (byte) (bitLen >>> (8 * i));
        }
        return padded;
    }

    private static int ch(int x, int y, int z) {return (x & y) ^ (~x & z);}
    private static int maj(int x, int y, int z) {return (x & y) ^ (x & z) ^ (y & z);}

    private static int bigSigma0(int x) {return Integer.rotateRight(x, 2) ^ Integer.rotateRight(x, 13) ^ Integer.rotateRight(x, 22);}
    private static int bigSigma1(int x) {return Integer.rotateRight(x, 6) ^ Integer.rotateRight(x, 11) ^ Integer.rotateRight(x, 25);}
    private static int smallSigma0(int x) {return Integer.rotateRight(x, 7) ^ Integer.rotateRight(x, 18) ^ (x >>> 3);}
    private static int smallSigma1(int x) {return Integer.rotateRight(x, 17) ^ Integer.rotateRight(x, 19) ^ (x >>> 10);}
}