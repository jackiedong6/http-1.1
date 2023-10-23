package AcceptHandler;

import java.nio.channels.SelectionKey;
import java.io.IOException;
import ChannelHandler.ChannelHandler;

public interface AcceptHandler extends ChannelHandler {
    public void handleAccept(SelectionKey key) throws IOException;
}