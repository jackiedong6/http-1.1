package Management;

import Dispatcher.Dispatcher;
import Timeout.TimeoutThread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class ManagementThread extends Thread {
    private final Dispatcher[] dispatcherThreads;
    private TimeoutThread timeoutThread; 
    private ServerSocket welcomeSocket; 
    private static boolean debug = false;

    public ManagementThread(Dispatcher[] dispatcherThreads, TimeoutThread timeoutThread, ServerSocket welcomeSocket) {
        this.dispatcherThreads = dispatcherThreads;
        this.welcomeSocket = welcomeSocket;
        this.timeoutThread = timeoutThread;
    }

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.println("Server Management> ");
                String command = reader.readLine();
                if (command == null) {
                    continue;
                }

                switch (command.trim()) {
                    case "shutdown":
                        welcomeSocket.close();  // This will break out the accept() method in the server's run()
                        timeoutThread.interrupt(); 
                        DEBUG("Server shutting down. Processing remaining requests...");
                        for (Dispatcher dispatcherThread : dispatcherThreads) {
                            DEBUG("Shutting down a Dispatcher thread...");
                            dispatcherThread.interrupt(); 
                        }
                        // System.out.println("hello world");
                        DEBUG("All requests processed. Server shut down.");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Unknown command");
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("ManagementThread encountered an error: " + e.getMessage());
        }
    }

    private static void DEBUG(String s) {
        if (debug) {
            System.out.println(s);
        }
    }
}
