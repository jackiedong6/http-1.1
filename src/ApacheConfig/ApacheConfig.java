package ApacheConfig;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ApacheConfig {
    private final String configFile;
    private int serverPort;
    private int numSelectLoops;
    private  static List<VirtualHost> virtualHosts = new ArrayList<>();

    public ApacheConfig(String configFile) {
        this.configFile = configFile;
    }

    public String getConfigFile() {
        return configFile;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getNumSelectLoops() {
        return numSelectLoops;
    }

    public List<VirtualHost> getVirtualHosts() {
        return virtualHosts;
    }


    public void parse() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            String line;
            String documentRoot = null;
            String serverName = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("nSelectLoops")) {
                    numSelectLoops = Integer.parseInt(line.split("\\s+")[1]);
                }
                if (line.startsWith("Listen")) {
                    serverPort = Integer.parseInt(line.split("\\s+")[1]);
                }
                if (line.startsWith("DocumentRoot")) {
                    documentRoot = line.split("\\s+")[1];
                }
                if (line.startsWith("ServerName")) {
                    serverName = line.split("\\s+")[1];
                }
                if (serverName != null && documentRoot != null) {
                    virtualHosts.add(new VirtualHost(serverName, documentRoot));
                    serverName = null;
                    documentRoot = null;
                }
            }
            reader.close();
        } catch (FileNotFoundException ignored) {
            System.out.println("Configuration file not found");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




}