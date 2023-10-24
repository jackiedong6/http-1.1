package HTTPInfo;

/*
 * Encapsulates the data for an Http Message
 * Child classes include HttpRequest, HttpResponse
 */

import java.util.HashMap;

public class HTTPMessage {
    // Headers

    String httpVersion;
    HashMap<String, String> allHeaders; // HashMap containing the headers for this message

    public HTTPMessage(String httpVersion)
    {
        allHeaders = new HashMap<>();
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
}
