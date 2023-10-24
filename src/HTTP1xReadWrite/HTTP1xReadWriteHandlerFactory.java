package HTTP1xReadWrite;
import ApacheConfig.ApacheConfig;
import SocketReadWriteHandlerFactory.SocketReadWriteHandlerFactory;
import ReadWriteHandler.ReadWriteHandler;


public class HTTP1xReadWriteHandlerFactory implements SocketReadWriteHandlerFactory {
    public ReadWriteHandler createHandler(ApacheConfig config, String CGI_BIN) {
        return new HTTP1xReadWriteHandler(config, CGI_BIN);
    }
}