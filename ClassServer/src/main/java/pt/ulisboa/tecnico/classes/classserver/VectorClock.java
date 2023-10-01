package pt.ulisboa.tecnico.classes.classserver;

import java.util.concurrent.ConcurrentHashMap;

public class VectorClock {

    private final ConcurrentHashMap<String, Integer> _clocks = new ConcurrentHashMap<>();

    private final String _server;

    public VectorClock(String host, Integer port) {
        this._server = host + ":" + port;
        updateClock(host, port, 0);
    }

    public synchronized void updateClock(String host, Integer port, Integer clock) {
        String server = host + ":" + port;
        this._clocks.put(server, clock);
    }

    public synchronized void updateClock(String server, Integer clock) {
        this._clocks.put(server, clock);
    }

    public Integer getClock(String host, Integer port) {
        String server = host + ":" + port;
        if (!_clocks.containsKey(server)) {
            return 0;
        }
        return this._clocks.get(server);
    }

    public synchronized void incrementClock() {
        updateClock(_server, this._clocks.get(_server) + 1);
    }


    public boolean contains(String server) {
        return this._clocks.containsKey(server);
    }

}
