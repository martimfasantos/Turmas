package pt.ulisboa.tecnico.classes.namingserver;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.DeleteRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.DeleteResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterResponse;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc.NamingServerServiceImplBase;

public class NamingServerServiceImpl extends NamingServerServiceImplBase {
    final private NamingServices _namingServices = new NamingServices();

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        String serviceName = request.getServiceName();
        String[] address = request.getAddress().split(":");
        List<String> qualifiers = request.getQualifiersList();

        String host = address[0];
        Integer port = Integer.parseInt(address[1]);


        // Add server to naming server
        _namingServices.addServer(serviceName, host, port, qualifiers);

        NamingServer.debug(String.format("Registered server with address %s:%d with qualifiers %s", host, port, qualifiers));

        // Build and send response
        responseObserver.onNext(RegisterResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {

        String service = request.getServiceName();
        List<String> addresses = new ArrayList<>();
        LookupResponse response;

        NamingServer.debug(String.format("Lookup requested for service %s", service));

        // If service exists look for servers with qualifiers
        if (_namingServices.serviceExists(service)) {
            List<String> qualifiers = request.getQualifiersList();

            // If no qualifiers were given, return servers with any qualifier
            if (qualifiers.size() == 0) {
                qualifiers.add("P");
                qualifiers.add("S");
            }

            ServiceEntry serviceEntry = _namingServices.getServiceEntry(service);

            if  (serviceEntry != null) {
                for (String qualifier : qualifiers) {
                    List<ServerEntry> servers = serviceEntry.getServerEntriesWithQualifiers(qualifier);
                    for (ServerEntry server : servers) {
                        // Add server address to list
                        addresses.add(server.getHost() + ":" + server.getPort());
                    }
                }
            }
        }

        response = LookupResponse.newBuilder().addAllAddress(addresses).build();

        // Build and send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        String service = request.getServiceName();
        String[] address = request.getAddress().split(":");

        String host = address[0];
        Integer port = Integer.parseInt(address[1]);

        _namingServices.getServiceEntry(service).removeServerEntry(host, port);

        NamingServer.debug(String.format("Removed server with address %s:%d", host, port));

        // Build and send response
        responseObserver.onNext(DeleteResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
