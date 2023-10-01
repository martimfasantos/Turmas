package pt.ulisboa.tecnico.classes.admin;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class AdminFrontend {

    // error format
    private final String _error_fmt = "ERROR: %s\n";

    // error messages
    private final String SERVER_UNREACHABLE = "Unreachable server... Try again...";


    // grpc
    private static ManagedChannel _nameServerChannel;
    private static NamingServerServiceGrpc.NamingServerServiceBlockingStub _nameServerStub;

    private static ManagedChannel _channel;
    private static AdminServiceGrpc.AdminServiceBlockingStub _stub;

    /**
     * Default Constructor
     */
    public AdminFrontend() {
        String host = "localhost";
        int port = 5000;

        _nameServerChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        _nameServerStub = NamingServerServiceGrpc.newBlockingStub(_nameServerChannel);
    }

    /**
     * Update stub to communicate with server with qualifier given as parameter
     *
     * @param host
     * @param port
     */
    private void updateStub(String host, Integer port) {
        try {
            if (_channel != null) {
                _channel.shutdownNow();
            }

            _channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            _stub = AdminServiceGrpc.newBlockingStub(_channel);
        }
        catch (Exception e){
            System.err.println(e.getCause());
        }
    }

    /**
     * Returns a list of all the servers in the naming server
     * @return
     */
    private List<String> getServers() {
        List<String> qualifiers = new ArrayList<String>();
        qualifiers.add("P");
        qualifiers.add("S");

        LookupRequest request = LookupRequest.newBuilder().setServiceName("Turmas").addAllQualifiers(qualifiers).build();
        LookupResponse response;

        try {
            response = _nameServerStub.lookup(request);
        } catch (StatusRuntimeException e) {
            System.err.printf(_error_fmt, "Unreachable naming server... Try again...");
            return new ArrayList<>();
        }

        return response.getAddressList();

    }



    /**
     * Activates server enabling it to receive requests from clients
     *
     * @param host
     * @param port
     */
    public void activate(String host, Integer port) {
        updateStub(host, port);

        final ActivateRequest request = ActivateRequest.newBuilder().build();
        final ActivateResponse response;

        // Send request and catch exceptions
        try {
            response = _stub.activate(request);
        } catch (StatusRuntimeException e) {
            // If server unreachable, call lookup on naming server to get other running server with given qualifier
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        // Displaying to user according to response
        System.out.println(Stringify.format(response.getCode()));
    }

    /**
     * Deactivates server suspending it from responding requests from clients
     *
     * @param host
     * @param port
     */
    public void deactivate(String host, Integer port) {
        updateStub(host, port);

        final DeactivateRequest request = DeactivateRequest.newBuilder().build();
        final DeactivateResponse response;

        // Send request and catch exceptions
        try {
            response = _stub.deactivate(request);
        } catch (StatusRuntimeException e) {
            // If server unreachable, call lookup on naming server to get other running server with given qualifier
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        // Displaying to user according to response
        System.out.println(Stringify.format(response.getCode()));
    }

    /**
     * Lists the state of the server received as argument, displaying the list of
     * enrolled students, discarded students, class capacity and the state of the
     * enrollments
     *
     * @param host
     * @param port
     */
    public void dump(String host, Integer port) {
        updateStub(host, port);

        final DumpRequest request = DumpRequest.newBuilder().build();
        final DumpResponse response;

        // Send request and catch exceptions
        try {
            response = _stub.dump(request);
        } catch (StatusRuntimeException e) {
            // If server unreachable, call lookup on naming server to get other running server with given qualifier
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        // Display to user according to response
        if (response.getCode() == ClassesDefinitions.ResponseCode.OK) {
            System.out.println(Stringify.format(response.getClassState()));
        } else {
            System.out.println(Stringify.format(response.getCode()));
        }
    }

    /**
     * Activates propagation of state
     */
    public void activateGossip() {

        List<String> servers = getServers();

        for(String server: servers) {
            String[] address = server.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);

            updateStub(host, port);

            ActivateGossipRequest request = ActivateGossipRequest.newBuilder().build();
            ActivateGossipResponse response;

            try {
                response = _stub.activateGossip(request);
            } catch (StatusRuntimeException e) {
                // If server unreachable, call lookup on naming server to get other running server with given qualifier
                System.err.printf(_error_fmt, SERVER_UNREACHABLE);
                return;
            }

            // Displaying to user according to response
            System.out.println(Stringify.format(response.getCode()));
        }

    }

    /**
     * Deactivates propagation of state
     */
    public void deactivateGossip() {
        List<String> servers = getServers();

        for(String server: servers) {
            String[] address = server.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);

            updateStub(host, port);

            DeactivateGossipRequest request = DeactivateGossipRequest.newBuilder().build();
            DeactivateGossipResponse response;

            try {
                response = _stub.deactivateGossip(request);
            } catch (StatusRuntimeException e) {
                // If server unreachable, call lookup on naming server to get other running server with given qualifier
                System.err.printf(_error_fmt, SERVER_UNREACHABLE);
                return;
            }

            // Displaying to user according to response
            System.out.println(Stringify.format(response.getCode()));
        }
    }

    /**
     * Forces propagation of state from server to the other replicas
     * @param host
     * @param port
     */
    public void gossip(String host, Integer port) {
        updateStub(host, port);

        final GossipRequest request = GossipRequest.newBuilder().build();
        final GossipResponse response;

        // Send request and catch exceptions
        try {
            response = _stub.gossip(request);
        } catch (StatusRuntimeException e) {
            // If server unreachable, call lookup on naming server to get other running server with given qualifier
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        // Displaying to user according to response
        System.out.println(Stringify.format(response.getCode()));
    }

    /**
     * Closes channel that communicates with server
     */
    public void exit() {
        _channel.shutdown();
    }
}
