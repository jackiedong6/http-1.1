package HTTPInfo;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

/*
 *
 * Represents a generic HTTP request
 *
 */
public class HTTPRequest {

    public static final String HOST = "Host";
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String CONNECTION = "Connection";
    public static final String AUTHORIZATION = "Authorization";

    private String requestUrl;
    private String httpMethod;
    private String body;
    private String httpVersion;
    private BufferedReader requestReader;
    private HashMap<String, String> allHeaders; // HashMap containing the headers for this message
    SimpleDateFormat format; // HTTP Time Format

    public HTTPRequest(BufferedReader requestReader) {
        this.requestReader = requestReader;
        this.allHeaders = new HashMap<>();
        format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /*
     * Get the http method associated with this request
     */
    public String getHttpMethod() {
        return this.httpMethod;
    }

    /*
     * Get the url associated with this request
     */
    public String getUrlName()
    {
        return requestUrl;
    }

    public void setRequestBody(String body)
    {
        this.body = body;
    }

    public String getRequestBody()
    {
        return body;
    }

    /*
     * Set the value for a header for this http message
     */
    public void setHeader(String headerType, String headerValue) {
        this.allHeaders.put(headerType, headerValue);
    }

    /*
     * Get the value of a header in this http message
     */
    public String getHeader(String headerType) {
        return this.allHeaders.get(headerType);
    }

    /*
     * Return all the headers in the following format:
     * headerType: headerValue
     * e.g. "Content-Length": "15"
     */
    public HashMap<String, String> getAllHeaders() {
        return this.allHeaders;
    }

    /*
     * Get the http version that this message supports
     */
    public String getHttpVersion() {
        return this.httpVersion;
    }

    public void parseRequest() throws Exception {
        String requestMessageLine = requestReader.readLine();
        DEBUG("Request: " + requestMessageLine);
        String[] request = requestMessageLine.split("\\s");

        if (request.length != 3) {
            return;
        }
        this.httpMethod = request[0];
        this.requestUrl = request[1];
        this.httpVersion = request[2];

        if (requestUrl.startsWith("/")) {
            requestUrl = requestUrl.substring(1);
        }

        String requestHeaderLine = requestReader.readLine();
        while (!requestHeaderLine.equals("")) {
            if (requestHeaderLine.contains(": ")) {
                String[] headerParts = requestHeaderLine.split(": ", 2);
                setHeader(headerParts[0], headerParts[1]);
            }
            requestHeaderLine = requestReader.readLine();
        }

        DEBUG(String.valueOf(allHeaders));
    }

    public boolean validateUrl() {
        return !requestUrl.contains("..");
    }

    private void DEBUG(String s) {
        System.out.println(s);
    }

    // TODO: Verify this function
    // Returns true if we have a valid accept header
    public boolean processAcceptHeader() {
        String preferredContentType = null;
        String header = getHeader(ACCEPT);
        // If there is a specified host
        if (header != null) {
            if (header.contains("image/jpeg") && requestUrl.endsWith(".jpg")) {
                preferredContentType = "image/jpeg";
            } else if (header.contains("image/gif") && requestUrl.endsWith(".gif")) {
                preferredContentType = "image/gif";
            } else if (header.contains("text/html") || requestUrl.contains("*/*")) {
                preferredContentType = "text/html";
            } else if (header.contains("text/plain") || requestUrl.contains("*/*")) {
                preferredContentType = "text/plain";
            }
            return !(preferredContentType == null);
        }
        return true;
    }

    public boolean keepAlive() {
        String header = getHeader(CONNECTION);
        return header != null && header.equalsIgnoreCase("keep-alive");
    }

    /*
     * Determine if the request has a mobile user-agent (iPhone)
     */
    public boolean isMobileUserAgent() {
        String userAgent = getHeader(USER_AGENT);
        return userAgent != null && userAgent.contains("iPhone");
    }

    public boolean isSupportedHttpMethod() {
        return httpMethod.equals("GET") || httpMethod.equals("POST");
    }
    public boolean isSupportedHttpVersion() {
        return httpVersion.equals("HTTP/1.1");
    }

    /*
     * Returns true only if the file has been last modified after the given date in the "If-Modified-Since" header
     */
    public boolean ifmodifiedSince(File file) {
        String ifModifiedSince = getHeader(IF_MODIFIED_SINCE);
        if (ifModifiedSince != null) {
            try {
                Date ifModifiedSinceDate = format.parse(ifModifiedSince);
                Date fileLastModified = new Date(file.lastModified());
                Date test1 = format.parse("Sun Oct 22 14:47:06 EDT 2023");
                return fileLastModified.getTime() > ifModifiedSinceDate.getTime();
            }
            catch (ParseException e) {
                // Handle date parsing error
            }
        }
        return false;
    }

    /*
     * Determine if the current Http request has the proper authorization header to access the requested resource
     */
    public boolean processAuthorizationHeader(Map<String, String> htaccessContent)  {
        String authorizationHeader = getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith(htaccessContent.get("AuthType"))) {
            String [] split = authorizationHeader.split("\\s");
            if(split.length != 2) {
                return false;
            }
            String base64Credentials = split[1];
            String decodedCredentials;
            try {
                decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // If we have an invalid base64 encoding of the username/passcode
                return false;
            }
            String [] credSplit = decodedCredentials.split(":");
            if (credSplit.length != 2) {
                return false;
            }
            String username = credSplit[0];
            String password =  credSplit[1];

            return username.equals(new String(Base64.getDecoder().decode(htaccessContent.get("User"))))
                    && password.equals(new String(Base64.getDecoder().decode(htaccessContent.get("Password"))));
        }
        return false;
    }

}