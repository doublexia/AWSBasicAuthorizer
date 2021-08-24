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
        connection.setRequestProperty("Content-Type","application/octet-stream");
        connection.setRequestProperty("x-amz-server-side-encryption","AES256");
        connection.setRequestMethod("PUT");
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
        out.write(content);
        out.close();
        connection.disconnect();

        return connection.getResponseCode();
    }
    
    public static int uploadBase64StringToPresignedUrl(String presignedURL, String base64EncodedContent) throws IOException {
        URL url = new URL(presignedURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type","application/octet-stream");
        connection.setRequestProperty("x-amz-server-side-encryption","AES256");
        connection.setRequestMethod("PUT");
        
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        byteArrayStream.write(Base64.getDecoder().decode(encodedString));
        byteArrayStream.writeTo(connection.getOutputStream());
        
        byteArrayStream.close();
        connection.disconnect();

        return connection.getResponseCode();
    }
}
