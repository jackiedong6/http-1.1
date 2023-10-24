package SocketReadWriteHandlerFactory;
import ApacheConfig.ApacheConfig;
import ReadWriteHandler.ReadWriteHandler;

public interface SocketReadWriteHandlerFactory {
    public ReadWriteHandler createHandler(ApacheConfig config, String CGI_BIN);
}