package HTTP1xReadWrite;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import ReadWriteHandler.ReadWriteHandler;

public class HTTP1xReadWriteHandler implements ReadWriteHandler {
    private ByteBuffer inBuffer;
    private ByteBuffer outBuffer;
    String WWW_ROOT;
    String CGI_BIN;
    String urlName;
    String fileName;
    File fileInfo;

    int errCode;
    String errorMsg;
    Map<String, String> htaccessContent;
    String preferredContentType = null; // Accept: specified by client via header
    boolean closeConnection = false; // Connection: specifies if we keep a connection alive
    boolean wwwAuthenticate = false;


    private int defaultBufferSize = 4096;

    private StringBuffer request;

    private enum State {
        READ_REQUEST,
        REQUEST_COMPLETE,
        RESPONSE_READY,
        CONN_CLOSED,
    }
    private State state;

    public HTTP1xReadWriteHandler() {
        inBuffer = ByteBuffer.allocate(defaultBufferSize);
        outBuffer = ByteBuffer.allocate(defaultBufferSize);

        request = new StringBuffer(4096);

        state = State.READ_REQUEST;
    }

    public int getInitOps() {
        return SelectionKey.OP_READ;
    }

    public void handleException() {
    }

    public void handleRead(SelectionKey key) throws IOException {
        // a connection is ready to be read
        DEBUG("A connection is ready to be read. Entered handleRead");

        if (state == State.REQUEST_COMPLETE) { // this call should not happen, ignore
            return;
        }

        // process the data
        processInBuffer(key);

        // update state
        updateSelectorState(key);

        DEBUG("handleRead->");

    } // end of handleRead

    private void updateSelectorState(SelectionKey key) throws IOException {
        DEBUG("Entered Update Dispatcher Selector State.");


        if (state == State.CONN_CLOSED) {
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
            // If we are done reading the request then we are not interested in reading or writing, but parsing
            case REQUEST_COMPLETE:
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
            state = State.CONN_CLOSED;
        }
        // update state
        updateSelectorState(key);

        // try {Thread.sleep(5000);} catch (InterruptedException e) {}
        DEBUG("handleWrite->");
    } // end of handleWrite

    private void processInBuffer(SelectionKey key) throws IOException {
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

            while (state != State.REQUEST_COMPLETE && inBuffer.hasRemaining() && request.length() < request.capacity()) {
                char ch = (char) inBuffer.get();
                request.append(ch);
                if (request.toString().endsWith("\r\n\r\n")) {
                    state = State.REQUEST_COMPLETE;
                    DEBUG("processInBuffer: Request complete");
                    break;
                }
                // end if
            } // end of while
        }
        inBuffer.clear();
        if (state == State.REQUEST_COMPLETE) {
            parseRequest();
        }

    } // end of process input

    private void parseRequest() throws IOException {
        BufferedReader requestReader = new BufferedReader(new StringReader(request.toString()));

        // Parse the Request Line
        String requestMessageLine = requestReader.readLine();

        DEBUG("Request: " + requestMessageLine);
        String[] request = requestMessageLine.split("\\s");

        DEBUG("Request Info:" + Arrays.toString(request));
        // first line should be in the following format: <HTTP method> <URL> <HTTP version>
        if (request.length != 3) {
            errCode = 500;
            errorMsg = "Bad Request";
        }

        String httpMethod = request[0];
        urlName = request[1];
        String httpVersion = request[2];

        if (!httpMethod.equals("GET") && !httpMethod.equals("POST")) {
            errCode = 500;
            errorMsg = "Server does not recognize " + httpMethod + " method";
        }
        if (!httpVersion.equals("HTTP/1.1")) {
            errCode = 505;
            errorMsg = "Version Not Supported";
        }
        if (urlName.startsWith("/")) {
            urlName = urlName.substring(1);
        }


        for (int i = 0; i < request.length(); i++) {
            char ch = (char) request.charAt(i);
            ch = Character.toUpperCase(ch);
            outBuffer.put((byte) ch);
        }
        outBuffer.flip();

        state = State.RESPONSE_READY;
    } // end of generate response

    private static void DEBUG(String s) {
        System.out.println(s);
    }

}