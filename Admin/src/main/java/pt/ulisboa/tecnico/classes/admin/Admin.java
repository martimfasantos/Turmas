package pt.ulisboa.tecnico.classes.admin;

import java.util.Scanner;

public class Admin {

  // error format
  private static final String _error_fmt = "ERROR: %s\n";
  // frontend
  private static AdminFrontend _adminFrontend;
  // read input
  private static Scanner _scanner;

  public static void main(String[] args) {
    // Instaciating Admin Frontend
    _adminFrontend = new AdminFrontend();
    // Scanner to read command line
    _scanner = new Scanner(System.in);
    while (true) {
      System.out.printf("%n> ");
      String line = _scanner.nextLine();
      parseCommand(line);
    }
  }

  /**
   * Parses a command and executes it
   *
   * @param line
   */
  private static void parseCommand(String line) {

    String[] tokens = line.split(" ");

    switch (tokens[0]) {
      case "exit":
        _adminFrontend.exit();
        _scanner.close();
        System.exit(0);
        return;

      case "activate":
        // validate argument
        if ((tokens.length == 3) && (Integer.parseInt(tokens[2]) >= 1024 && Integer.parseInt(tokens[2]) < 65535)) {
          _adminFrontend.activate(tokens[1], Integer.parseInt(tokens[2]));
        } else {
          System.err.printf(_error_fmt, "Wrong format: try 'activate [host] [port]'");
        }
        break;

      case "deactivate":
        // validate argument
        if ((tokens.length == 3) && (Integer.parseInt(tokens[2]) >= 1024 && Integer.parseInt(tokens[2]) < 65535)) {
          _adminFrontend.deactivate(tokens[1], Integer.parseInt(tokens[2]));
        } else {
          System.err.printf(_error_fmt, "Wrong format: try 'deactivate [host] [port]'");
        }
        break;

      case "dump":
        // validate argument
        if ((tokens.length == 3) && (Integer.parseInt(tokens[2]) >= 1024 && Integer.parseInt(tokens[2]) < 65535)) {
          _adminFrontend.dump(tokens[1], Integer.parseInt(tokens[2]));
        } else {
          System.err.printf(_error_fmt, "Wrong format: try 'dump [host] [port]'");
        }
        break;

      case "deactivateGossip":
        _adminFrontend.deactivateGossip();
        break;

      case "activateGossip":
        _adminFrontend.activateGossip();
        break;

      case "gossip":
        // validate argument
        if ((tokens.length == 3) && (Integer.parseInt(tokens[2]) >= 1024 && Integer.parseInt(tokens[2]) < 65535)) {
          _adminFrontend.gossip(tokens[1], Integer.parseInt(tokens[2]));
        } else {
          System.err.printf(_error_fmt, "Wrong format: try 'gossip [host] [port]");
        }
        break;

      default:
        if ((tokens.length != 0) && (tokens[0].length() != 0)) {
          System.err.printf(_error_fmt, String.format("Unknown command: %s", tokens[0]));
        }
        break;
    }

  }

}
