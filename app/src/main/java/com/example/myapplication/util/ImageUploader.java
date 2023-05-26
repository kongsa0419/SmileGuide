package com.example.myapplication.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageUploader {

    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Uploads an image file to the given URL using multipart/form-data POST request with headers.
     *
     * @param url          URL to upload the image file to
     * @param imageFile    File object representing the image to upload
     * @param requestHeaders Map of headers to include in the request
     * @return Response from the server
     * @throws IOException If an error occurs while uploading the image file
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Response uploadImageWithHeader(String url, File imageFile, Map<String, String> requestHeaders) throws IOException {
        // Create a new request builder with the given URL and headers
        Request.Builder builder = new Request.Builder()
                .url(url)
                .headers(Headers.of(requestHeaders));

        // Create a new multipart builder with the image file's content type and content length
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.getName(),
                        RequestBody.create(MediaType.parse(Files.probeContentType(imageFile.toPath())), imageFile));

        // Build the multipart request body and add it to the request builder
        RequestBody requestBody = multipartBuilder.build();
        builder.post(requestBody);

        // Build and execute the POST request, returning the server's response
        Request request = builder.build();
        return client.newCall(request).execute();
    }

}