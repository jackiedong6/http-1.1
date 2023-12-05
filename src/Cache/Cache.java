package Cache;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

    public static class CachedContent {
        public ByteBuffer content;
        public long lastModified;

        public CachedContent(ByteBuffer content, long lastModified) {
            this.content = content;
            this.lastModified = lastModified;

        }
    }

    private ConcurrentHashMap<String, CachedContent> cache = new ConcurrentHashMap<>();

    public void put(String uri, ByteBuffer content, long lastModified) {
        cache.put(uri, new CachedContent(content, lastModified));
    }

    public CachedContent get(String uri) {
        return cache.get(uri);
    }
}
