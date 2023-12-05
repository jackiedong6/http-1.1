package HTTP1xReadWrite;
import ApacheConfig.ApacheConfig;
import SocketReadWriteHandlerFactory.SocketReadWriteHandlerFactory;
import Timeout.TimeoutThread;
import ReadWriteHandler.ReadWriteHandler;


public class HTTP1xReadWriteHandlerFactory implements SocketReadWriteHandlerFactory {
    public ReadWriteHandler createHandler(ApacheConfig config, String CGI_BIN, TimeoutThread timeoutThread) {
        return new HTTP1xReadWriteHandler(config, CGI_BIN, timeoutThread);
    }
}