import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

//import org.json.JSONException;
//import org.json.JSONObject;

//import static org.junit.Assert.assertEquals;

public abstract class AbstractTestCase {
    public static void main(String[] args) throws IOException {
        String response = new AbstractTestCase() {}.post("requestDocumentUrl",
            "{\n" + "    \"name\":\"name\",\n" + "    \"type\":\"type\",\n" + "    \"keys\": [\n" +
                "        {\"key\": \"RMA2\",\"value\":\"RRabcdefgh\"},\n" +
                "        {\"key\":\"SHP2\", \"value\":\"SO12345678\"}\n" + "        ],\n" +
                "        \"description\": \"description of this document\",\n" +
                "        \"requestSource\": \"SOURCE\",\n" + "        \"locationId\":\"LocationID\",\n" +
                "        \"userId\": \"MyName\"\n" + "}\n"
        );
        System.out.println("RESPONSE="+response);
    }

    protected static final String ACCESS_KEY_ID     = "<access-key>";
    protected static final String ACCESS_KEY_SECRET = "<secret-key>";
    protected static final String URL_PREFIX        = "<invoke-url>";

//
//
//    IBM UI  (using u/p) -> CEVA AWS (...) -- AWS IAM --> DRPIL using keys
//
//    CEVA:
//        API gateway -> DRPIL API Gateway (?)
//       custom authorizer to support u/p
//        L in Python -> call DRPIL
    /**
     * Make a post request to our API Gateway.
     * Our gateway is an AWS API Gateway with specific prefix, with no apiKey, accepts POST,
     * and will return HTTP 200 if succeeded.
     * This method will use a pre-existing user which is created to test DRPI-Devo, and will not use STS.
     * @param method  Our method to be called.
     * @param payload The post body.
     * @return The response body.
     * @throws IOException If unexpected error happens.
     */
    protected String post(String method, String payload) throws IOException {
        String url = URL_PREFIX + method;
        String apiKey = null;
        String stsSecurityToken = null;
        Integer httpStatusCodeExpected = 200;
        return request(ACCESS_KEY_ID, ACCESS_KEY_SECRET, url, apiKey, stsSecurityToken,
                payload, httpStatusCodeExpected);
    }

    /**
     * Make a request to AWS API Gateway, with Signature Version 4 signing. The logic follows
     * <a href="https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html">AWS official document</a>.
     * @param accessKeyId            The Access-Key-Id of the IAM user.
     * @param accessKeySecret        The Access-Key-Secret of the IAM user.
     * @param urlText                The URL of our API Gateway. Should be completed with protocol.
     * @param apiKey                 The API Key. Used if we set our API Gateway requiring it.
     * @param stsSecurityToken       The STS Security token. Used if we call with assumed role.
     * @param payload                Post body. Use null for GET.
     * @param httpStatusCodeExpected The HTTP status code we expect from the response. Will throw if set and unmatched.
     * @return                       The response body.
     * @throws IOException           If unexpected error happens.
     */
    private static String request(String accessKeyId, String accessKeySecret, String urlText, String apiKey,
            String stsSecurityToken, String payload, Integer httpStatusCodeExpected) throws IOException {
        String method;
        String contentType;
        if (null == payload) {
            method = "GET";
            contentType = null;
        } else {
            method = "POST";
            contentType = "application/x-amz-json-1.0";
        }
        // The time stamp must be within 15 minutes of the AWS system time when the request is received.
        // If it isn't, the request fails with the RequestExpired error code to prevent someone else from replaying your requests.
        long dateMillis = System.currentTimeMillis();
        URL url = new URL(urlText);
        String[] domainParts = url.getHost().split("\\.");
        // Our URL have a well-known format <moreInfo*>.<serviceName>.<awsRegion>.amazonaws.com, so the following fetch.
        String awsRegion = domainParts[domainParts.length - 3];
        String serviceName = domainParts[domainParts.length - 4];
        byte[] payloadBytes = null == payload ? null : payload.getBytes(CHARSET_UTF8);
        String dateYMD = makeGMTDateString("yyyyMMdd", dateMillis);
        // Data prepared. Initiate the connection.
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        try {
            // Follow the document and prepare the http request headers.
            String headerHost = url.getHost();
            String headerXAmzDate = makeGMTDateString("yyyyMMdd'T'HHmmss'Z'", dateMillis);
            String[] signedHeaderArray = createSignedHeaderArray(null != payload);
            String signedHeaders = Arrays.stream(signedHeaderArray).collect(Collectors.joining(";"));
            String canonicalRequest = createCanonicalRequest(method, contentType, url, payloadBytes, headerHost,
                    headerXAmzDate, signedHeaderArray, signedHeaders);
            String credentialScope = dateYMD + "/" + awsRegion + "/" + serviceName + "/aws4_request";
            String stringToSign = createStringToSign(headerXAmzDate, credentialScope, canonicalRequest);
            String signature = calculateSignature(accessKeySecret, awsRegion, serviceName, dateYMD, stringToSign);
            con.setRequestProperty("Host", headerHost);
            con.setRequestProperty("x-amz-date", headerXAmzDate);
            addSignatureToRequest(con, accessKeyId, credentialScope, signedHeaders, signature);
            if (null != contentType) {
                con.setRequestProperty("Content-Type", contentType);
            }
            if (null != payloadBytes) {
                con.setRequestProperty("Content-Length", Integer.toString(payloadBytes.length));
            }
            if (null != apiKey) {
                con.setRequestProperty("x-api-key", apiKey);
            }
            if (null != stsSecurityToken) {
                con.setRequestProperty("X-Amz-Security-Token", stsSecurityToken);
            }
            if (null != payloadBytes) {
                con.setDoOutput(true);
            }
            // Connection prepared. Start the connection.
            if (null != payloadBytes) {
                try (OutputStream outStream = con.getOutputStream()) {
                    outStream.write(payloadBytes);
                    outStream.flush();
                }
            } else {
                con.connect();
            }
            // Request sent. Reading response.
            int responseCode = con.getResponseCode();
            InputStream in;
            if (200 == responseCode) {
                in = con.getInputStream();
            } else {
                in = con.getErrorStream();
            }
            String response;
            try (Reader sr = new InputStreamReader(in, CHARSET_UTF8)) {
                @SuppressWarnings("resource")
                Scanner scanner = new Scanner(sr);
                try {
                    response = scanner.useDelimiter("\\A").next();
                } catch (NoSuchElementException e) {
                    response = "";
                }
            }
            if (null != httpStatusCodeExpected && httpStatusCodeExpected != responseCode) {
                throw new java.net.ConnectException("Returned httpStatusCode " + responseCode + " does not match" +
                        " expected " + httpStatusCodeExpected + ". Http body: " + response);
            }
            return response;
        }
        finally {
            con.disconnect();
        }
    }

    /**
     * Create the array containing the headers that we want to sign. Here we chose the minimum required values.
     * @param isPost whether this request is POST.
     * @return the created array.
     */
    private static String[] createSignedHeaderArray(boolean isPost) {
        String signedHeaderDefine; // SignedHeaders = Lowercase(HeaderName0) + ';' + Lowercase(HeaderName1) + ";" + ... + Lowercase(HeaderNameN)
        if (isPost) {
            signedHeaderDefine = "host;x-amz-date;content-type";
        } else {
            signedHeaderDefine = "host;x-amz-date";
        }
        return Arrays.stream(signedHeaderDefine.split("\\;")).sorted().toArray(String[]::new);
    }

    /**
     * Create canonical request following
     * <a href="https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html">the document</a>.
     * @param method Request method.
     * @param contentType Request content type.
     * @param url Request URL.
     * @param payloadBytes Post body in bytes. Null for GET.
     * @param headerHost The value for header: host.
     * @param headerXAmzDate The value for header: x-amz-date.
     * @param signedHeaderArray The headers to sign in array form.
     * @param signedHeaders The headers to sign in string form.
     * @return The created canonical request.
     */
    private static String createCanonicalRequest(String method, String contentType, URL url, byte[] payloadBytes,
            String headerHost, String headerXAmzDate, String[] signedHeaderArray, String signedHeaders) {
        StringBuilder canonicalRequestBuffer = new StringBuilder();
        canonicalRequestBuffer.append(method + "\n");
        String pathURIEncoded = url.getPath();
        if (pathURIEncoded.length() == 0) {
            pathURIEncoded = "/";
        }
        String pathSegmentURIEncoded = Arrays.stream(pathURIEncoded.split("\\/", -1)).map(segment -> {
            try {
                return URLEncoder.encode(segment, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                throw new AssertionError("No UTF-8", e);
            }
        }).collect(Collectors.joining("/"));
        // Each path segment must be URI-encoded twice (except for Amazon S3 which only gets URI-encoded once).
        canonicalRequestBuffer.append(pathSegmentURIEncoded + "\n");
        String query = url.getQuery();
        if (null == query) {
            query = "";
        } else {
            TreeMap<String, List<String>> queryDetails = new TreeMap<>();
            for (String kv : query.split("\\&")) {
                int index = kv.indexOf('=');
                String key;
                String value;
                if (-1 == index) {
                    key = kv;
                    value = "";
                } else {
                    key = kv.substring(0, index);
                    value = kv.substring(index + 1);
                }
                List<String> vs = queryDetails.get(key);
                if (null == vs) {
                    vs = new ArrayList<>();
                    queryDetails.put(key, vs);
                }
                vs.add(value);
            }
            StringBuilder queryBuilder = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : queryDetails.entrySet()) {
                String key = null;
                try {
                    key = URLDecoder.decode(entry.getKey(), "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    throw new AssertionError("No UTF-8", e);
                }
                for (String value : entry.getValue()) {
                    if (queryBuilder.length() > 0) {
                        queryBuilder.append("&");
                    }
                    queryEncode(queryBuilder, key);
                    queryBuilder.append('=');
                    queryEncode(queryBuilder, value);
                }
            }
            query = queryBuilder.toString();
        }
        canonicalRequestBuffer.append(query + "\n");
        canonicalRequestBuffer.append(Arrays.stream(signedHeaderArray).map(header -> {
            String value;
            switch(header) {
                case "content-type":
                    if (null == contentType) {
                        throw new AssertionError("Should not reach here.");
                    }
                    value = contentType;
                    break;
                case "host":
                    value = headerHost;
                    break;
                case "x-amz-date":
                    value = headerXAmzDate;
                    break;
                default:
                    throw new AssertionError("Should not reach here.");
            }
            return header.toLowerCase().trim() + ":" + value.trim().replaceAll("[ ][ ]*", " ") + "\n";
        }).collect(Collectors.joining("")) + "\n");
        canonicalRequestBuffer.append(signedHeaders + "\n");
        canonicalRequestBuffer.append(printHexBinary(sha256(null == payloadBytes ? new byte[0] : payloadBytes)));
        return canonicalRequestBuffer.toString();
    }

    /**
     * Create the string to sign following
     * <a href="https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html">the document</a>.
     * @param requestDateTime The request date time.
     * @param credentialScope The credential scope.
     * @param canonicalRequest The previously created canonical request.
     * @return The created string to sign.
     */
    private static String createStringToSign(String requestDateTime, String credentialScope, String canonicalRequest) {
        String Algorithm = "AWS4-HMAC-SHA256";
        String hashedCanonicalRequest = printHexBinary(sha256(canonicalRequest.getBytes(CHARSET_UTF8)));
        return Algorithm + "\n"
                + requestDateTime + "\n"
                + credentialScope + "\n"
                + hashedCanonicalRequest;
    }

    /**
     * Calculate the signature following
     * <a href="https://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html">the document</a>.
     * @param accessKeySecret The access-key-secret of the IAM user.
     * @param awsRegion The aws region in the request URL.
     * @param serviceName The service name in the request URL.
     * @param dateYMD The date in YMD format.
     * @param stringToSign The string to sign.
     * @return The calculated signature.
     */
    private static String calculateSignature(String accessKeySecret, String awsRegion, String serviceName,
            String dateYMD, String stringToSign) {
        try {
            byte[] kDate = hmacSha256(dateYMD, ("AWS4" + accessKeySecret).getBytes(CHARSET_UTF8));
            byte[] kRegion = hmacSha256(awsRegion, kDate);
            byte[] kService = hmacSha256(serviceName, kRegion);
            byte[] kSigning = hmacSha256("aws4_request", kService);
            return printHexBinary(hmacSha256(stringToSign, kSigning));
        } catch (InvalidKeyException e) {
            throw new AssertionError("Manually assigned key should never be invalid.", e);
        }
    }

    /**
     * Add signature to request following
     * <a href="https://docs.aws.amazon.com/general/latest/gr/sigv4-add-signature-to-request.html">the document</a>.
     * @param con The connection.
     * @param accessKeyId The access-key-id of the IAM user.
     * @param credentialScope The credential scope.
     * @param signedHeaders The list of the signed headers' name.
     * @param signature The calculated signature.
     */
    private static void addSignatureToRequest(HttpsURLConnection con, String accessKeyId,
            String credentialScope, String signedHeaders, String signature) {
        String authorization = MessageFormat.format("AWS4-HMAC-SHA256 Credential={0}/{1}, SignedHeaders={2}, Signature={3}",
                accessKeyId, credentialScope, signedHeaders, signature);
        con.setRequestProperty("Authorization", authorization);
    }

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    /**
     * Encode the query according to AWS Signature V4 requires.
     * @param buffer The string buffer to append encoded text.
     * @param text The source text.
     */
    private static void queryEncode(StringBuilder buffer, String text) {
        for (byte b : text.getBytes(CHARSET_UTF8)) {
            if ((b >= 'A' && b <= 'Z')
                    || (b >= 'a' && b <= 'z')
                    || (b >= '0' && b <= '9')
                    || b == '-'
                    || b == '_'
                    || b == '.'
                    || b == '~') {
                buffer.append((char) b);
            } else {
                int h = 0xf & (b >> 4);
                int l = 0xf & b;
                buffer.append('%');
                buffer.append(h > 9 ? (char) ('A' + (h - 10)) : (char) ('0' + h));
                buffer.append(l > 9 ? (char) ('A' + (l - 10)) : (char) ('0' + l));
            }
        }
    }

    /**
     * Make a string form of a time in millis, in GMT timezone.
     * @param format The format of output string.
     * @param millis The input time in millis.
     * @return The expected string form.
     */
    private static String makeGMTDateString(String format, long millis) {
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        return fmt.format(millis);
    }

    /**
     * Convert the input byte array into its hex form, lower cased.
     * @param bytes The input data.
     * @return The hex form.
     */
    private static String printHexBinary(byte[] bytes) {
        int count = bytes.length;
        StringBuilder buffer = new StringBuilder(count * 2);
        for (int i = 0; i < count; i++) {
            byte b = bytes[i];
            int h = 0xf & (b >> 4);
            int l = 0xf & b;
            buffer.append(h > 9 ? (char) ('a' + (h - 10)) : (char) ('0' + h));
            buffer.append(l > 9 ? (char) ('a' + (l - 10)) : (char) ('0' + l));
        }
        return buffer.toString();
    }

    /**
     * Do a SHA256 with the input bytes.
     * @param bytes The input data.
     * @return The result byte array.
     */
    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Do a HMAC-SHA256 with the input data and key.
     * @param data The input data.
     * @param key  The input key.
     * @return The result byte array.
     * @throws InvalidKeyException if the input key is invalid by content of by size.
     */
    private static byte[] hmacSha256(String data, byte[] key) throws InvalidKeyException {
        Mac sha256_HMAC = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        sha256_HMAC.init(new SecretKeySpec(key, "HmacSHA256"));
        return sha256_HMAC.doFinal(data.getBytes(CHARSET_UTF8));
    }
}
