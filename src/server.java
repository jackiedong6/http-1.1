import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import ApacheConfig.ApacheConfig;
import Dispatcher.Dispatcher;
import Acceptor.Acceptor;
import SocketReadWriteHandlerFactory.SocketReadWriteHandlerFactory;
import HTTP1xReadWrite.HTTP1xReadWriteHandlerFactory;

public class server {

    public static ServerSocketChannel openServerChannel(int port) {
        ServerSocketChannel serverChannel = null;
        try {

            // open server socket for accept
            serverChannel = ServerSocketChannel.open();
            // extract server socket of the server channel and bind the port
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            // configure channel to be non-blocking
            serverChannel.configureBlocking(false);
            DEBUG("Server listening for connections on port " + port);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } // end of catch
        return serverChannel;
    } // end of open serverChannel


    public static void main(String[] args) throws Exception {

        if (args.length < 2 || !args[0].equals("-config")) {
            System.out.println("Usage: java server -config <config_file_name>");
            return;
        }
        String configFile = args[1];
        ApacheConfig config = new ApacheConfig(configFile);

        config.parse();

        // get dispatcher/selector
        Dispatcher dispatcher = new Dispatcher();


        ServerSocketChannel sch = openServerChannel(config.getServerPort());

        // create server acceptor for Echo Line ReadWrite Handler
        SocketReadWriteHandlerFactory echoFactory = new HTTP1xReadWriteHandlerFactory();
        Acceptor acceptor = new Acceptor(echoFactory);

        Thread dispatcherThread;
        // register the server channel to a selector
        try {
            SelectionKey key = sch.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);
            key.attach(acceptor);
            // start dispatcher
            dispatcherThread = new Thread(dispatcher);
            dispatcherThread.start();
        } catch (IOException ex) {
            System.out.println("Cannot register and start server");
            System.exit(1);
        }
        // may need to join the dispatcher thread

    } // end of main
    private static void DEBUG(String s) {
        System.out.println(s);
    }

} // end of class