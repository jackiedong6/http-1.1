package Dispatcher;

import AcceptHandler.AcceptHandler;
import ReadWriteHandler.ReadWriteHandler;
import Timeout.TimeoutThread;

import java.nio.channels.*;
import java.io.IOException;
import java.util.*; // for Set and Iterator

public class Dispatcher extends Thread {

    private Selector selector;
    private static boolean debug = false;
    private TimeoutThread timeoutThread;  

    public Dispatcher(TimeoutThread timeoutThread) {
        // create selector
        try {
            this.timeoutThread = timeoutThread; 
            selector = Selector.open();
        } catch (IOException ex) {
            System.out.println("Cannot create selector.");
            ex.printStackTrace();
            System.exit(1);
        } // end of catch
    } // end of Dispatcher

    public Selector selector() {
        return selector;
    }
    /*
     * public SelectionKey registerNewSelection(SelectableChannel channel,
     * IChannelHandler handler, int ops) throws ClosedChannelException {
     * SelectionKey key = channel.register(selector, ops); key.attach(handler);
     * return key; } // end of registerNewChannel
     *
     * public SelectionKey keyFor(SelectableChannel channel) { return
     * channel.keyFor(selector); }
     *
     * public void deregisterSelection(SelectionKey key) throws IOException {
     * key.cancel(); }
     *
     * public void updateInterests(SelectionKey sk, int newOps) {
     * sk.interestOps(newOps); }
     */

    public void run() {
        while (!Thread.interrupted()) {
            DEBUG("Enter Dispatcher Selection Loop");
            try {
                // check to see if any events
                selector.select();
                if (Thread.interrupted()) {
                    return; 
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }

            // readKeys is a set of ready events
            Set<SelectionKey> readyKeys = selector.selectedKeys();

            // create an iterator for the set
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            // iterate over all events
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    // a new connection is ready to be accepted
                    if (key.isAcceptable()) {
                        AcceptHandler aH = (AcceptHandler) key.attachment();
                        aH.handleAccept(key);
                    }
                    // if the key is readable or writeable create a read write handler
                    if (key.isReadable() || key.isWritable()) {
                        ReadWriteHandler rwH = (ReadWriteHandler) key.attachment();
                        // let the timeout thread know that the channel on this key has gotten a request
                        timeoutThread.removeSelectionKey(rwH.getSelectionHashKey());
                        if (key.isReadable()) {
                            rwH.handleRead(key);
                        } // end of if isReadable
                        if (key.isWritable()) {
                            rwH.handleWrite(key);
                        } // end of if isWritable
                    } // end of readwrite
                } catch (IOException ex) {
                    DEBUG("Exception when handling key " + key);
                    key.cancel();
                    try {
                        key.channel().close();
                        // in a more general design, call have a handleException
                    } catch (IOException cex) {
                    }
                } // end of catch

            } // end of while (iterator.hasNext()) {

        } // end of while (true)
    } // end of run

    private static void DEBUG(String s) {
        if (debug) {
            System.out.println(s);
        }
    }
}