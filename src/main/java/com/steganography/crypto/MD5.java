package com.steganography.crypto;

public final class MD5 {
    private static final int[] STATE = {0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476};
    private static final int[] SHIFT = {
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    };

    private static final int[] K = {
        0xD76AA478, 0xE8C7B756, 0x242070DB, 0xC1BDCEEE,
        0xF57C0FAF, 0x4787C62A, 0xA8304613, 0xFD469501,
        0x698098D8, 0x8B44F7AF, 0xFFFF5BB1, 0x895CD7BE,
        0x6B901122, 0xFD987193, 0xA679438E, 0x49B40821,
        0xF61E2562, 0xC040B340, 0x265E5A51, 0xE9B6C7AA,
        0xD62F105D, 0x02441453, 0xD8A1E681, 0xE7D3FBC8,
        0x21E1CDE6, 0xC33707D6, 0xF4D50D87, 0x455A14ED,
        0xA9E3E905, 0xFCEFA3F8, 0x676F02D9, 0x8D2A4C8A,
        0xFFFA3942, 0x8771F681, 0x6D9D6122, 0xFDE5380C,
        0xA4BEEA44, 0x4BDECFA9, 0xF6BB4B60, 0xBEBFBC70,
        0x289B7EC6, 0xEAA127FA, 0xD4EF3085, 0x04881D05,
        0xD9D4D039, 0xE6DB99E5, 0x1FA27CF8, 0xC4AC5665,
        0xF4292244, 0x432AFF97, 0xAB9423A7, 0xFC93A039,
        0x655B59C3, 0x8F0CCC92, 0xFFEFF47D, 0x85845DD1,
        0x6FA87E4F, 0xFE2CE6E0, 0xA3014314, 0x4E0811A1,
        0xF7537E82, 0xBD3AF235, 0x2AD7D2BB, 0xEB86D391
    };

    private MD5() {
    }

    public static byte[] digest(byte[] message) {
        int[] state = STATE.clone();
        byte[] padded = pad(message);
        int[] words = new int[16];

        int j;
        for (int offset = 0; offset < padded.length; offset += 64) {
            for (int i = 0; i < 16; i++) {
                j = offset + (i * 4);
                words[i] = (padded[j] & 0xFF) | ((padded[j + 1] & 0xFF) << 8) | ((padded[j + 2] & 0xFF) << 16) | ((padded[j + 3] & 0xFF) << 24);
            }

            int a, b, c, d;
            a = state[0]; b = state[1]; c = state[2]; d = state[3];

            int f, g, temp;
            for (int i = 0; i < 64; i++) {
                if (i < 16) {
                    f = (b & c) | (~b & d);
                    g = i;
                } else if (i < 32) {
                    f = (d & b) | (~d & c);
                    g = ((i * 5) + 1) & 0x0F;
                } else if (i < 48) {
                    f = b ^ c ^ d;
                    g = ((i * 3) + 5) & 0x0F;
                } else {
                    f = c ^ (b | ~d);
                    g = (i * 7) & 0x0F;
                }

                temp = d;
                d = c;
                c = b;
                b += Integer.rotateLeft(a + f + K[i] + words[g], SHIFT[i]);
                a = temp;
            }
            state[0] += a; state[1] += b; state[2] += c; state[3] += d;
        }

        byte[] out = new byte[16];
        int v, p;
        for (int i = 0; i < state.length; i++) {
            v = state[i];
            p = i * 4;
            out[p] = (byte) v;
            out[p + 1] = (byte) (v >>> 8);
            out[p + 2] = (byte) (v >>> 16);
            out[p + 3] = (byte) (v >>> 24);
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
            padded[totalLen - 8 + i] = (byte) (bitLen >>> (8 * i));
        }
        return padded;
    }
}