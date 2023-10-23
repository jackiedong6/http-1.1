package ReadWriteHandler;
import java.nio.channels.SelectionKey;
import java.io.IOException;
import ChannelHandler.ChannelHandler;

public interface ReadWriteHandler extends ChannelHandler {

    public void handleRead(SelectionKey key) throws IOException;

    public void handleWrite(SelectionKey key) throws IOException;

    public int getInitOps();
}