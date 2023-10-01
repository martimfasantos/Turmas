package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import static io.grpc.Status.Code.INVALID_ARGUMENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc.ProfessorServiceBlockingStub;

public class ProfessorFrontend {

    // error format
    final String _error_fmt = "ERROR: %s\n";

    // error messages
    private final String SERVER_UNREACHABLE = "Unreachable server... Try again...";

    // grpc
    private ManagedChannel _channel;
    private final NamingServerServiceBlockingStub _stubNaming;
    private ProfessorServiceGrpc.ProfessorServiceBlockingStub _stubP;
    private ProfessorServiceGrpc.ProfessorServiceBlockingStub _stubS;

    // grpc randomness
    private double probabilityP = 0.5;

    private enum classServerType {
        P, S
    }

    /**
     * Create channel and stub to call remote services on server
     * 
     * @param host
     * @param port
     */
    public ProfessorFrontend(String host, int port) {
        _channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        _stubNaming = NamingServerServiceGrpc.newBlockingStub(_channel);
        updateStub(classServerType.P);
        updateStub(classServerType.S);
    }

    /**
     * Opens the enrollments for the class, with a certain maximum capacity
     * 
     * @param capacity
     */
    public void openEnrollmentsRequest(int capacity) {
        final OpenEnrollmentsRequest request = OpenEnrollmentsRequest.newBuilder().setCapacity(capacity).build();
        final OpenEnrollmentsResponse response;

        // Send Request and Receive Response Try-Catch Block
        try {
            response = generateStub(classServerType.P).openEnrollments(request);

            // Outdated stub, update and retry
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == INVALID_ARGUMENT) {
                System.err.printf(_error_fmt, e.getMessage());
                return;
            }
            updateStub(classServerType.P);
            openEnrollmentsRequest(capacity);
            return;
            // No server is available
        } catch (NullPointerException e) {
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        // Displaying to user according to response
        System.out.println(Stringify.format(response.getCode()));
    }

    /**
     * Closes the class enrollments
     */
    public void closeEnrollmentsRequest() {
        final CloseEnrollmentsRequest request = CloseEnrollmentsRequest.newBuilder().build();
        final CloseEnrollmentsResponse response;

        // Send Request and Receive Response Try-Catch Block
        try {
            ProfessorServiceBlockingStub stub = generateStub(classServerType.P);
            response = stub.closeEnrollments(request);

            // Outdated stub, update and retry
        } catch (StatusRuntimeException e) {
            updateStub(classServerType.P);
            closeEnrollmentsRequest();
            return;
        } catch (NullPointerException e) {
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        // Displaying to user according to response
        System.out.println(Stringify.format(response.getCode()));
    }

    /**
     * Lists the state of the enrollments, displaying the list of enrolled students,
     * the list of canceled enrollments and the class' total capacity
     */
    public void listClass() {
        final ListClassRequest request = ListClassRequest.newBuilder().build();
        ListClassResponse response;

        ProfessorServiceBlockingStub stub = generateStub(classServerType.S);

        // Send Request and Receive Response Try-Catch Block
        try {

            response = stub.listClass(request);

            // Outdated stub, update and retry
        } catch (StatusRuntimeException e) {
            updateStub(classServerType.P);
            updateStub(classServerType.S);
            listClass();
            return;
        } catch (NullPointerException e) {
            System.out.println("1");
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        // Display to user according to response
        if (response.getCode() == ResponseCode.OK) {
            System.out.println(Stringify.format(response.getClassState()));
        }
        // Inactive Server Case
        else {

            // Retrying on the other stub
            try {
                stub = (stub == _stubP) ? _stubS : _stubP;

                response = stub.listClass(request);

                // Outdated stub, update and retry
            } catch (StatusRuntimeException e) {
                updateStub(classServerType.P);
                updateStub(classServerType.S);
                listClass();
                return;
            } catch (NullPointerException e) {
                System.out.println(Stringify.format(ResponseCode.INACTIVE_SERVER));
                return;
            }

            if (response.getCode() == ResponseCode.OK) {
                System.out.println(Stringify.format(response.getClassState()));
                // Both servers are inactive
            } else {
                System.out.println(Stringify.format(response.getCode()));
            }
        }
    }

    /**
     * Receives the student's id and the correspondent enrollment in the class
     * Also adds the student to the canceled enrollments list
     * 
     * @param student_id
     */
    public void cancelEnrollment(String student_id) {
        final CancelEnrollmentRequest request = CancelEnrollmentRequest.newBuilder().setStudentId(student_id).build();
        CancelEnrollmentResponse response;

        ProfessorServiceBlockingStub stub = generateStub(classServerType.S);

        // Send Request and Receive Response Try-Catch Block
        try {
            response = stub.cancelEnrollment(request);

            // Outdated stub, update and retry
        } catch (StatusRuntimeException e) {
            updateStub(classServerType.P);
            updateStub(classServerType.S);
            cancelEnrollment(student_id);
            return;
            // No server is available
        } catch (NullPointerException e) {
            System.err.printf(_error_fmt, SERVER_UNREACHABLE);
            return;
        }

        if (!(response.getCode() == ResponseCode.INACTIVE_SERVER)) {
            System.out.println(Stringify.format(response.getCode()));
        }
        // Inactive Server Case
        else {

            // Retrying on the other stub
            try {
                stub = (stub == _stubP) ? _stubS : _stubP;

                response = stub.cancelEnrollment(request);

                // Outdated stub, update and retry
            } catch (StatusRuntimeException e) {
                updateStub(classServerType.P);
                updateStub(classServerType.S);
                cancelEnrollment(student_id);
                return;
            } catch (NullPointerException e) {
                System.out.println(Stringify.format(ResponseCode.INACTIVE_SERVER));
                return;
            }

            System.out.println(Stringify.format(response.getCode()));
        }
    }

    /**
     * Exits the frontend
     */
    public void exit() {
        _channel.shutdownNow();
    }

    /**
     * Updates the corresponding stub
     * 
     * @param type
     */
    private void updateStub(classServerType type) {
        final List<String> qualifiers = new ArrayList<>();

        if (type == classServerType.P) {
            qualifiers.add("P");
        } else {
            qualifiers.add("S");
        }

        final LookupRequest request = LookupRequest.newBuilder().setServiceName("Turmas").addAllQualifiers(qualifiers)
                .build();
        final LookupResponse response;

        try {
            response = _stubNaming.lookup(request);
        } catch (StatusRuntimeException e) {
            System.err.printf(_error_fmt, "Unreachable naming server...");
            return;
        }

        List<String> serverList = response.getAddressList().stream().toList();

        if (serverList.size() != 0) {
            String server = serverList.get(new Random().nextInt(serverList.size()));
            String[] address = server.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);

            _channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

            if (type == classServerType.P) {
                _stubP = ProfessorServiceGrpc.newBlockingStub(_channel);
            } else {
                _stubS = ProfessorServiceGrpc.newBlockingStub(_channel);
            }

            // If empty list:
        } else if (type == classServerType.P) {
            _stubP = null;
        } else {
            _stubS = null;
        }
    }

    /**
     * Generates a primary or secondary server stub non-deterministically
     * Updates probabilityP variable dynamically according to number of reads and
     * writes
     * 
     * @param type
     * @return
     */
    private ProfessorServiceBlockingStub generateStub(classServerType type) {
        if (type == classServerType.P) {
            probabilityP = Math.min(probabilityP + 0.25, 1);
            return _stubP;

        } else if (type == classServerType.S) {
            probabilityP = Math.max(0, probabilityP - 0.15);

            // Reconnect to stubs if they are down
            if (_stubP == null) {
                updateStub(classServerType.P);
            }
            if (_stubS == null) {
                updateStub(classServerType.S);
            }

            // Choice = P
            if (Math.random() < probabilityP) {
                if (_stubP == null && _stubS != null) {
                    return _stubS;
                } else {
                    return _stubP;
                }
            }
            // Choice = S
            else {
                if (_stubS == null) {
                    return _stubP;
                } else {
                    return _stubS;
                }
            }

        } else /* ERROR */ {
            return _stubP;
        }
    }
}
