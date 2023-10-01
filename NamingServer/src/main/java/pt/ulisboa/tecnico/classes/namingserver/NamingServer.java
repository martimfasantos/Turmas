package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class NamingServer {

  private static boolean _debug = false;

  public static void debug(String debugMessage) {
    if (_debug)
      System.err.printf("D> %s%n%n", debugMessage);
  }

  public static void main(String[] args) throws Exception {

    if ((args.length > 0) && (args[0].equals("-debug")) || (System.getProperty("debug") != null)) {
      _debug = true;
    }

    final int _port = 5000;
    final String _host = "localhost";

    final BindableService NamingServerServiceImpl = new NamingServerServiceImpl();

    // Create a new server to listen on port and add remote services
    Server namingServer = ServerBuilder
        .forPort(_port)
        .addService(NamingServerServiceImpl)
        .build();

    // Start server
    namingServer.start();

    debug(NamingServer.class.getSimpleName());
    debug(String.format("Server started on %s port: %d", _host, _port));

    // Wait until server is terminated
    namingServer.awaitTermination();
  }
}
