package SocketReadWriteHandlerFactory;
import ApacheConfig.ApacheConfig;
import ReadWriteHandler.ReadWriteHandler;
import Timeout.TimeoutThread;

public interface SocketReadWriteHandlerFactory {
    public ReadWriteHandler createHandler(ApacheConfig config, String CGI_BIN, TimeoutThread timeoutThread);
}