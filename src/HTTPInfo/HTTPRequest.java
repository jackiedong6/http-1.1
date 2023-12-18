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

    private static boolean debug = false;
    private String requestUrl;
    private String httpMethod;
    private String requestBody;
    private String httpVersion;
    private String requestString;
    private HashMap<String, String> allHeaders; // HashMap containing the headers for this message
    SimpleDateFormat format; // HTTP Time Format

    public HTTPRequest(String requestString) {
        this.requestString = requestString;
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

    public void setRequestBody(String requestBody)
    {
        this.requestBody = requestBody;
    }

    public String getRequestBody()
    {
        return requestBody;
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
        String[] lines = requestString.split("\r\n"); // Adjust based on your request's line delimiter

        // Process the request line
        String[] requestParts = lines[0].split("\\s");
        if (requestParts.length != 3) {
            return;
        }
        this.httpMethod = requestParts[0];
        this.requestUrl = requestParts[1].startsWith("/") ? requestParts[1].substring(1) : requestParts[1];
        this.httpVersion = requestParts[2];

        // Process headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) break; // Assuming empty line denotes end of headers

            int colonIndex = line.indexOf(": ");
            if (colonIndex != -1) {
                setHeader(line.substring(0, colonIndex), line.substring(colonIndex + 2));
            }
        }

        DEBUG(String.valueOf(allHeaders));
    }

    public void parseBody(BufferedReader bodyReader) throws Exception{
        StringBuilder requestBodyBuilder = new StringBuilder();
        String requestBodyLine;
        // should only be one line?
        while((requestBodyLine = bodyReader.readLine()) != null)  {
            requestBodyBuilder.append(requestBodyLine);
            DEBUG("RequestBodyLine: " + requestBodyLine);
        }
        setRequestBody(requestBodyBuilder.toString());

        String contentType = getHeader("Content-Type");
        if (contentType == null) {
            DEBUG("Content-Type header is missing or null");
            return;
        }
        // Parse the body
        switch (contentType)
        {
            case ("application/x-www-form-urlencoded"):
            {
                 DEBUG("Parsing body with content type: " + contentType);
                // get the body from the rest of the input
                break;
            }
            case ("application/json"):
            {
//                String jsonFileName = requestReader.readLine();
                // DEBUG("jsonFileName: " + jsonFileName);
                //TODO: parse the contents of the file
                break;
            }
            default:
            {
                DEBUG(requestBody);
                // handle error
                DEBUG("Detected file format not supported on this server");
                break;
            }
        } // end of parse body
    }

    public boolean validateUrl() {
        return !requestUrl.contains("..");
    }

    private void DEBUG(String s) {
        if (debug) {
            System.out.println(s);
        }
    }

    // TODO: Verify this function
    // Returns true if we have a valid accept header
    public boolean processAcceptHeader() {
        String preferredContentType = null;
        String header = getHeader(ACCEPT);
        // If there is a specified host
        if (header != null) {
            if (header.contains("*/*")) {
                preferredContentType = "text/html";
            }
            else if (header.contains("image/jpeg") && requestUrl.endsWith(".jpg")) {
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
        if (header == null) {
            return true;
        }
        return !header.equalsIgnoreCase("close");
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
        DEBUG("Authorization Header: " + authorizationHeader);
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
            DEBUG(Arrays.toString(credSplit));
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

    /*
     * Determine if the User-Agent that sent this request is from a browser (Safari, Chrome)
     */
    public Boolean isUserAgentABrowser() {
        String userAgent = getHeader(USER_AGENT);
        if (userAgent == null) {
            return false;
        }
        String safari = "Safari";
        String chrome = "Chrome";
        String firefox = "Firefox";
        int sPosition = userAgent.indexOf(safari);
        int cPosition = userAgent.indexOf(chrome);
        int fPosition = userAgent.indexOf(firefox);
        if (sPosition == -1 && cPosition == -1 && fPosition == -1) {
            return false;
        }
        return true; 
    }

    /*
     * Print an entire http request, including all headers and the body
     */
    public void printHttpRequest() {
        DEBUG("PRINTING HTTP REQUEST:");
        // print the first line 
        System.out.println(httpMethod + " " + requestUrl + " " + httpVersion);
        // print the headers 
        for (Map.Entry<String, String> element: allHeaders.entrySet()) {
            System.out.println(element.getKey() + ": " + element.getValue()); 
        }
        System.out.println("");
        // print the body
        System.out.println(requestBody); 
        DEBUG("DONE PRINTING HTTP REQUEST");
    }

}