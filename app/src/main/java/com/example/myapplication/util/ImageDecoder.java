package com.example.myapplication.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class ImageDecoder {

    /**
     * Decodes a Base64-encoded image string and saves it to a file with the specified filename.
     *
     * @param base64String Base64-encoded string representing the image
     * @param filename     Name of the file to save the decoded image to
     * @throws IOException If an error occurs while decoding and saving the image
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void decodeImage(String base64String, String filename) throws IOException {
        // Decode the Base64-encoded string to a byte array
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);

        // Write the byte array to a new file with the specified filename
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(decodedBytes);
        }
    }

}