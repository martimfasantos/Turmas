package pt.ulisboa.tecnico.classes.classserver;

import java.util.regex.Pattern;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    final private Class _class;

    private final VectorClock _vectorClock;

    public StudentServiceImpl(Class _class, VectorClock vectorClock) {
        this._class = _class;
        this._vectorClock = vectorClock;
    }

    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
        ClassServer.debug(String.format("STUDENT: Enroll Request | Args: %s", request.getStudent().getStudentId()));

        final String student_id = request.getStudent().getStudentId();
        final String student_name = request.getStudent().getStudentName();

        if (!Pattern.compile("aluno\\d{4}").matcher(student_id).matches()) {
            responseObserver
                    .onError(INVALID_ARGUMENT.withDescription("Invalid student ID").asRuntimeException());
            ClassServer.debug("STUDENT: Last Enroll Request failed, invalid student ID");
            return;
        }

        else if (student_name.length() < 3 || student_name.length() > 30) {
            responseObserver
                    .onError(INVALID_ARGUMENT.withDescription("Invalid student name").asRuntimeException());
            ClassServer.debug("STUDENT: Last Enroll Request failed, invalid student name");
            return;
        }

        final EnrollResponse.Builder response = EnrollResponse.newBuilder();
        if (!ClassServer.isActive()) {
            response.setCode(ResponseCode.INACTIVE_SERVER);
        } else {
            synchronized (_class) {
                if (!_class.getOpenEnrollments()) {
                    response.setCode(ResponseCode.ENROLLMENTS_ALREADY_CLOSED);
                } else if (_class.isEnrolled(request.getStudent().getStudentId())) {
                    response.setCode(ResponseCode.STUDENT_ALREADY_ENROLLED);
                } else if (_class.isFull()) {
                    response.setCode(ResponseCode.FULL_CLASS);
                } else {
                    // Register student with given parameters
                    _class.enroll(student_id, student_name);

                    // Increment vector clock
                    _vectorClock.incrementClock();

                    response.setCode(ResponseCode.OK);
                }
            }
        }

        // Send response
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {
        ClassServer.debug("STUDENT: List Request");

        final ListClassResponse.Builder response = ListClassResponse.newBuilder();

        if (!ClassServer.isActive()) {
            response.setCode(ResponseCode.INACTIVE_SERVER);
        } else {
            response.setCode(ResponseCode.OK).setClassState(_class.createClassState());
        }

        // Send response
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}