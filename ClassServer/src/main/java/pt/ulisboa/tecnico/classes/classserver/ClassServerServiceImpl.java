package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc.ClassServerServiceImplBase;

import javax.crypto.spec.PSource;

public class ClassServerServiceImpl extends ClassServerServiceImplBase {
    final private Class _class;

    private final VectorClock _vectorClock;

    private final String _qualifier;

    private final Integer _port;

    public ClassServerServiceImpl(Class _class, VectorClock vectorClock, String qualifier, Integer port) {
        this._class = _class;
        this._vectorClock = vectorClock;
        this._qualifier = qualifier;
        this._port = port;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        if (!ClassServer.isActive()) {
            PropagateStateResponse response = PropagateStateResponse.newBuilder()
                    .setCode(ResponseCode.OK).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }


        ClassServer.debug("SERVER: State Propagation received");

        ClassesDefinitions.ClassState serverState = request.getClassState();
        String serverQualifier = request.getQualifier();
        Integer serverPort = request.getClock().getPort();
        String serverHost = request.getClock().getHost();
        Integer serverClock = request.getClock().getValue();

        boolean wasUpdated = false;

        // compared saved clock with received one
        if (_vectorClock.getClock(serverHost, serverPort) < serverClock) {
            if (_qualifier.equals("P") || (serverQualifier.equals("S") && _port < serverPort)) {
                synchronized (_class) {
                    // The capacity value will always be updated to the highest one
                    // Open enrollments boolean always set equal to incoming class state's value
                    if (serverState.getOpenEnrollments()) {
                        _class.openEnrollments(serverState.getCapacity());
                    } else {
                        _class.setCapacity(serverState.getCapacity());
                    }

                    // Enrolled students exceed class capacity
                    for (Student student : serverState.getEnrolledList()) {
                        String studentId = student.getStudentId();
                        if (!_class.isFull()) {
                            if (!_class.isDiscarded(studentId)) {
                                _class.enroll(studentId, student.getStudentName());
                            }
                        } else if (!_class.isEnrolled(studentId)) {
                            _class.cancelEnrollment(studentId);
                        }
                    }

                    for (Student student : serverState.getDiscardedList()) {
                        if (!_class.isEnrolled(student.getStudentId())) {
                            _class.cancelEnrollment(student.getStudentId());
                        }
                    }
                }
            } else {
                synchronized (_class) {
                    Class newClass = new Class(serverState);

                    // The capacity value will always be updated to the highest one
                    // Open enrollments boolean always set equal to incoming class state's value
                    if (_class.getOpenEnrollments()) {
                        newClass.openEnrollments(_class.getCapacity());
                    } else {
                        newClass.setCapacity(serverState.getCapacity());
                    }

                    for (String studentId : _class.getEnrolled().keySet()) {
                        if (!newClass.isFull()) {
                            if (!newClass.isDiscarded(studentId)) {
                                newClass.enroll(studentId, _class.getName(studentId));
                            }
                        } else if (!newClass.isEnrolled(studentId)) {
                            newClass.cancelEnrollment(studentId);
                        }
                    }

                    for (String studentId : _class.getDiscarded().keySet()) {
                        if (!newClass.isEnrolled(studentId)) {
                            newClass.cancelEnrollment(studentId);
                        }
                    }

                    _class.setFromClass(newClass);
                }
            }

            _vectorClock.updateClock(serverHost, serverPort, serverClock);
            _vectorClock.incrementClock();
        }


        PropagateStateResponse response = PropagateStateResponse.newBuilder()
                .setCode(ResponseCode.OK).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
