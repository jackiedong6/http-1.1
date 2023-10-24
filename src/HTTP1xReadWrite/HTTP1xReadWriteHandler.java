package HTTP1xReadWrite;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.*;
import java.io.IOException;

import ApacheConfig.*;
import ReadWriteHandler.ReadWriteHandler;
import java.text.ParseException;
import HTTPInfo.*;

public class HTTP1xReadWriteHandler implements ReadWriteHandler {
    private ByteBuffer inBuffer;
    private ByteBuffer outBuffer;
    String WWW_ROOT;
    String CGI_BIN;
    String urlName;
    String fileName;
    File fileInfo;

    int errCode;
    String errMsg;

    Map<String, String> htaccessContent;
    boolean wwwAuthenticate = false;
    private StringBuffer request;
    private enum State {
        READ_REQUEST,
        PARSE_REQUEST,
        RESPONSE_READY,
        RESPONSE_SENT,
    }
    private State state;
    HTTPRequest httpRequest;
    HTTPResponse httpResponse;
    SimpleDateFormat format; // HTTP Time Format

    ApacheConfig config;
    boolean keepAlive;
    public HTTP1xReadWriteHandler(ApacheConfig config, String CGI_BIN) {
        inBuffer = ByteBuffer.allocate(4096);
        outBuffer = ByteBuffer.allocate(4096);
        request = new StringBuffer(4096);
        state = State.READ_REQUEST;
        format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        keepAlive = false;
        this.config = config;
        // Set WWW Root as the first virtual host
        this.WWW_ROOT = "." + config.getVirtualHosts().get(0).getDocumentRoot() + "/";
    }

    public int getInitOps() {
        return SelectionKey.OP_READ;
    }

    public void handleException() {
    }

    public void handleRead(SelectionKey key) throws IOException {
        // a connection is ready to be read
        DEBUG("A connection is ready to be read. Entered handleRead");

        if (state != State.READ_REQUEST) { // this call should not happen, ignore
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
                DEBUG("HERE");
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
                keepAlive = false;
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
                if (request.toString().endsWith("\r\n\r\n")) {
                    state = State.PARSE_REQUEST;
                    DEBUG("processInBuffer: Request complete");
                    break;
                }
                // end if
            } // end of while
        }
        inBuffer.clear();
        if (state == State.PARSE_REQUEST) {
            httpRequest = new HTTPRequest(new BufferedReader(new StringReader(request.toString())));
            httpRequest.parseRequest();
            processHTTPRequest();
        }
    } // end of process input

    private void processHTTPRequest() {
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

        switch (httpRequest.getHttpMethod()) {
            case ("GET"): {
                processGetRequest();
                if (fileInfo == null) {
                    outputError();
                    break;
                }
                outBuffer = ByteBuffer.allocate((int) (4096 + fileInfo.length()));
                try {
                    outputResponseHeader();
                    outputResponseBody();
                    break;
                } catch (IOException e) {
                    //
                }
            }
        }
    }


    /*
     * Handle a GET request by retrieving the static file referred to
     */
    private void processGetRequest() {
        if (!httpRequest.processAcceptHeader()) {
            errCode = 406;
            errMsg = "Not Acceptable";
            outputError();
        }

        urlName = httpRequest.getUrlName();

        // First, we check if there is a htaccess content file in the directory of the requested url
        htaccessContent = processHtaccess(WWW_ROOT);
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
        if (urlName.equals("")) {
            if (httpRequest.isMobileUserAgent()) {
                fileName = WWW_ROOT + "index_m.html";
                fileInfo = new File(fileName);
                if (fileInfo.isFile()) {
                    if (httpRequest.ifmodifiedSince(fileInfo)) {
                        return;
                    }
                    else if (httpRequest.getHeader("If-Modified-Since") != null){
                        errCode = 304;
                        errMsg = "Not Modified";
                        outputError();
                        return;
                    }
                }
            }
            else {
                fileName = WWW_ROOT + "index.html";
                fileInfo = new File(fileName);
                if(httpRequest.ifmodifiedSince(fileInfo)) {
                    return;
                } else if (httpRequest.getHeader("If-Modified-Since") != null) {
                    errCode = 304;
                    errMsg = "Not Modified";
                    fileInfo = null;
                    outputError();
                    return;
                }
            }
            return;
        }
        // If we have a mobile user agent and the mobile file extension exists, use it
        fileName = WWW_ROOT + urlName;
        fileInfo = new File(fileName);
        if (fileInfo.isFile()) {
            if (httpRequest.ifmodifiedSince(fileInfo)) {
                return;
            } else if (httpRequest.getHeader("If-Modified-Since") != null){
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
                return false;
            }
            WWW_ROOT = "." + documentRoot + "/";
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
        int numOfBytes = (int) fileInfo.length();
        FileInputStream fileStream = new FileInputStream(fileName);
        byte[] fileInBytes = new byte[numOfBytes];
        fileStream.read(fileInBytes);
        outBuffer.put(fileInBytes);
        outBuffer.flip();

        request.delete(0, request.length());
        keepAlive = httpRequest.keepAlive();
        httpRequest = null;
        state = State.RESPONSE_READY;
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
        if (urlName.endsWith(".jpg"))
            return "image/jpeg";
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
        System.out.println(s);
    }

    private void putString(ByteBuffer buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buf.put(bytes);
    }
}