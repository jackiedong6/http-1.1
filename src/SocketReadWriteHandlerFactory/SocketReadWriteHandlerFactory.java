package SocketReadWriteHandlerFactory;
import ReadWriteHandler.ReadWriteHandler;

public interface SocketReadWriteHandlerFactory {
    public ReadWriteHandler createHandler();
}