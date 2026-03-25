package com.steganography.video;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import com.steganography.crypto.A5;
import com.steganography.crypto.SHA256;
import com.steganography.utils.common.Scheme;
import com.steganography.utils.decoder.HeaderInfo;
import com.steganography.utils.decoder.SequentialCollector;

public class Extractor {
    // welp same as embedder for the explanation
    private static final int FLAG_FILE = 0x01;
    private static final int FLAG_ENCRYPTED = 0x02;
    private static final int FLAG_RANDOM = 0x04;
    public static final int HEADER_SIZE = 8;
    private static final int MAX_FILENAME = 4096;

    public ExtractResult extract(File input, String stegoKey, String encKey) throws Exception {
        Reader reader = new Reader(input);
        if (stegoKey == null || stegoKey.isBlank()) {
            return extractSequential(reader, encKey);
        }

        reader.readMetadata();
        long capacityBytes = (long) reader.getWidth() * reader.getHeight() * reader.getTotalFrames();

        if (capacityBytes < HEADER_SIZE) {
            throw new IllegalArgumentException("Video is too small to contain stego header.");
        }

        HeaderInfo randomHeader = detectRandomHeader(reader, capacityBytes, stegoKey);
        if (randomHeader == null || !randomHeader.wasRandom) {
            return extractSequential(reader, encKey);
        }

        return extractRandom(reader, encKey, stegoKey, randomHeader);
    }

    public String extractText(File input, Long stegoKey, Long a5Key) throws Exception {
        ExtractResult result = extract(input, toNullableString(stegoKey), toNullableString(a5Key));
        return result.getText();
    }

    public void extractToFile(File input, File output, Long stegoKey, Long a5Key) throws Exception {
        ExtractResult result = extract(input, toNullableString(stegoKey), toNullableString(a5Key));
        Files.write(output.toPath(), result.data);
    }

    public static HeaderInfo parseHeader(byte[] headerBytes) {
        ByteBuffer buf = ByteBuffer.wrap(headerBytes);
        int flags = Byte.toUnsignedInt(buf.get());
        Scheme scheme = Scheme.fromId(Byte.toUnsignedInt(buf.get()));
        int dataSize = buf.getInt();
        int fileNameLen = Short.toUnsignedInt(buf.getShort());
        return new HeaderInfo((flags & FLAG_FILE) != 0, (flags & FLAG_ENCRYPTED) != 0, (flags & FLAG_RANDOM) != 0, scheme, dataSize, fileNameLen, flags);
    }

    public static boolean isHeaderSane(HeaderInfo header, long capacityBytes) {
        if ((header.flags & ~(FLAG_FILE | FLAG_ENCRYPTED | FLAG_RANDOM)) != 0) {
            return false;
        }
        if (header.scheme == null) {
            return false;
        }
        if (header.dataSize < 0 || header.fileNameLen < 0) {
            return false;
        }
        if (header.fileNameLen > MAX_FILENAME) {
            return false;
        }
        long total = HEADER_SIZE + header.fileNameLen + header.dataSize;
        return total <= capacityBytes;
    }

    // Welp, the step are similar to the embed process
    private static ExtractResult extractSequential(Reader reader, String encKey) throws Exception {
        reader.readMetadata();
        long capacityBytes = (long) reader.getWidth() * reader.getHeight() * reader.getTotalFrames();

        if (capacityBytes < HEADER_SIZE) {
            throw new IllegalArgumentException("Video is too small to contain stego header.");
        }

        HeaderInfo detectedHeader = detectSequentialHeader(reader, capacityBytes);
        if (detectedHeader == null) {
            throw new IllegalArgumentException("Could not parse valid stego header.");
        }

        SequentialCollector collector = new SequentialCollector(capacityBytes);

        reader.processFrames((frame, frameIndex) -> {
            int width = frame.getWidth();
            int height = frame.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    collector.accept((byte) extractByteFromPixel(frame.getRGB(x, y), detectedHeader.scheme));
                    if (collector.isComplete()) {
                        return false;
                    }
                }
            }
            return true;
        });

        if (!collector.isComplete()) {
            throw new IllegalArgumentException("Could not parse valid stego payload.");
        }
        return buildResult(collector.getHeader(), collector.getPayloadBytes(), false, encKey);
    }

    // Ok some tricky part, cuz the payload randomized in pixel, we need to read some header so we can brick by brick getting the real info
    private static ExtractResult extractRandom(Reader reader, String encKey, String stegoKey, HeaderInfo randomHeader) throws Exception {
        int payloadLength = HEADER_SIZE + randomHeader.fileNameLen + randomHeader.dataSize;
        byte[] payloadBytes = readRandomBytes(reader, stegoKey, payloadLength, randomHeader.scheme);
        return buildResult(randomHeader, payloadBytes, true, encKey);
    }

    private static HeaderInfo detectSequentialHeader(Reader reader, long capacityBytes) throws Exception {
        int[] rgbs = new int[HEADER_SIZE];
        int[] writeIndex = {0};

        reader.processFrames((frame, frameIndex) -> {
            int width = frame.getWidth();
            int height = frame.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rgbs[writeIndex[0]++] = frame.getRGB(x, y);
                    if (writeIndex[0] >= rgbs.length) {
                        return false;
                    }
                }
            }
            return true;
        });

        if (writeIndex[0] < HEADER_SIZE) {
            return null;
        }
        return detectHeaderFromPixels(rgbs, capacityBytes, false);
    }

    // yes, this is the detector for the random part
    private static HeaderInfo detectRandomHeader(Reader reader, long capacityBytes, String stegoKey) throws Exception {
        int[] rgbs = readRandomPixels(reader, stegoKey, HEADER_SIZE);
        return detectHeaderFromPixels(rgbs, capacityBytes, true);
    }

    private static HeaderInfo detectHeaderFromPixels(int[] rgbs, long capacityBytes, boolean requireRandom) {
        byte[] headerBytes;
        
        for (Scheme scheme : Scheme.values()) {
            headerBytes = new byte[HEADER_SIZE];
            for (int i = 0; i < HEADER_SIZE; i++) {
                headerBytes[i] = (byte) extractByteFromPixel(rgbs[i], scheme);
            }

            HeaderInfo header = parseHeader(headerBytes);
            if (header.scheme == scheme && isHeaderSane(header, capacityBytes) && header.wasRandom == requireRandom) {
                return header;
            }
        }
        return null;
    }

    private static int[] readRandomPixels(Reader reader, String stegoKey, int length) throws Exception {
        int[] result = new int[length];
        long baseSeed = computeSeed(stegoKey);
        int[] collected = {0};

        reader.processFrames((frame, frameIndex) -> {
            if (collected[0] >= length) {
                return false;
            }

            int width = frame.getWidth();
            int height = frame.getHeight();
            int framePixels = width * height;
            int count = Math.min(length - collected[0], framePixels);
            long[] positions = sampleRandomPositions(framePixels, count, computeFrameSeed(baseSeed, frameIndex));
            int pixelIndex,x,y;

            for (int i = 0; i < count; i++) {
                pixelIndex = (int) positions[i];
                x = pixelIndex % width;
                y = pixelIndex / width;
                result[collected[0] + i] = frame.getRGB(x, y);
            }

            collected[0] += count;
            return collected[0] < length;
        });

        if (collected[0] < length) {
            throw new IllegalArgumentException("Random extraction exceeds available pixels.");
        }
        return result;
    }

    private static byte[] readRandomBytes(Reader reader, String stegoKey, int length, Scheme scheme) throws Exception {
        byte[] result = new byte[length];
        long baseSeed = computeSeed(stegoKey);
        int[] collected = {0};

        reader.processFrames((frame, frameIndex) -> {
            if (collected[0] >= length) {
                return false;
            }

            int width = frame.getWidth();
            int height = frame.getHeight();
            int framePixels = width * height;
            int count = Math.min(length - collected[0], framePixels);
            long[] positions = sampleRandomPositions(framePixels, count, computeFrameSeed(baseSeed, frameIndex));
            int x, y, pixelIndex;

            for (int i = 0; i < count; i++) {
                pixelIndex = (int) positions[i];
                x = pixelIndex % width;
                y = pixelIndex / width;
                result[collected[0] + i] = (byte) extractByteFromPixel(frame.getRGB(x, y), scheme);
            }

            collected[0] += count;
            return collected[0] < length;
        });

        if (collected[0] < length) {
            throw new IllegalArgumentException("Random extraction exceeds available pixels.");
        }
        return result;
    }

    public static long computeSeed(String stegoKey) {
        long seed = stegoKey.hashCode();

        for (int i = 0; i < stegoKey.length(); i++) {
            seed = seed * 31L + stegoKey.charAt(i);
        }
        return seed;
    }

    public static long computeFrameSeed(long baseSeed, int frameIndex) {
        long mixed = baseSeed ^ (0x9E3779B97F4A7C15L * (frameIndex + 1L));
        mixed ^= (mixed >>> 30);
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= (mixed >>> 27);
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    public static long[] sampleRandomPositions(int totalPixels, int count, long seed) {
        if (count < 0 || count > totalPixels) {
            throw new IllegalArgumentException("Random extraction exceeds available pixels.");
        }

        Random random = new Random(seed);
        long[] positions = new long[count];
        int candidate;

        if (count == 0) {
            return positions;
        }
        if (count == totalPixels) {
            for (int i = 0; i < count; i++) {
                positions[i] = i;
            }
            shufflePositions(positions, random);
            return positions;
        }

        BitSet used = new BitSet(totalPixels);
        int selected = 0;

        while (selected < count) {
            candidate = random.nextInt(totalPixels);
            if (!used.get(candidate)) {
                used.set(candidate);
                positions[selected++] = candidate;
            }
        }
        return positions;
    }

    private static void shufflePositions(long[] positions, Random random) {
        int j;
        long tmp;

        for (int i = positions.length - 1; i > 0; i--) {
            j = random.nextInt(i + 1);
            tmp = positions[i];
            positions[i] = positions[j];
            positions[j] = tmp;
        }
    }

    private static ExtractResult buildResult(HeaderInfo header, byte[] payloadBytes, boolean wasRandom, String encKey) {
        String fileName = "";
        if (header.fileNameLen > 0) {
            fileName = new String(payloadBytes, HEADER_SIZE, header.fileNameLen, StandardCharsets.UTF_8);
        }

        byte[] data = Arrays.copyOfRange(payloadBytes, HEADER_SIZE + header.fileNameLen, payloadBytes.length);
        data = decryptIfEncrypted(header, data, encKey);
        return new ExtractResult(header.isFile, header.wasEncrypted, wasRandom, fileName, data);
    }

    private static byte[] decryptIfEncrypted(HeaderInfo header, byte[] data, String encKey) {
        if (header.wasEncrypted && (encKey == null || encKey.isEmpty())) {
            throw new IllegalArgumentException("The payload is encrypted, please provide the A5/1 key to extract it.");
        }
        if (!header.wasEncrypted) {
            return data;
        }

        A5 cipher = new A5();
        long key = deriveKey(encKey);
        return cipher.decrypt(data, key, 0);
    }

    private static long deriveKey(String userKey) {
        byte[] hash = SHA256.digest(userKey.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
    }

    private static int extractByteFromPixel(int rgb, Scheme scheme) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int blueBits = scheme.getBlueBits();
        int greenBits = scheme.getGreenBits();
        int blueMask = (1 << blueBits) - 1;
        int greenMask = (1 << greenBits) - 1;
        int redMask = (1 << scheme.getRedBits()) - 1;
        return ((r & redMask) << (greenBits + blueBits)) | ((g & greenMask) << blueBits) | (b & blueMask);
    }

    private static String toNullableString(Long value) {return value == null ? null : String.valueOf(value);}
}