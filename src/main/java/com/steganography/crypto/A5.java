package com.steganography.crypto;

public class A5 {

    private int R1, R2, R3;

    private static final int R1_MASK = (1 << 19) - 1;
    private static final int R2_MASK = (1 << 22) - 1;
    private static final int R3_MASK = (1 << 23) - 1;

    private static final int R1_CLK = 8;
    private static final int R2_CLK = 10;
    private static final int R3_CLK = 10;

    private int getBit(int reg, int pos) {
        return (reg >> pos) & 1;
    }

    private int majority(int x, int y, int z) {
        return (x + y + z) >= 2 ? 1 : 0;
    }

    private void clockR1() {
        int newBit = getBit(R1, 13) ^ getBit(R1, 16) ^ getBit(R1, 17) ^ getBit(R1, 18);
        R1 = ((R1 << 1) & R1_MASK) | newBit;
    }

    private void clockR2() {
        int newBit = getBit(R2, 20) ^ getBit(R2, 21);
        R2 = ((R2 << 1) & R2_MASK) | newBit;
    }

    private void clockR3() {
        int newBit = getBit(R3, 7) ^ getBit(R3, 20) ^ getBit(R3, 21) ^ getBit(R3, 22);
        R3 = ((R3 << 1) & R3_MASK) | newBit;
    }

    private void clockAll() {
        clockR1();
        clockR2();
        clockR3();
    }

    private void clockMajority() {
        int m = majority(
                getBit(R1, R1_CLK),
                getBit(R2, R2_CLK),
                getBit(R3, R3_CLK)
        );

        if(getBit(R1, R1_CLK) == m) {
            clockR1();
        }
        if(getBit(R2, R2_CLK) == m) {
            clockR2();
        }
        if(getBit(R3, R3_CLK) == m) {
            clockR3();
        }
    }

    private void initialize(long key, int frame) {
        R1 = R2 = R3 = 0;

        for (int i = 0; i < 64; i++) {
            int bit = (int)((key >> i) & 1);
            R1 ^= bit;
            R2 ^= bit;
            R3 ^= bit;
            clockAll();
        }

        for (int i = 0; i < 22; i++) {
            int bit = (frame >> i) & 1;
            R1 ^= bit;
            R2 ^= bit;
            R3 ^= bit;
            clockAll();
        }

        for (int i = 0; i < 100; i++) {
            clockMajority();
        }
    }

    private int[] generateKeystream(long key, int frame) {
        initialize(key, frame);

        int[] keystream = new int[228];

        for (int i = 0; i < 228; i++) {
            clockMajority();
            keystream[i] = getBit(R1, 18) ^ getBit(R2, 21) ^ getBit(R3, 22);
        }

        return keystream;
    }

    private byte[] process(byte[] data, long key, int frame) {
        byte[] result = new byte[data.length];

        int totalBits = data.length * 8;
        int bitIndex = 0;

        while (bitIndex < totalBits) {
            int[] ks = generateKeystream(key, frame);

            for (int i = 0; i < 228 && bitIndex < totalBits; i++, bitIndex++) {
                int bytePos = bitIndex >> 3;
                int bitPos = 7 - (bitIndex & 7);

                int dataBit = (data[bytePos] >> bitPos) & 1;
                int outBit = dataBit ^ ks[i];

                result[bytePos] &= ~(1 << bitPos);
                result[bytePos] |= (outBit << bitPos);
            }

            frame++;
        }

        return result;
    }

    public byte[] encrypt(byte[] data, long key, int startFrame) {
        return process(data, key, startFrame);
    }

    public byte[] decrypt(byte[] data, long key, int startFrame) {
        return process(data, key, startFrame);
    }
}