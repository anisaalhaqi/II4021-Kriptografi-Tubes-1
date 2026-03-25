package com.steganography.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Writer {
    @FunctionalInterface
    public interface VideoFrameTransformer {
        BufferedImage transform(BufferedImage frame, int imageIndex, long timestampMicros) throws Exception;
    }

    public static int writeTransformedFrames(File outputFile, File sourceFile, double frameRate, int width, int height,
            VideoFrameTransformer transformer) throws Exception {

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFile)) {
            grabber.start();

            int audioChannels = Math.max(0, grabber.getAudioChannels());
            int sampleRate = grabber.getSampleRate();

            try (FFmpegFrameRecorder recorder = createStartedRecorder(outputFile, width, height, audioChannels, frameRate, sampleRate)) {
                Java2DFrameConverter converter = new Java2DFrameConverter();
                int imageIndex = 0;
                Frame sourceFrame;

                while ((sourceFrame = grabber.grab()) != null) {
                    if (sourceFrame.image != null) {
                        BufferedImage original = converter.convert(sourceFrame);
                        if (original == null) {
                            continue;
                        }

                        BufferedImage inputImage = ensureRecorderImage(original, width, height);
                        BufferedImage transformed = transformer.transform(inputImage, imageIndex, sourceFrame.timestamp);
                        BufferedImage outputImage = ensureRecorderImage(transformed, width, height);

                        Frame outputFrame = converter.getFrame(outputImage);
                        recorder.record(outputFrame);
                        imageIndex++;
                    } else if (sourceFrame.samples != null && audioChannels > 0) {
                        recorder.record(sourceFrame);
                    }
                }

                recorder.stop();
                return imageIndex;
            }
        }
    }

    private static FFmpegFrameRecorder createStartedRecorder(File outputFile, int width, int height, int audioChannels, double frameRate,
            int sampleRate) throws Exception {
                
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height, audioChannels);
        try {
            configureRecorder(recorder, audioChannels, frameRate, sampleRate);
            recorder.start();
            return recorder;
        } catch (Exception ex) {
            closeRecorderQuietly(recorder);
            deleteIfExists(outputFile);
            throw ex;
        }
    }

    private static void configureRecorder(FFmpegFrameRecorder recorder, int audioChannels, double frameRate, int sampleRate) {
        recorder.setFormat("avi");
        recorder.setFrameRate(frameRate);
        recorder.setPixelFormat(AV_PIX_FMT_RGB24);
        recorder.setVideoCodec(AV_CODEC_ID_HUFFYUV); // So we'll use HuffYuv since its the legacy code + stable (lossless too)
        // needs to run it on vlc player or sum cuz windows player doesn't support the codec

        if (audioChannels > 0) {
            recorder.setAudioCodec(AV_CODEC_ID_PCM_S16LE);
            recorder.setAudioChannels(audioChannels);
            recorder.setSampleRate(sampleRate);
            recorder.setSampleFormat(AV_SAMPLE_FMT_S16);
        }
    }

    private static void closeRecorderQuietly(FFmpegFrameRecorder recorder) {
        try {
            recorder.stop();
        } catch (Exception ignored) {
        }
        try {
            recorder.release();
        } catch (Exception ignored) {
        }
    }

    private static void deleteIfExists(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    private static BufferedImage ensureRecorderImage(BufferedImage img, int width, int height) {
        if (img.getType() == BufferedImage.TYPE_3BYTE_BGR && img.getWidth() == width && img.getHeight() == height) {
            return img;
        }

        BufferedImage recorderImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = recorderImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return recorderImage;
    }
}