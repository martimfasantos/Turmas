package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc.StudentServiceBlockingStub;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StudentFrontend {

    // error format
    static final String _error_fmt = "ERROR: %s\n";

    // error messages
    private static final String SERVER_UNREACHABLE = "Unreachable server... Try again...";

    // grpc
    private ManagedChannel _channel;
    private final NamingServerServiceBlockingStub _stubNaming;
    private StudentServiceBlockingStub _stubP;
    private StudentServiceBlockingStub _stubS;

    // grpc randomness
    private double probabilityP = 0.3;

    private enum classServerType {
        P, S
    }

    /**
     * Create channel and stub to call connect to NamingServer
     *
     * @param host
     * @param port
     */
    public StudentFrontend(String host, int port) {
        _channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        _stubNaming = NamingServerServiceGrpc.newBlockingStub(_channel);
        updateStub(classServerType.P);
        updateStub(classServerType.S);
    }

    /**
     * Lists the state of the enrollments, displaying the list of enrolled students,
     * the list of canceled enrollments and the class' total capacity
     */
    public void listClass() {
        final ListClassRequest request = ListClassRequest.newBuilder().build();
        ListClassResponse response;

        StudentServiceBlockingStub stub = generateStub(classServerType.S);

        // Send Request and Receive Response Try-Catch Block
        try {
            response = stub.listClass(request);

            // Outdated stub, update and retry
        } catch (StatusRuntimeException e) {
            updateStub(classServerType.P);
            updateStub(classServerType.S);
            listClass();
            return;
            // No server is available
        } catch (NullPointerException e) {
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
     * Enrolls the student in the class
     */
    public void enroll(String id, String name) {
        final Student student = Student.newBuilder().setStudentId(id).setStudentName(name).build();
        final EnrollRequest request = EnrollRequest.newBuilder().setStudent(student).build();
        EnrollResponse response;

        StudentServiceBlockingStub stub = generateStub(classServerType.S);

        // Send Request and Receive Response Try-Catch Block
        try {
            response = stub.enroll(request);

            // Outdated stub, update and retry
        } catch (StatusRuntimeException e) {
            updateStub(classServerType.P);
            updateStub(classServerType.S);
            enroll(id, name);
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

                response = stub.enroll(request);

                // Outdated stub, update and retry
            } catch (StatusRuntimeException e) {
                updateStub(classServerType.P);
                updateStub(classServerType.S);
                enroll(id, name);
                return;
            } catch (NullPointerException e) {
                System.out.println(Stringify.format(ResponseCode.INACTIVE_SERVER));
                return;
            }

            System.out.println(Stringify.format(response.getCode()));
        }
    }

    /**
     * Closes the channel
     */
    public void exit() {
        _channel.shutdownNow();
    }

    /**
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
                // System.out.println(host + ":" + port);
                _stubP = StudentServiceGrpc.newBlockingStub(_channel);
            } else if (type == classServerType.S) {
                // System.out.println(host + ":" + port);
                _stubS = StudentServiceGrpc.newBlockingStub(_channel);
            } else {
                System.err.printf(_error_fmt, "Unknown error");
            }

            // If empty list:
        } else if (type == classServerType.P) {
            // System.out.println("NULL P");
            _stubP = null;
        } else if (type == classServerType.S) {
            // System.out.println("NULL S");
            _stubS = null;
        } else {
            System.err.printf(_error_fmt, "Unknown error");
        }
    }

    /**
     * Generates a primary or secondary server stub non-deterministically
     * Updates probabilityP variable dynamically according to number of reads and
     * writes
     * 
     * @param serverType
     * @return
     */
    private StudentServiceBlockingStub generateStub(classServerType serverType) {
        if (serverType == classServerType.P) {
            probabilityP = Math.min(probabilityP + 0.25, 1);
            return _stubP;

        } else if (serverType == classServerType.S) {
            probabilityP = Math.max(0, probabilityP - 0.15);

            // Reconnect to stubs if they are down
            if (_stubP == null)
                updateStub(classServerType.P);
            if (_stubS == null)
                updateStub(classServerType.S);

            // Choice = P
            if (Math.random() < probabilityP) {
                if (_stubP == null && _stubS != null) {
                    return _stubS;
                } else
                    return _stubP;
            }
            // Choice = S
            else {
                if (_stubS == null)
                    return _stubP;
                else
                    return _stubS;
            }

        } else /* ERROR */ {
            return _stubP;
        }
    }
}
