package com.steganography.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.BitSet;
import java.util.Random;

import com.steganography.crypto.A5;
import com.steganography.crypto.SHA256;
import com.steganography.utils.common.Scheme;
import com.steganography.utils.common.Metrics;
import com.steganography.utils.encoder.PixelEncoder;
import com.steganography.utils.encoder.RandomEmbedSession;
import com.steganography.utils.encoder.SequentialEmbedSession;

// please refer to utils/encoder/*EmbedSession.java for detail :3
public class Embedder {
    private static final int FLAG_FILE = 0x01; // for text or file, representation: 001
    private static final int FLAG_ENCRYPTED = 0x02; // for encrypted or not, representation: 010
    private static final int FLAG_RANDOM = 0x04; // for random scheme or not, representation: 100

    // [flags][scheme][dataSize][fileNameLen]
    // from left to right: 1 + 1 + 4 + 2 = 8 byte
    private static final int HEADER_SIZE = 8;

    public EmbedResult embedText(File input, File output, String text, Long stegoKey, Long a5Key) throws Exception {
        return embedText(input, output, text, toNullableString(stegoKey), toNullableString(a5Key), Scheme.RGB_332);
    }

    public EmbedResult embedFile(File input, File output, File secretFile, Long stegoKey, Long a5Key) throws Exception {
        return embedFile(input, output, secretFile, toNullableString(stegoKey), toNullableString(a5Key), Scheme.RGB_332);
    }

    public EmbedResult embedText(File input, File output, String text, String stegoKey, String encKey, Scheme scheme) throws Exception {
        Reader reader = new Reader(input);
        reader.readMetadata();
        if (stegoKey == null || stegoKey.isBlank()) {
            return embedSequential(reader, input, output, createSequentialTextSession(text,
                    encKey != null && !encKey.isBlank(), encKey, scheme,
                    calculateCapacityBits(reader.getWidth(), reader.getHeight(), reader.getTotalFrames())));
        }

        long totalPixels = (long) reader.getWidth() * reader.getHeight() * reader.getTotalFrames();
        return embedRandom(reader, input, output,
                createRandomTextSession(text, encKey != null && !encKey.isBlank(), encKey, scheme, totalPixels, stegoKey));
    }

    public EmbedResult embedFile(File input, File output, File secretFile, String stegoKey, String encKey, Scheme scheme) throws Exception {
        byte[] fileData = Files.readAllBytes(secretFile.toPath());
        Reader reader = new Reader(input);
        reader.readMetadata();
        if (stegoKey == null || stegoKey.isBlank()) {
            return embedSequential(reader, input, output, createSequentialFileSession(fileData,
                    secretFile.getName(), encKey != null && !encKey.isBlank(), encKey, scheme,
                    calculateCapacityBits(reader.getWidth(), reader.getHeight(), reader.getTotalFrames())));
        }

        long totalPixels = (long) reader.getWidth() * reader.getHeight() * reader.getTotalFrames();
        return embedRandom(reader, input, output, createRandomFileSession(fileData, secretFile.getName(), encKey != null && !encKey.isBlank(), encKey, scheme, totalPixels, stegoKey));
    }

    /* So, the idea are to read 1 frames (and so on if the frames are alr full),
    then we will take pixel sequentially from left to right and top to bottom
    remember that 1 pixel contain r,g,b (which each of them are 8 bits).
    depends on the lsb scheme, we'll embed 1 byte of the payload to each of each r,g,b lsb.
    Continue until the payload are nothing left */ 
    private EmbedResult embedSequential(Reader reader, File input, File output, SequentialEmbedSession session) throws Exception {
        long[][] origHistogram = new long[3][256];
        long[][] stegHistogram = new long[3][256];

        int writtenFrames = Writer.writeTransformedFrames(output, input, reader.getFrameRate(),
                reader.getWidth(), reader.getHeight(),
                (frame, imageIndex, timestampMicros) -> {
                    Metrics.accumulateHistogramData(frame, origHistogram);
                    session.embedFrame(frame);
                    Metrics.accumulateHistogramData(frame, stegHistogram);
                    return frame;
                });

        if (writtenFrames <= 0) {
            throw new IllegalStateException("No video frames found in selected video.");
        }
        if (!session.isComplete()) {
            throw new IllegalStateException("Not enough video capacity to embed the full payload.");
        }

        long totalSamples = (long) writtenFrames * reader.getWidth() * reader.getHeight() * 3L;
        double mse = totalSamples == 0 ? 0.0 : (double) session.getSquaredError() / totalSamples;
        double psnr = mse == 0.0 ? Double.POSITIVE_INFINITY : 10.0 * Math.log10((255.0 * 255.0) / mse);

        return new EmbedResult(mse, psnr, Metrics.averageHistogramData(origHistogram, writtenFrames), Metrics.averageHistogramData(stegHistogram, writtenFrames));
    }

    /* So, the idea are to read 1 frames (and so on if the frames are alr full),
    then we will generate random number from seed that generated from the stego key.
    These random onl to pick random pixel positions in 1 frame, then the rest are same step
    as the sequential (so yeah, the computation time will slightly spike but its ok) */ 
    private EmbedResult embedRandom(Reader reader, File input, File output, RandomEmbedSession session) throws Exception {
        long[][] origHistogram = new long[3][256];
        long[][] stegHistogram = new long[3][256];

        int writtenFrames = Writer.writeTransformedFrames(output, input, reader.getFrameRate(),
                reader.getWidth(), reader.getHeight(),
                (frame, imageIndex, timestampMicros) -> {
                    Metrics.accumulateHistogramData(frame, origHistogram);
                    session.embedFrame(frame);
                    Metrics.accumulateHistogramData(frame, stegHistogram);
                    return frame;
                });

        if (writtenFrames <= 0) {
            throw new IllegalStateException("No video frames found in selected video.");
        }
        if (!session.isComplete()) {
            throw new IllegalStateException("Not enough video capacity to embed the full payload.");
        }

        long totalSamples = (long) writtenFrames * reader.getWidth() * reader.getHeight() * 3L;
        double mse = totalSamples == 0 ? 0.0 : (double) session.getSquaredError() / totalSamples;
        double psnr = mse == 0.0 ? Double.POSITIVE_INFINITY : 10.0 * Math.log10((255.0 * 255.0) / mse);

        return new EmbedResult(mse, psnr, Metrics.averageHistogramData(origHistogram, writtenFrames), Metrics.averageHistogramData(stegHistogram, writtenFrames));
    }

    private static String toNullableString(Long value) {return value == null ? null : String.valueOf(value);}

    public static SequentialEmbedSession createSequentialTextSession(String message, boolean encrypt, String encKey,
            Scheme scheme, long capacityBits) {

        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        return createSequentialSession(payload, false, "", encrypt, encKey, scheme, capacityBits);
    }

    public static SequentialEmbedSession createSequentialFileSession(byte[] fileData, String fileName,
            boolean encrypt, String encKey, Scheme scheme, long capacityBits) {

        return createSequentialSession(fileData, true, fileName, encrypt, encKey, scheme, capacityBits);
    }

    public static RandomEmbedSession createRandomTextSession(String message, boolean encrypt, String encKey,
            Scheme scheme, long totalPixels, String stegoKey) {

        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        return createRandomSession(payload, false, "", encrypt, encKey, scheme, totalPixels, stegoKey);
    }

    public static RandomEmbedSession createRandomFileSession(byte[] fileData, String fileName, boolean encrypt,
            String encKey, Scheme scheme, long totalPixels, String stegoKey) {

        return createRandomSession(fileData, true, fileName, encrypt, encKey, scheme, totalPixels, stegoKey);
    }

    public static long calculateCapacityBits(int width, int height, int totalFrames) {
        return (long) width * height * totalFrames * 8L;
    }

    public static void validateCapacityBits(long requiredBits, long capacityBits) {
        if (requiredBits > capacityBits) {
            throw new IllegalArgumentException(String.format(
                    "Message too large! Need %d bits but only %d bits available (%.2f KB > %.2f KB)",
                    requiredBits, capacityBits,
                    requiredBits / 8.0 / 1024.0,
                    capacityBits / 8.0 / 1024.0));
        }
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
            throw new IllegalArgumentException("Random sampling exceeds available pixels.");
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

    private static SequentialEmbedSession createSequentialSession(byte[] payload, boolean isFile, String fileName,
            boolean encrypt, String encKey, Scheme scheme, long capacityBits) {

        byte[] fullPayload = preparePayload(payload, isFile, fileName, encrypt, encKey, false, scheme);
        validateCapacityBits((long) fullPayload.length * 8L, capacityBits);
        return new SequentialEmbedSession(fullPayload, createPixelEncoder(scheme));
    }

    private static RandomEmbedSession createRandomSession(byte[] payload, boolean isFile, String fileName,
            boolean encrypt, String encKey, Scheme scheme, long totalPixels, String stegoKey) {

        byte[] fullPayload = preparePayload(payload, isFile, fileName, encrypt, encKey, true, scheme);
        validateCapacityBits((long) fullPayload.length * 8L, totalPixels * 8L);
        return new RandomEmbedSession(fullPayload, computeSeed(stegoKey), createPixelEncoder(scheme));
    }

    private static PixelEncoder createPixelEncoder(Scheme scheme) {
        return new PixelEncoder(scheme);
    }

    private static byte[] preparePayload(byte[] payload, boolean isFile, String fileName, boolean encrypt, String encKey, boolean randomMode, Scheme scheme) {
        byte[] data = payload;
        if (encrypt && encKey != null && !encKey.isEmpty()) {
            A5 cipher = new A5();
            long key = deriveKey(encKey);
            data = cipher.encrypt(payload, key, 0);
        }

        byte[] header = buildHeader(isFile, encrypt, randomMode, scheme, fileName, data.length);
        byte[] fullPayload = new byte[header.length + data.length];
        System.arraycopy(header, 0, fullPayload, 0, header.length);
        System.arraycopy(data, 0, fullPayload, header.length, data.length);
        return fullPayload;
    }

    private static byte[] buildHeader(boolean isFile, boolean encrypt, boolean randomMode, Scheme scheme, String fileName, int dataSize) {
        byte flags = 0;
        if (isFile) {
            flags |= FLAG_FILE;
        }
        if (encrypt) {
            flags |= FLAG_ENCRYPTED;
        }
        if (randomMode) {
            flags |= FLAG_RANDOM;
        }

        byte[] fileNameBytes = (isFile && fileName != null) ? fileName.getBytes(StandardCharsets.UTF_8) : new byte[0];
        int fileNameLen = fileNameBytes.length;

        ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE + fileNameLen);
        buf.put(flags);
        buf.put((byte) scheme.getId());
        buf.putInt(dataSize);
        buf.putShort((short) fileNameLen);

        if (fileNameLen > 0) {
            buf.put(fileNameBytes);
        }
        return buf.array();
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

    private static long deriveKey(String userKey) {
        byte[] hash = SHA256.digest(userKey.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
    }
}