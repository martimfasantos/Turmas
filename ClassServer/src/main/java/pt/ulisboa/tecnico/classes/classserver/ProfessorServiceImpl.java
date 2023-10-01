package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc.ProfessorServiceImplBase;

import java.util.regex.Pattern;

import static io.grpc.Status.INVALID_ARGUMENT;

public class ProfessorServiceImpl extends ProfessorServiceImplBase {
    final private Class _class;

    private final VectorClock _vectorClock;

    public ProfessorServiceImpl(Class _class, VectorClock vectorClock) {
        this._class = _class;
        this._vectorClock = vectorClock;
    }

    @Override
    public void openEnrollments(OpenEnrollmentsRequest request,
                                StreamObserver<OpenEnrollmentsResponse> responseObserver) {
        ClassServer.debug(String.format("PROFESSOR: Open Enrollments Request | Args: capacity %d",
                request.getCapacity()));

        final int capacity = request.getCapacity();
        if (capacity < 0) {
            responseObserver
                    .onError(INVALID_ARGUMENT.withDescription("Invalid class capacity").asRuntimeException());
            ClassServer.debug("PROFESSOR: Last Open Enroll Request failed, invalid class capacity");
            return;
        }

        final OpenEnrollmentsResponse.Builder response = OpenEnrollmentsResponse.newBuilder();

        if (!ClassServer.isActive()) {
            response.setCode(ResponseCode.INACTIVE_SERVER);
        } else {
            synchronized (_class) {
                // If class is already open, notify client and don't change its capacity
                if (_class.getOpenEnrollments()) {
                    response.setCode(ResponseCode.ENROLLMENTS_ALREADY_OPENED).build();
                } else if (_class.getClassSize() >= request.getCapacity()) {
                    response.setCode(ResponseCode.FULL_CLASS).build();
                } else {
                    _class.openEnrollments(capacity);
                    // Increment vector clock
                    _vectorClock.incrementClock();

                    response.setCode(ResponseCode.OK).build();
                }
            }

        }

        // Send Response
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void closeEnrollments(CloseEnrollmentsRequest request,
                                 StreamObserver<CloseEnrollmentsResponse> responseObserver) {
        ClassServer.debug("PROFESSOR: Close Enrollments Request");

        final CloseEnrollmentsResponse.Builder response = CloseEnrollmentsResponse.newBuilder();

        if (!ClassServer.isActive()) {
            response.setCode(ResponseCode.INACTIVE_SERVER);
        } else {
            synchronized (_class) {
                // If class is already closed, notify client
                if (!_class.getOpenEnrollments()) {
                    response.setCode(ResponseCode.ENROLLMENTS_ALREADY_CLOSED).build();
                } else {
                    _class.closeEnrollments();
                    // Increment vector clock
                    _vectorClock.incrementClock();

                    response.setCode(ResponseCode.OK).build();
                }
            }

        }

        // Send response
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {
        ClassServer.debug("PROFESSOR: List Request");

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

    @Override
    public void cancelEnrollment(
            CancelEnrollmentRequest request, StreamObserver<CancelEnrollmentResponse> responseObserver) {
        ClassServer.debug(String.format("PROFESSOR: Cancel Enrollment Request | Args: student %s",
                request.getStudentId()));

        final String student_id = request.getStudentId();
        if (!Pattern.compile("aluno\\d{4}").matcher(student_id).find()) {
            responseObserver
                    .onError(INVALID_ARGUMENT.withDescription("Invalid student ID").asRuntimeException());
            ClassServer.debug("PROFESSOR: Last Cancel Enroll Request failed, invalid student ID");
            return;
        }

        CancelEnrollmentResponse.Builder response = CancelEnrollmentResponse.newBuilder();

        if (!ClassServer.isActive()) {
            response.setCode(ResponseCode.INACTIVE_SERVER);
        } else {
            synchronized (_class) {
                if (!_class.isEnrolled(student_id)) {
                    response.setCode(ResponseCode.NON_EXISTING_STUDENT);
                } else {
                    _class.cancelEnrollment(student_id);
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

}
