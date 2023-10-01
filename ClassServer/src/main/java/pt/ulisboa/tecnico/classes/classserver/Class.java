package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Class {

    private int _capacity;
    private boolean _openEnrollments;
    private ConcurrentHashMap<String, String> _enrolled = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> _discarded = new ConcurrentHashMap<>();

    public Class() {

    }

    public Class(ClassState classState) {
        setFromClassState(classState.getCapacity(), classState.getOpenEnrollments(),
                classState.getEnrolledList(), classState.getDiscardedList());
    }

    /**
     * Gets class's capacity
     *
     * @return
     */
    public int getCapacity() {
        return this._capacity;
    }

    /**
     * Sets the capacity for the class
     * @param capacity
     */
    public void setCapacity(int capacity) {
        _capacity = Math.max(capacity, _capacity);
    }

    /**
     * Gets class's size
     *
     * @return
     */
    public int getClassSize() {
        return _enrolled.size();
    }

    /**
     * Returns true if enrollments are open
     *
     * @return
     */
    public boolean getOpenEnrollments() {
        return this._openEnrollments;
    }

    /**
     * Gets enrolled students
     *
     * @return
     */
    public ConcurrentHashMap<String, String> getEnrolled() {
        return _enrolled;
    }

    /**
     * Gets discarded students
     *
     * @return
     */
    public ConcurrentHashMap<String, String> getDiscarded() {
        return _discarded;
    }

    /**
     * Returns true if a student is discarded from the class
     * @param studentId
     * @return
     */
    public boolean isDiscarded(String studentId) {
        return _discarded.containsKey(studentId);
    }

    /**
     * Returns true if class is currently full
     *
     * @return
     */
    public boolean isFull() {
        return (getClassSize() == this._capacity);
    }

    /**
     * Returns true if a particular student
     * is currently enrolled on the class
     *
     * @param id
     * @return
     */
    public boolean isEnrolled(String id) {
        return _enrolled.containsKey(id);
    }

    /**
     * Opens the class's enrollments
     * with a certain maximum capacity
     *
     * @param capacity
     */
    public void openEnrollments(int capacity) {
        this._capacity = Math.max(_capacity, capacity);
        this._openEnrollments = true;
    }

    /**
     * Closes class's enrollments
     */
    public void closeEnrollments() {
        this._openEnrollments = false;
    }

    /**
     * Cancels enrollments for a particular student
     *
     * @param id
     */
    public void cancelEnrollment(String id) {
        String nome = _enrolled.get(id);
        if (nome != null) {
            _enrolled.remove(id);
            _discarded.put(id, nome);
        }
    }

    /**
     * Enrolls a particular student on the class
     *
     * @param id
     * @param nome
     */
    public void enroll(String id, String nome) {
        _discarded.remove(id);
        _enrolled.put(id, nome);
    }

    public String getName(String id) {
        return _enrolled.get(id);
    }

    /**
     * Creates a ClassState object for the current class state
     *
     * @return
     */
    public synchronized ClassState createClassState() {
        // Create a classState to send to the client
        final List<Student> EnrolledList = new ArrayList<>();
        final List<Student> DiscardedList = new ArrayList<>();

        // Set parameters for classState
        int capacity = getCapacity();
        boolean openEnrollments = getOpenEnrollments();

        // Add the list of enrolled students
        for (ConcurrentHashMap.Entry<String, String> entry : getEnrolled().entrySet()) {
            final String id = entry.getKey();
            final String name = entry.getValue();
            final Student student = Student.newBuilder().setStudentId(id).setStudentName(name).build();
            EnrolledList.add(student);
        }

        // Add the list of discarded students
        for (ConcurrentHashMap.Entry<String, String> entry : getDiscarded().entrySet()) {
            final String id = entry.getKey();
            final String name = entry.getValue();
            final Student student = Student.newBuilder().setStudentId(id).setStudentName(name).build();
            DiscardedList.add(student);
        }

        // Build class state with enrolled and discarded students, capacity and
        // openEnrollments info
        return ClassState.newBuilder()
                .addAllEnrolled(EnrolledList)
                .addAllDiscarded(DiscardedList)
                .setCapacity(capacity)
                .setOpenEnrollments(openEnrollments)
                .build();
    }

    public synchronized void setFromClassState(Integer capacity, boolean _openEnrollments, List<Student> enrolled, List<Student> discarded) {
        this._capacity = capacity;
        this._openEnrollments = _openEnrollments;
        this._enrolled = new ConcurrentHashMap<>();
        this._discarded = new ConcurrentHashMap<>();

        for (Student student : enrolled) {
            enroll(student.getStudentId(), student.getStudentName());
        }

        for (Student student : discarded) {
            cancelEnrollment(student.getStudentId());
        }
    }

    public synchronized void setFromClass(Class _class) {
        this._capacity = _class.getCapacity();
        this._openEnrollments = _class.getOpenEnrollments();
        this._enrolled = _class.getEnrolled();
        this._discarded = _class.getDiscarded();
    }
}
