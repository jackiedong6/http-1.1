import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import ApacheConfig.ApacheConfig;
import Dispatcher.Dispatcher;
import Acceptor.Acceptor;
import SocketReadWriteHandlerFactory.SocketReadWriteHandlerFactory;
import HTTP1xReadWrite.HTTP1xReadWriteHandlerFactory;
import Management.ManagementThread;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
public class server {
    public static String CGI_BIN = "cgi-bin";
    private static ServerSocket welcomeSocket; 
    private static Dispatcher[] dispatcherThreads; 

    public static ServerSocketChannel openServerChannel(int port) {
        ServerSocketChannel serverChannel = null;
        try {

            // open server socket for accept
            serverChannel = ServerSocketChannel.open();
            // extract server socket of the server channel and bind the port
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            welcomeSocket = ss; 
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

        // get socket channel for this port 
        ServerSocketChannel sch = openServerChannel(config.getServerPort());

        // create a lock to keep separate threads from accessing the same accept connection 
        Lock acceptLock = new ReentrantLock(); 

        try {
            /*
             * create n dispatchers to provide nSelectLoops 
             */
            // start n select loops 
            int numSelectLoops = config.getNumSelectLoops();
            dispatcherThreads = new Dispatcher[numSelectLoops]; 
            for (int i = 0; i < numSelectLoops; i++) {
                // get dispacher/selector
                Dispatcher dispatcher = new Dispatcher();

                // create server acceptor for Echo Line ReadWrite Handler 
                SocketReadWriteHandlerFactory readWriteFactory = new HTTP1xReadWriteHandlerFactory(); 
                Acceptor acceptor = new Acceptor(readWriteFactory, config, CGI_BIN, acceptLock); 
                
                SelectionKey key = sch.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);
                key.attach(acceptor);

                // start dispatcher
                System.out.println("Starting thread: " + i); 
                dispatcherThreads[i] = dispatcher;
                dispatcherThreads[i].start();
            }
            ManagementThread management = new ManagementThread(dispatcherThreads, welcomeSocket); 
            management.start();
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