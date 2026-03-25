package com.steganography.video;

public final class EmbedResult {
    public final double mse;
    public final double psnr;
    public final int[][] origHist;
    public final int[][] stegHist;

    public EmbedResult(double mse, double psnr, int[][] origHist, int[][] stegHist) {
        this.mse = mse;
        this.psnr = psnr;
        this.origHist = origHist;
        this.stegHist = stegHist;
    }
}