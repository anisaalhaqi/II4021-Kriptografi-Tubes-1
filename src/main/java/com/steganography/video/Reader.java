package com.steganography.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Reader {
    @FunctionalInterface
    public interface FrameProcessor {
        boolean process(BufferedImage frame, int frameIndex) throws Exception;
    }

    private final File videoFile;
    private double frameRate;
    private int width;
    private int height;
    private int totalFrames;
    private String videoCodecName;
    private int videoBitrate;

    public Reader(File videoFile) {
        this.videoFile = videoFile;
    }

    public void readMetadata() throws Exception {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();

            this.frameRate = grabber.getFrameRate();
            this.width = grabber.getImageWidth();
            this.height = grabber.getImageHeight();
            this.totalFrames = resolveVideoFrameCount(grabber);
            this.videoCodecName = grabber.getVideoCodecName();
            this.videoBitrate = grabber.getVideoBitrate();

            grabber.stop();
        }
    }

    public List<BufferedImage> readFrames() throws Exception {
        List<BufferedImage> frames = new ArrayList<>();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();
            this.frameRate = grabber.getFrameRate();
            this.width = grabber.getImageWidth();
            this.height = grabber.getImageHeight();
            this.totalFrames = resolveVideoFrameCount(grabber);
            this.videoCodecName = grabber.getVideoCodecName();
            this.videoBitrate = grabber.getVideoBitrate();

            Java2DFrameConverter converter = new Java2DFrameConverter();

            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                BufferedImage img = converter.convert(frame);
                if (img != null) {
                    BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D g = copy.createGraphics();
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                    frames.add(copy);
                }
            }
            grabber.stop();
        }
        return frames;
    }

    public void processFrames(FrameProcessor processor) throws Exception {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();
            this.frameRate = grabber.getFrameRate();
            this.width = grabber.getImageWidth();
            this.height = grabber.getImageHeight();
            this.totalFrames = resolveVideoFrameCount(grabber);
            this.videoCodecName = grabber.getVideoCodecName();
            this.videoBitrate = grabber.getVideoBitrate();

            Java2DFrameConverter converter = new Java2DFrameConverter();
            int frameIndex = 0;

            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                BufferedImage img = converter.convert(frame);
                if (img == null) {
                    continue;
                }

                BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D g = copy.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();

                if (!processor.process(copy, frameIndex++)) {
                    break;
                }
            }
            grabber.stop();
        }
    }

    public double getFrameRate() {return frameRate;}
    public int getWidth() {return width;}
    public int getHeight() {return height;}
    public int getTotalFrames() {return totalFrames;}
    public String getVideoCodecName() {return videoCodecName;}
    public int getVideoBitrate() {return videoBitrate;}

    private static int resolveVideoFrameCount(FFmpegFrameGrabber grabber) {
        int videoFrames = grabber.getLengthInVideoFrames();
        if (videoFrames > 0) {
            return videoFrames;
        }
        return grabber.getLengthInFrames();
    }
}