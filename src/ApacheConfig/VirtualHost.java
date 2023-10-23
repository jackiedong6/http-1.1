package ApacheConfig;

public class VirtualHost {
    String serverName;
    String documentRoot;

    public VirtualHost(String serverName, String documentRoot) {
        this.serverName = serverName;
        this.documentRoot = documentRoot;

    }

    public String getServerName() {
        return this.serverName;
    }
    public String getDocumentRoot() {
        return this.documentRoot;
    }
}
