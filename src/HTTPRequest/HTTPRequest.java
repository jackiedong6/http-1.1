package HTTPRequest;

import com.sun.net.httpserver.Request;

/*
 *
 * Represents a generic HTTP request
 *
 */
public class HTTPRequest extends HTTPMessage {
    public static final String HOST = "Host";
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String CONNECTION = "Connection";
    public static final String AUTHORIZATION = "Authorization";

    private String requestMethod;
    private String requestUrl;
    private String body;

    public HTTPRequest(String requestMethod, String requestUrl, String httpVersion)
    {
        super(httpVersion);
        this.requestMethod = requestMethod;
        this.requestUrl = requestUrl;
    }
    /*
     * Get the http method associated with this request
     */
    public String getHttpMethod()
    {
        return requestMethod;
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

}