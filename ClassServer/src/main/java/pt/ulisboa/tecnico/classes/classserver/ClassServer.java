package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import sun.misc.Signal;

import java.util.Timer;
import java.util.TimerTask;

public class ClassServer {

    private static Server _server;
    private static final int _period = 60000;

    private static boolean _active;
    private static boolean _debug = false;
    private static ClassServerFrontend _classServerFrontend;

    private static String _host;
    private static int _port;
    private static String _qualifier;
    private static String _service;

    private static Class _class;

    private static VectorClock _vectorClock;

    private static Timer _timer;

    // error format
    private static final String _error_fmt = "ERROR: %s\n";

    public static void debug(String debugMessage) {
        if (_debug)
            System.err.printf("D> %s%n%n", debugMessage);
    }

    private static void setTurma(Class turma) {
        _class = turma;
    }

    public static void main(String[] args) throws Exception {

        // Handle SIGINT
        Signal.handle(new Signal("INT"), signal -> exit());

        // Parse arguments
        if (!parseArgs(args)) {
            return;
        }

        if ((args.length > 3) && (args[3].equals("-debug")) || (System.getProperty("debug") != null)) {
            _debug = true;
        }

        _host = args[0];
        _port = Integer.parseInt(args[1]);
        _qualifier = args[2];
        _service = "Turmas";

        setTurma(new Class());

        // Create vector clock for server. First number is the server's own clock.
        _vectorClock = new VectorClock(_host, _port);

        final BindableService AdminImpl = new AdminServiceImpl(_class);
        final BindableService StudentImpl = new StudentServiceImpl(_class, _vectorClock);
        final BindableService ProfessorImpl = new ProfessorServiceImpl(_class, _vectorClock);
        final BindableService ClassServerImpl = new ClassServerServiceImpl(_class, _vectorClock, _qualifier, _port);

        // Create a new server to listen on port and add remote services
        _server = ServerBuilder
                .forPort(_port)
                .addService(StudentImpl)
                .addService(ProfessorImpl)
                .addService(AdminImpl)
                .addService(ClassServerImpl)
                .build();

        // Start server
        _server.start();

        // Create frontend and register server on naming server
        _classServerFrontend = new ClassServerFrontend();
        _classServerFrontend.register(_service, _qualifier, _host, _port);

        _active = true;

        debug(ClassServer.class.getSimpleName());
        debug(String.format("Server started on %s port: %d with qualifier %s", _host, _port, _qualifier));

        // Wait until server is terminated
        _server.awaitTermination();

    }

    /**
     * Schedule primary server state propagation
     */
    private static void setGossip() {
        _timer = new Timer();

        TimerTask gossip = new TimerTask() {
            @Override
            public void run() {
                _classServerFrontend.propagateState(_class.createClassState(), _vectorClock);
            }
        };

        // Every time a period goes by, gossip TimerTask run method is called
        _timer.schedule(gossip, 0, _period);
    }

    private static boolean parseArgs(String[] args) {

        if (args.length < 3) {
            System.err.printf(_error_fmt, "Wrong format. Try $ turmas [host] [port] [P|S]");
            return false;
        }

        try {
            int _port = Integer.parseInt(args[1]);
            if (_port < 1024 || _port > 65535) {
                throw new Exception();
            }
        } catch (Exception e) {
            System.err.printf(_error_fmt, "Wrong port format. Try a number between 1024 and 65535");
            return false;
        }

        if (!args[2].equals("P") && !args[2].equals("S")) {
            System.err.printf(_error_fmt, "Wrong qualifier format. Try 'P' or 'S'");
            return false;
        }
        return true;
    }

    /**
     * Activates the ClassServer
     */
    public static void activateServer() {
        debug("Server activated");
        _active = true;
    }

    /**
     * Deactivates the ClassServer
     */
    public static void deactivateServer() {
        debug("Server deactivated");
        _active = false;
    }

    /**
     * Returns true if ClassServer is active
     *
     * @return
     */
    public static boolean isActive() {
        return _active;
    }

    /**
     * Activates timer to propagate state
     */
    public static void activateGossip() {
        // Schedule gossip state propagation
        setGossip();
    }

    /**
     * Suspends timer to propagate state
     */
    public static void deactivateGossip() {
        _timer.cancel();
    }

    /**
     * Forces propagation of state
     */
    public static void gossip() {
        _classServerFrontend.propagateState(_class.createClassState(), _vectorClock);
    }

    /**
     * Exit Routine
     */
    private static void exit() {
        debug("Exiting server...");
        if (_classServerFrontend.exit(_service, _host, _port)) {
            debug("Server removed from naming server");
        } else {
            debug("Could not remove server from naming server");
        }
        _server.shutdown();
        System.exit(0);
    }

}
