package com.steganography.video;

import java.nio.charset.StandardCharsets;

public final class ExtractResult {
    public final boolean isFile;
    public final boolean wasEncrypted;
    public final boolean wasRandom;
    public final String fileName;
    public final byte[] data;

    public ExtractResult(boolean isFile, boolean wasEncrypted, boolean wasRandom, String fileName, byte[] data) {
        this.isFile = isFile;
        this.wasEncrypted = wasEncrypted;
        this.wasRandom = wasRandom;
        this.fileName = fileName;
        this.data = data;
    }

    public String getText() {return new String(data, StandardCharsets.UTF_8);}
}