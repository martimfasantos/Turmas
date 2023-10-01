package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.Clock;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.DeleteRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterRequest;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class ClassServerFrontend {

    // Error format
    private final String _error_fmt = "ERROR: %s\n";

    // Error messages
    private final String NAMING_SERVER_UNREACHABLE = "Unreachable naming server...";
    private final String FAILED_TO_PROPAGATE_STATE = "Failed to propagate state...";

    private static NamingServerServiceGrpc.NamingServerServiceBlockingStub _stub;
    private static ManagedChannel _channel;

    private String _host;
    private int _port;
    private String _qualifier;

    /**
     * Create channel and stub to call remote services on naming server
     */
    public ClassServerFrontend() {
        // Naming server address
        String host = "localhost";
        int port = 5000;
        // Grpc
        _channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        _stub = NamingServerServiceGrpc.newBlockingStub(_channel);
    }

    /**
     * Register server on naming server
     *
     * @param service
     * @param qualifier
     * @param host
     * @param port
     */
    public void register(String service, String qualifier, String host, int port) {
        ClassServer.debug("SERVER: Registered in Naming Server");
        this._host = host;
        this._port = port;
        this._qualifier = qualifier;

        List<String> qualifiers = new ArrayList<>();
        qualifiers.add(qualifier);

        final RegisterRequest request = RegisterRequest.newBuilder()
                .setServiceName(service)
                .setAddress(host + ":" + port)
                .addAllQualifiers(qualifiers)
                .build();

        // Send request and catch exception if server is unreachable
        try {
            _stub.register(request);
        } catch (StatusRuntimeException e) {
            System.err.printf(_error_fmt, NAMING_SERVER_UNREACHABLE);
        }
    }

    /**
     * Propagates state to the secondary servers
     *
     * @param classState
     */
    public void propagateState(ClassState classState, VectorClock vectorClock) {
        if (!ClassServer.isActive()) {
            return;
        }

        ClassServer.debug("SERVER: Propagating State...");

        final List<String> qualifiers = new ArrayList<>();
        qualifiers.add("S");
        qualifiers.add("P");

        final LookupRequest requestL = LookupRequest.newBuilder().setServiceName("Turmas").addAllQualifiers(qualifiers)
                .build();
        final LookupResponse responseL;

        try {
            responseL = _stub.lookup(requestL);
        } catch (StatusRuntimeException e) {
            System.err.printf(_error_fmt, FAILED_TO_PROPAGATE_STATE);
            return;
        }

        Clock ownClock = Clock.newBuilder().setHost(_host).setPort(_port).setValue(vectorClock.getClock(_host, _port)).build();
        final PropagateStateRequest request = PropagateStateRequest.newBuilder().setClassState(classState)
                .setClock(ownClock).setQualifier(_qualifier).build();

        // Get all secondary servers
        List<String> serversList = responseL.getAddressList().stream().toList();

        // Propagate state to every other server
        for (String server : serversList) {

            String[] address = server.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);
            if (host.equals(this._host) && port == this._port) {
                continue;
            }

            if (!vectorClock.contains(server)) {
                vectorClock.updateClock(server, 0);
            }


            // Setup channel and stub to send propagate state request
            ManagedChannel _auxChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            ClassServerServiceGrpc.ClassServerServiceBlockingStub stub = ClassServerServiceGrpc
                    .newBlockingStub(_auxChannel);

            PropagateStateResponse response = stub.propagateState(request);

            _auxChannel.shutdown();

            if (response.getCode() == ResponseCode.OK) {
                ClassServer.debug(String.format("SERVER: Propagated to %s:%s successfully", host, port));
            }
        }
    }

    /**
     * Calls delete service on naming server
     *
     * @param service
     * @param host
     * @param port
     * @return
     */
    public boolean exit(String service, String host, int port) {

        final DeleteRequest request = DeleteRequest.newBuilder()
                .setServiceName(service)
                .setAddress(host + ":" + port)
                .build();

        boolean success;

        // Send request and catch exception if server is unreachable
        try {
            _stub.delete(request);
            success = true;
        } catch (StatusRuntimeException e) {
            System.err.printf(_error_fmt, NAMING_SERVER_UNREACHABLE);
            success = false;
        } finally {
            _channel.shutdown();
        }

        return success;
    }
}
