package HTTPInfo;

import java.util.ArrayList;

/*
 *
 * Represents a generic Http Response
 *
 */
public class HTTPResponse extends HTTPMessage {
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String SERVER = "Server";
    public static final String DATE = "Date";
    public static final String LAST_MODIFIED = "Last-Modified";

    int statusCode;
    String statusMessage; // e.g. OK
    String content; // will probably be a Content object that can represent static or dynamic content
    ArrayList<String> payload;
    String preferredContentType;

    public HTTPResponse(String httpVersion)
    {
        super(httpVersion);
        statusCode = 200;
        statusMessage = "OK";
        content = null;
        payload = null;
    }

    /*
     * Set a new status code for this response
     */
    public void setStatusCode(int newCode)
    {
        statusCode = newCode;
    }

    /*
     * Get the payload (Response body) of this response
     */
    public ArrayList<String> getPayload()
    {
        return payload;
    }

    /*
     * Set the payload (Response body) of this response
     */
    public void setPayload(ArrayList<String> payload)
    {
        this.payload = payload;
    }
}