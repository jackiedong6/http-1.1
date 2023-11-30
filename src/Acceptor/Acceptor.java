package Acceptor;

import java.nio.channels.*;
import java.util.concurrent.locks.Lock;
import java.io.IOException;
import AcceptHandler.AcceptHandler;
import ApacheConfig.ApacheConfig;
import SocketReadWriteHandlerFactory.SocketReadWriteHandlerFactory;
import ReadWriteHandler.ReadWriteHandler;

public class Acceptor implements AcceptHandler {

    private SocketReadWriteHandlerFactory srwf;
    private String CGI_BIN;
    private ApacheConfig config;
    private Lock acceptLock; 
    public Acceptor(SocketReadWriteHandlerFactory srwf, ApacheConfig config, String CGI_BIN, Lock acceptLock) {
        this.srwf = srwf;
        this.config = config;
        this.CGI_BIN = CGI_BIN;
        this.acceptLock = acceptLock;
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
                ReadWriteHandler rwH = srwf.createHandler(config, CGI_BIN);
                int ops = rwH.getInitOps();
                SelectionKey clientKey = client.register(key.selector(), ops);
                clientKey.attach(rwH); 
            } else {
                DEBUG("handleAccept: client was null, no connection accepted");
            }
        } finally {
            acceptLock.unlock();
        }
    } // end of handleAccept
    private static void DEBUG(String s) {
        System.out.println(s);
    }

} // end of class