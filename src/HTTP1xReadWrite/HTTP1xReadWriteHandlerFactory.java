package HTTP1xReadWrite;
import SocketReadWriteHandlerFactory.SocketReadWriteHandlerFactory;
import ReadWriteHandler.ReadWriteHandler;


public class HTTP1xReadWriteHandlerFactory implements SocketReadWriteHandlerFactory {
    public ReadWriteHandler createHandler() {
        return new HTTP1xReadWriteHandler();
    }
}