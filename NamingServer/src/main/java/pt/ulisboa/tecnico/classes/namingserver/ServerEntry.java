package pt.ulisboa.tecnico.classes.namingserver;

import java.util.List;

public class ServerEntry {
    private final String _host;
    private final Integer _port;
    private final List<String> _qualifiers;

    ServerEntry(String host, Integer port, List<String> qualifiers) {
        _host = host;
        _port = port;
        _qualifiers = qualifiers;
    }

    public String getHost() {
        return _host;
    }

    public Integer getPort() {
        return _port;
    }

    public List<String> getQualifiers() {
        return _qualifiers;
    }

}