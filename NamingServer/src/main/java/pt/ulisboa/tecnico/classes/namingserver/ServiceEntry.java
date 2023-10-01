package pt.ulisboa.tecnico.classes.namingserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceEntry {
    private final Set<ServerEntry> _serverEntries = new HashSet<ServerEntry>();

    ServiceEntry() {
    }

    /**
     * Adds a server
     * 
     * @param entry
     */
    public void addServerEntry(ServerEntry entry) {
        _serverEntries.add(entry);
    }

    /**
     * Removes a server
     */
    public void removeServerEntry(String host, Integer port) {
        _serverEntries.removeIf(server -> (server.getHost().equals(host)) && (server.getPort().equals(port)));
    }

    /**
     * Returns all the servers with given qualifier(s)
     * 
     * @param qualifier
     * @return
     */
    public List<ServerEntry> getServerEntriesWithQualifiers(String qualifier) {
        List<ServerEntry> servers = new ArrayList<ServerEntry>();

        for (ServerEntry server : _serverEntries) {
            if (server.getQualifiers().contains(qualifier)) {
                servers.add(server);
            }
        }
        return servers;
    }
}
