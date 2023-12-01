package Management;

import Dispatcher.Dispatcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class ManagementThread extends Thread {
    private final Dispatcher[] dispatcherThreads;
    private ServerSocket welcomeSocket; 

    public ManagementThread(Dispatcher[] dispatcherThreads, ServerSocket welcomeSocket) {
        this.dispatcherThreads = dispatcherThreads;
        this.welcomeSocket = welcomeSocket; 
    }

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("Server Management> ");
                String command = reader.readLine();
                if (command == null) {
                    continue;
                }

                switch (command.trim()) {
                    case "shutdown":
                        welcomeSocket.close();  // This will break out the accept() method in the server's run()
                        System.out.println("Server shutting down. Processing remaining requests...");
                        for (Dispatcher dispatcherThread : dispatcherThreads) {
                            System.out.println("Shutting down a Dispatcher thread...");
                            dispatcherThread.interrupt(); 
                        }
                        // System.out.println("hello world");
                        System.out.println("All requests processed. Server shut down.");
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
}
