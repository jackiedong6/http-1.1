package Acceptor;

import java.nio.channels.*;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.io.IOException;
import AcceptHandler.AcceptHandler;
import ApacheConfig.ApacheConfig;
import SocketReadWriteHandlerFactory.SocketReadWriteHandlerFactory;
import Timeout.TimeoutThread;
import ReadWriteHandler.ReadWriteHandler;

public class Acceptor implements AcceptHandler {

    private SocketReadWriteHandlerFactory srwf;
    private String CGI_BIN;
    private ApacheConfig config;
    private Lock acceptLock; 
    private static boolean debug = false;  
    private TimeoutThread timeoutThread; 

    public Acceptor(SocketReadWriteHandlerFactory srwf, ApacheConfig config, String CGI_BIN, Lock acceptLock, TimeoutThread timeoutThread) {
        this.srwf = srwf;
        this.config = config;
        this.CGI_BIN = CGI_BIN;
        this.acceptLock = acceptLock;
        this.timeoutThread = timeoutThread;
    }

    public void handleException() {
        System.out.println("handleException(): of Acceptor");
    }

    public void handleAccept(SelectionKey key) throws IOException {
        try {
            acceptLock.lock();
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            // extract the ready connection
            SocketChannel client = server.accept();
            if (client != null) {
                DEBUG("handleAccept: Accepted connection from " + client);
                client.configureBlocking(false); 
                ReadWriteHandler rwH = srwf.createHandler(config, CGI_BIN, timeoutThread);
                int ops = rwH.getInitOps();
                SelectionKey clientKey = client.register(key.selector(), ops);
                clientKey.attach(rwH); 
                int hashkey = timeoutThread.addSelectionKeyTimestamp(clientKey, Instant.now().getEpochSecond());
                rwH.setSelectionHashKey(hashkey);
            } else {
                DEBUG("handleAccept: client was null, no connection accepted");
            }
        } finally {
            acceptLock.unlock();
        }
    } // end of handleAccept
    private static void DEBUG(String s) {
        if (debug) {
            System.out.println(s);
        }
    }

} // end of class