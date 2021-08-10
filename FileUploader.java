package com.amazon.hamletresellerportalservice.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FileUploader {
    public static int uploadToPresignedUrl(String presignedURL, String content) throws IOException {
        URL url = new URL(presignedURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type","text/plain");
        connection.setRequestProperty("x-amz-server-side-encryption","AES256");
        connection.setRequestMethod("PUT");
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
        out.write(content);
        out.close();

        return connection.getResponseCode();
    }
}
