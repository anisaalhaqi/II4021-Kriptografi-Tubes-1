package com.steganography;

import com.steganography.ui.MainApp;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Logger.getLogger("com.sun.javafx.application.PlatformImpl").setLevel(Level.OFF);
        MainApp.main(args);
    }
}