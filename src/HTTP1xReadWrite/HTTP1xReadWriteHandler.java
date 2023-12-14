package HTTP1xReadWrite;

import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.*;
import java.io.IOException;

import ApacheConfig.*;
import ReadWriteHandler.ReadWriteHandler;
import HTTPInfo.*;
import Cache.Cache;

public class HTTP1xReadWriteHandler implements ReadWriteHandler {
    private static boolean debug = true;
    private ByteBuffer inBuffer;
    private ByteBuffer outBuffer;
    private Cache cache;
    String WWW_ROOT;
    String CGI_BIN;
    String urlName;
    String fileName;
    String directory;
    File fileInfo;

    int errCode;
    String errMsg;

    Map<String, String> htaccessContent;
    boolean wwwAuthenticate = false;
    private StringBuffer request;
    private StringBuffer requestBody;
    private enum State {
        READ_REQUEST,
        READ_BODY,
        PARSE_REQUEST,
        RESPONSE_READY,
        RESPONSE_SENT,
    }
    private State state;
    HTTPRequest httpRequest;
    HTTPResponse httpResponse;
    SimpleDateFormat format; // HTTP Time Format
    ArrayList<String> payload;
    ApacheConfig config;
    boolean keepAlive;
    boolean chunkedEncoding = true;
    int bodyLength = 0;
    private char[] lastFourChars = new char[4];
    private int lastFourIndex = 0;

    public HTTP1xReadWriteHandler(ApacheConfig config, String CGI_BIN) {
        inBuffer = ByteBuffer.allocate(4096);
        outBuffer = ByteBuffer.allocate(4096);
        request = new StringBuffer(4096);
        requestBody = new StringBuffer(4096);
        state = State.READ_REQUEST;
        format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        keepAlive = true;
        cache = new Cache();
        this.config = config;
        // Set WWW Root as the first virtual host
        this.WWW_ROOT = config.getVirtualHosts().get(0).getDocumentRoot() + "/";
        this.payload = null;
        this.CGI_BIN = CGI_BIN;
    }
    public int getInitOps() {
        return SelectionKey.OP_READ;
    }
    public void handleException() {}
    public void handleRead(SelectionKey key) throws IOException {
        // a connection is ready to be read
        DEBUG("A connection is ready to be read. Entered handleRead");

        if (state != State.READ_REQUEST && state != State.READ_BODY) { // this call should not happen, ignore
            return;
        }

        try {
            // process the data
            processInBuffer(key);
        } catch (Exception e){
            //
        }
        // update state
        updateSelectorState(key);
        DEBUG("handleRead->");

    } // end of handleRead
    private void updateSelectorState(SelectionKey key) {
        DEBUG("Entered Update Dispatcher Selector State.");
        if (state == State.RESPONSE_SENT) {
            try {
                key.channel().close();
                key.cancel();
            } catch (IOException e) {
                // handle exception, e.g., log it
            }
            return;
        }
        int nextState = key.interestOps();
        switch (state) {
            // If we are reading the request we aren't interested in writing
            case READ_REQUEST:
                nextState = (nextState | SelectionKey.OP_READ) & ~SelectionKey.OP_WRITE;
                break;
            case READ_BODY:
                nextState = (nextState | SelectionKey.OP_READ) & ~SelectionKey.OP_WRITE;
                break;
            // If we are done reading the request then we are not interested in reading or writing, but parsing
            case PARSE_REQUEST:
                nextState = nextState & ~SelectionKey.OP_READ & ~SelectionKey.OP_WRITE;
                break;
            case RESPONSE_READY:
                nextState = (nextState | SelectionKey.OP_WRITE) & ~SelectionKey.OP_READ;
                break;
        }
        key.interestOps(nextState);
    }
    public void handleWrite(SelectionKey key) throws IOException {
        DEBUG("->handleWrite");
        // process data
        SocketChannel client = (SocketChannel) key.channel();
        DEBUG("handleWrite: Write data to connection " + client + "; from buffer " + outBuffer);
        int writeBytes = client.write(outBuffer);
        DEBUG("handleWrite: write " + writeBytes + " bytes; after write " + outBuffer);
        if ((state == State.RESPONSE_READY) && (outBuffer.remaining() == 0)) {
            if(keepAlive) {
                state = State.READ_REQUEST;
                httpRequest = null;
            }
            else {
                state = State.RESPONSE_SENT;
            }
        }
        outBuffer.clear();
        // update state
        updateSelectorState(key);
        // try {Thread.sleep(5000);} catch (InterruptedException e) {}
        DEBUG("handleWrite->");
    } // end of handleWrite

    private void checkPostBody() {
        if (httpRequest.getHttpMethod().equals("POST")) {
            if (httpRequest.getHeader("Content-Type") == null){
                errCode = 500;
                outputError();
                return;
            }
            if (httpRequest.getHeader("Content-Length") != null) {
                bodyLength = Integer.parseInt(httpRequest.getHeader("Content-Length"));
            }
            state = State.READ_BODY;
        }
        else {
            state = State.PARSE_REQUEST;
        }
    }
    private void processInBuffer(SelectionKey key) throws Exception {
        DEBUG("processInBuffer");
        SocketChannel client = (SocketChannel) key.channel();
        int readBytes = client.read(inBuffer);
        DEBUG("handleRead: Read data from connection " + client + " for " + readBytes + " byte(s); to buffer "
                + inBuffer);
        // end of our stream
        if (readBytes == -1) {
            state = State.RESPONSE_READY;
            DEBUG("handleRead: readBytes == -1");
        } else {
            inBuffer.flip(); // read input
            while (state != State.PARSE_REQUEST && inBuffer.hasRemaining() && request.length() < request.capacity()) {
                char ch = (char) inBuffer.get();
                request.append(ch);
                lastFourChars[lastFourIndex % 4] = ch;
                lastFourIndex++;

                if (lastFourIndex > 3
                        && lastFourChars[(lastFourIndex-1) % 4] == '\n'
                        && lastFourChars[(lastFourIndex-2) % 4] == '\r'
                        && lastFourChars[(lastFourIndex-3) % 4] == '\n'
                        && lastFourChars[(lastFourIndex-4) % 4] == '\r') {
                    httpRequest = new HTTPRequest(request.toString());
                    httpRequest.parseRequest();
                    checkPostBody();
                    DEBUG("Finished reading Headers");
                }

                if (state == State.READ_BODY) {
                    while(bodyLength > 0 && inBuffer.hasRemaining() && request.length() < request.capacity()) {
                        ch = (char) inBuffer.get();
                        requestBody.append(ch);
                        bodyLength--;
                    }
                    if (bodyLength == 0) {
                        state = State.PARSE_REQUEST;
                        break;
                    }
                }
                // end if
            } // end of while
        }
        inBuffer.clear();
        if (state == State.PARSE_REQUEST) {
            processHTTPRequest(key);
        }
    } // end of process input



    private void processHTTPRequest(SelectionKey key) throws Exception {
        DEBUG("-> processHTTPRequest");
        if(httpRequest.getHttpMethod() == null || httpRequest.getHttpVersion() == null|| httpRequest.getUrlName() == null) {
            errCode = 500;
            errMsg = "Bad Request";
            outputError();
            return;
        }
        if (!httpRequest.isSupportedHttpMethod()) {
            errCode = 500;
            errMsg = "Server does not recognize " + httpRequest.getHttpMethod() + " method";
            outputError();
            return;
        }
        if (!httpRequest.isSupportedHttpVersion()) {
            errCode = 505;
            errMsg = "Version Not Supported";
            outputError();
            return;
        }
        if (!httpRequest.validateUrl()) {
            errCode = 403;
            errMsg = "Forbidden";
            fileInfo = null;
            outputError();
            return;
        }
        if(!processHostHeader()) {
            errCode = 400;
            errMsg = "Invalid Host Header";
            outputError();
            return;
        }
        urlName = httpRequest.getUrlName();

        // First, we check if there is a htaccess content file in the directory of the requested url
        String [] urlNameParts = urlName.split("/");
        directory = String.join("/", Arrays.copyOf(urlNameParts, urlNameParts.length - 1));
        htaccessContent = processHtaccess(WWW_ROOT + directory);
        if (htaccessContent != null) {
            if(!httpRequest.processAuthorizationHeader(htaccessContent)) {
                errCode = 401;
                errMsg = "Unauthorized";
                // We have not specified the authorization header even though it is required
                if (httpRequest.getHeader("Authorization") == null) {
                    wwwAuthenticate = true;
                }
                outputError();
                return;
            }
        }

        switch (httpRequest.getHttpMethod()) {
            case ("GET"): {
                processGetRequest();
                if (fileInfo == null) {
                    outputError();
                    break;
                }
                try {
                    Cache.CachedContent cachedContent = cache.get(fileName);
                    long lastModified = fileInfo.lastModified();
                    if (cachedContent == null || cachedContent.lastModified < lastModified) {
                        outBuffer.clear();
                        outputResponseHeader();
                        outputResponseBody();
                        cache.put(fileName, outBuffer, lastModified);
                    }
                    else {
                        outBuffer = cachedContent.content;
                        request.delete(0, request.length());
                        keepAlive = httpRequest.keepAlive();
                        httpRequest = null;
                        state = State.RESPONSE_READY;
                    }
                    break;
                } catch (IOException e) {
                    //
                    break;
                }
            }
            case ("POST"): {
                DEBUG("HERE");
                processPostRequest(key);
                putString(outBuffer,"HTTP/1.1 200 OK\r\n");
                outputPostResponse();
                break;
            }
        }
    }
    private void processPostRequest(SelectionKey key) throws Exception {
        httpRequest.parseBody(new BufferedReader(new StringReader(requestBody.toString())));
        // select the cgi script to handle the request
        String[] partsOfUrl = urlName.split("\\?");
        DEBUG(urlName);
        fileName = WWW_ROOT + urlName;
        fileInfo = new File(fileName);
        DEBUG("filename: " + fileName);
        // get the arguments from the request body

        DEBUG("parse body");
        String scriptArgs = httpRequest.getRequestBody();
        DEBUG("after parse body");
        // create and set up the process environment
        ProcessBuilder pb = new ProcessBuilder(fileName);
        SocketChannel client = (SocketChannel) key.channel();
        Socket socket = client.socket();
        String clientNetworkAddress = socket.getInetAddress().getHostAddress();
        String fullyQualifiedDomainName = socket.getInetAddress().getCanonicalHostName();;
        String remoteIdentity = "";
        String remoteUser = "";
        String serverName = config.getVirtualHosts().get(0).getServerName();
        String currentServerPort = "6789";
        String serverProtocol = httpRequest.getHttpVersion();
        String serverSoftware = "Jackie and Cesar's HTTP/1.0 Server";
        // set environment variables
        Map<String, String> env = pb.environment();

        if (partsOfUrl.length > 1) {
            env.put("QUERY_STRING", partsOfUrl[1]);
        }
        DEBUG("HERE on 345");
        env.put("REQUEST_METHOD", httpRequest.getHttpMethod());
        env.put("REMOTE_ADDR", clientNetworkAddress); // set to network address of the client sending the request to the server
        env.put("REMOTE_HOST", fullyQualifiedDomainName); // set to fully qualified domain name of client (or NULL)
        env.put("REMOTE_IDENT", remoteIdentity); // may be used to provide identity information reported about the connection
        env.put("REMOTE_USER", remoteUser); // set to a user identification string supplied by the client as part of user authentication
        env.put("SERVER_NAME", serverName); // set to the name of the server host to which the client request is directed
        env.put("SERVER_PORT", currentServerPort); // set to the TCP/IP port number on which this request is received
        env.put("SERVER_PROTOCOL", serverProtocol); // set to name and version of the application protocol used for this CGI request
        env.put("SERVER_SOFTWARE", serverSoftware);
        // redirect the stdout of the script to a file descriptor
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);

        DEBUG("HERE on 358");
        try {
            DEBUG("HERE IN PROCESS");
            // run the executable with the args passed in
            Process p = pb.start();
            try (OutputStream stdin = p.getOutputStream()) {
                stdin.write(scriptArgs.getBytes());
                stdin.flush();
            } // Automatically closes the stream
            // convert the cgi response into an HTTP response
            InputStream cgiResponseStream = p.getInputStream(); // read data from the stdout of the process p
            BufferedReader reader = new BufferedReader(new InputStreamReader(cgiResponseStream));
            ArrayList<String> cgiResponse = new ArrayList<>();
            String line;
            // Record the script response line-by-line
            while ((line = reader.readLine()) != null) {
                cgiResponse.add(line);
            }
            payload = cgiResponse;
        } catch (IOException e) {
            // log error here
            DEBUG("Error in starting the cgi script");
            DEBUG("Exception: " + e);
            errCode = 500;
            errMsg = "Internal Server Error: Could not create dynamic content";
            outputError();
        }
    }
    /*
     * Handle a GET request by retrieving the static file referred to
     */
    private void processGetRequest() throws IOException {
        if (!httpRequest.processAcceptHeader()) {
            errCode = 406;
            errMsg = "Not Acceptable";
            fileInfo = null;
            return;
        }
        if (urlName.equals("")) {
            if (httpRequest.isMobileUserAgent()) {
                fileName = WWW_ROOT + "index_m.html";
            } else {
                fileName = WWW_ROOT + "index.html";
            }
        } else {
            fileName = WWW_ROOT + urlName;
        }

        fileInfo = new File(fileName);

        if (fileInfo.isFile()) {
            if (httpRequest.ifmodifiedSince(fileInfo)) {
                return;
            } else if (httpRequest.getHeader("If-Modified-Since") != null) {
                errCode = 304;
                errMsg = "Not Modified";
                fileInfo = null;
            }
        } else {
            errCode = 404;
            errMsg = "Not Found";
            fileInfo = null;
        }
    }
    private boolean processHostHeader() {
        // If there is a specified host
        String host = httpRequest.getHeader("Host");
        if(host != null) {
            String documentRoot = null;
            for (VirtualHost vh : config.getVirtualHosts()) {
                if (vh.getServerName().equals(host)) {
                    documentRoot = vh.getDocumentRoot();
                }
            }
            if (documentRoot == null) {
                documentRoot = config.getVirtualHosts().get(0).getDocumentRoot();
            }
            WWW_ROOT = documentRoot + "/";
        }
        return true;
    }
    /*
     * Returns a Map containing the contents of the given directory's authorization file
     */
    private Map<String, String> processHtaccess(String directoryRoot) {
        File htaccessFile = new File (directoryRoot, ".htaccess");
        if (!htaccessFile.isFile()) {
            return null;
        }
        // Map of fields from htaccess content
        Map <String, String> htaccessContent = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(htaccessFile));
            String htaccessLine;
            while ((htaccessLine = reader.readLine()) != null) {
                String[] split = htaccessLine.split("\\s",2);
                htaccessContent.put(split[0], split[1]);

            }
        } catch (IOException e) {
            // handle the exception here
        }
        return htaccessContent;
    }

    private void outputPostBody() {
        if (chunkedEncoding) {
            String contentType = "";
            for (int i = 0; i < payload.size(); i++) {
                if (payload.get(i).startsWith("Content-Type:")) {
                    contentType = payload.get(i);
                    payload.remove(i);
                }
            }
            putString(outBuffer, "Transfer-Encoding: chunked\r\n");
            putString(outBuffer, contentType);
            putString(outBuffer, "\r\n\r\n");
            for (String str: payload) {
                if(str.isEmpty()) {
                    continue;
                }
                putString(outBuffer, String.valueOf(str.length()));
                putString(outBuffer, "\r\n");
                putString(outBuffer, str);
                putString(outBuffer, "\r\n");
            }
            putString(outBuffer, "0\r\n");
            putString(outBuffer, "\r\n");
        } else {
            if (fileInfo.isFile()) {
                putString(outBuffer, "Last-Modified: " + format.format(new Date(fileInfo.lastModified())) + "\r\n");
            }
            putString(outBuffer, "Content-Length: " + fileInfo.length() + "\r\n");
            for (String str : payload) {
                DEBUG(str);
                putString(outBuffer, str);
                putString(outBuffer, "\r\n");
            }
        }
    }

    private void outputPostResponse() {
        putString(outBuffer, "\r\n");
        putString(outBuffer, "Date: " + format.format(new Date()) + "\r\n");
        putString(outBuffer, "Server: Jackie and Cesar's HTTP/1.0 Server\r\n");
        // output the response body from CGI response
        outputPostBody();
        outBuffer.flip();
        request.delete(0, request.length());
        keepAlive = httpRequest.keepAlive();
        httpRequest = null;
        state = State.RESPONSE_READY;
    }
    private void outputResponseHeader() {
        putString(outBuffer, "HTTP/1.1 200 Document Follows\r\n");
        putString(outBuffer, "Set-Cookie: " + generateCookie() + "\r\n");
        putString(outBuffer, "Date: " + generateDate() + "\r\n");
        putString(outBuffer, "Server: Jackie and Cesar's HTTP/1.0 Server\r\n");
        if (fileInfo != null) {
            putString(outBuffer, "Last-Modified: " + format.format(new Date(fileInfo.lastModified())) + "\r\n");
        }
        putString(outBuffer, "Content-Type: " + contentType() + "\r\n");
        putString(outBuffer, "Content-Length: " + fileInfo.length() + "\r\n");
        putString(outBuffer, "\r\n");

    }
    private void outputResponseBody() throws IOException {
        // Assuming fileInfo is a File object referring to the file to be sent
        try (FileInputStream fileInputStream = new FileInputStream(fileInfo);
             FileChannel fileChannel = fileInputStream.getChannel()) {
            int numOfBytes = (int) fileInfo.length();
            // Ensure the buffer is large enough to hold the file content
            if (outBuffer.capacity() < numOfBytes) {
                ByteBuffer tempBuffer = ByteBuffer.allocate(numOfBytes);
                outBuffer.flip(); // Switch to reading mode to read existing data
                tempBuffer.put(outBuffer); // Copy existing data to the new buffer
                outBuffer = tempBuffer; // Replace outBuffer with the new buffer
            }
            // Read the file content directly into the buffer
            while (fileChannel.read(outBuffer) > 0) {
                // Continue reading until EOF
            }
        }
        keepAlive = httpRequest.keepAlive();
        outBuffer.flip(); // Prepare the buffer for writing to the socket channel

        request.delete(0, request.length());
        keepAlive = httpRequest.keepAlive();
        httpRequest = null;
        state = State.RESPONSE_READY;

        DEBUG("AFTER RESPONSE BODY");
    }
    private void outputError(){
        putString(outBuffer,"HTTP/1.1 " + errCode + " " + errMsg + "\r\n");
        if (wwwAuthenticate) {
            putString(outBuffer, "WWW-Authenticate: " + htaccessContent.get("AuthType") + "=" + htaccessContent.get("AuthName") +"\r\n");
        }
        putString(outBuffer,"Date: " + format.format(new Date()) + "\r\n");
        putString(outBuffer,"Server: Jackie and Cesar's HTTP/1.0 Server\r\n");
        if (fileInfo != null) {
            putString(outBuffer,"Last-Modified: " + format.format(new Date(fileInfo.lastModified())) + "\r\n");
        }
        if (httpRequest.getUrlName() != null) {
            putString(outBuffer, "Content-Type: " + contentType() + "\r\n");
        }
        putString(outBuffer, "Content-Length: 0\r\n"); // No content for error messages
        putString(outBuffer,"\r\n");

        outBuffer.flip();
        request.delete(0, request.length());
        keepAlive = httpRequest.keepAlive();
        httpRequest = null;
        state = State.RESPONSE_READY;
    }
    private String contentType() {
        urlName = httpRequest.getUrlName();
        if (urlName.endsWith(".jpg")) {
            return "image/jpeg";
        }
        else if (urlName.endsWith(".gif"))
            return "image/gif";
        else if (urlName.endsWith(".html"))
            return "text/html";
        else if (urlName.equals("")){
            return "text/html";
        }
        else
            return "text/plain";
    }
    // generateDate generates the current date.
    protected String generateDate() {
        return format.format(new Date());
    }
    // generateCookie generates a random UUID for the cookie value.
    protected String generateCookie() {
        return UUID.randomUUID().toString();
    }
    private static void DEBUG(String s) {
        if (debug) {
            System.out.println(s);
        }
    }
    private void putString(ByteBuffer buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buf.put(bytes);
    }
}