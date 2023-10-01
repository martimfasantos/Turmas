package pt.ulisboa.tecnico.classes.namingserver;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NamingServices {
    private final ConcurrentHashMap<String, ServiceEntry> _serviceEntries = new ConcurrentHashMap<String, ServiceEntry>();

    /**
     * Add server to list of servers of the given service
     * If service does not exist create it and add server to it's list
     *
     * @param serviceName
     * @param host
     * @param port
     * @param qualifiers
     */
    public synchronized void addServer(String serviceName, String host, Integer port, List<String> qualifiers) {
        ServiceEntry serviceEntry;

        if (_serviceEntries.containsKey(serviceName)) {
            serviceEntry = _serviceEntries.get(serviceName);
        } else {
            serviceEntry = new ServiceEntry();
            _serviceEntries.put(serviceName, serviceEntry);
        }

        ServerEntry serverEntry = new ServerEntry(host, port, qualifiers);
        serviceEntry.addServerEntry(serverEntry);
    }

    public ServiceEntry getServiceEntry(String service) {
        return _serviceEntries.get(service);
    }

    public boolean serviceExists(String service) {
        return _serviceEntries.containsKey(service);
    }

}
