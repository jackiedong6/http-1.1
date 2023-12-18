package Timeout;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

public class TimeoutThread extends Thread {
    public Random randNumGenerator; 
    public ConcurrentHashMap<Integer, SelectionKey> selectionKeys;
    public ConcurrentHashMap<Integer, Long> timestamps; 
    private static boolean debug = false; 

    public TimeoutThread() {
        selectionKeys = new ConcurrentHashMap<>();
        timestamps = new ConcurrentHashMap<>(); 
        randNumGenerator = new Random();
    }

    /*
     * 
     * Note: possible improvement would be to have a lock per entry (per SelectionKey) and only
     * hold the lock when checking that SelectionKey's timestamp. This would allow for SelectionKeys 
     * to be removed during one iteration of this process if there is a new request for a given SelectionKey
     * 
     */
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(1000); // check timeouts every second
            } catch (InterruptedException e) {
                return; 
            }
            Iterator<Map.Entry<Integer, Long>> iterator = timestamps.entrySet().iterator();
            long currTime = Instant.now().getEpochSecond(); 
            while (iterator.hasNext()) {
                Map.Entry<Integer, Long> entry = iterator.next();
                int currKey = entry.getKey();
                long currTimestamp = entry.getValue();
                if (currTime - currTimestamp > 3.0) {
                    DEBUG("Delay: " + (currTime - currTimestamp));
                    // close the connection 
                    try {
                        SelectionKey currSelectionKey = selectionKeys.get(currKey);
                        currSelectionKey.channel().close();
                        currSelectionKey.cancel();
                        // remove this key from the map since it has already been disconnected
                        this.removeSelectionKey(currKey);
                    } catch (IOException e) {
                        // handle exception, log it
                    }
                } // otherwise, leave the SelectionKey in the map 
            }
        }
    }

    /*
     * Mark a timestamp for a given selection key. This timestamp represents the instant 
     * when this channel was created.
     */
    public int addSelectionKeyTimestamp(SelectionKey key, long timestamp) {
        int hashkey = key.hashCode(); 
        // get a unique random number
        while (selectionKeys.containsKey(hashkey)) {
            hashkey = randNumGenerator.nextInt(Integer.MAX_VALUE);
        }
        // store the key and timestamp in the hash map
        selectionKeys.put(hashkey, key); 
        timestamps.put(hashkey, timestamp); 
        return hashkey;
    }

    /*
     * Remove a SelectionKey from the map. This represents when a request has been received with 
     * the key corresponding to the given hashkey
     */
    public void removeSelectionKey(int hashkey) {
        DEBUG("Removing selection key");
        if (hashkey >= 0) {
            selectionKeys.remove(hashkey);
            timestamps.remove(hashkey);
        }
    }

    private static void DEBUG(String s) {
        if (debug) {
            System.out.println(s);
        }
    }
}
