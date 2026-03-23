package com.steganography;

import com.steganography.crypto.A5;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        A5 a5 = new A5();
        long key = 0x123456789ABCDEFL;
        int frame = 0;

        String message = "Hello from A5/1 Stream Cipher";

        byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = a5.encrypt(plaintext, key, frame);
        byte[] decrypted = a5.decrypt(ciphertext, key, frame);

        System.out.println("Plaintext  : " + message);
        System.out.println("Ciphertext : " + bytesToHex(ciphertext));
        System.out.println("Decrypted  : " + new String(decrypted, StandardCharsets.UTF_8));
        System.out.println("Match: " + Arrays.equals(plaintext, decrypted));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b));
        }
        return hex.toString();
    }
}