package pt.ulisboa.tecnico.classes.professor;

import java.util.Scanner;
import java.util.regex.Pattern;

public class Professor {
  // error format
  static final String _error_fmt = "ERROR: %s\n";
  // frontend
  static private ProfessorFrontend _professorFrontend;
  // read input
  static private Scanner _scanner;

  public static void main(String[] args) {
    // Setup Frontend
    _professorFrontend = new ProfessorFrontend("localhost", 5000);

    // Read command line
    _scanner = new Scanner(System.in);

    // Parse Commands Loop
    while (true) {
      System.out.printf("%n> ");
      parseCommand(_scanner.nextLine());
    }
  }

  /**
   * Parses a command and executes it
   * 
   * @param cmd
   */
  private static void parseCommand(String cmd) {
    // Split command by space
    String[] tokens = cmd.split(" ");

    switch (tokens[0]) {
      case "closeEnrollments":
        _professorFrontend.closeEnrollmentsRequest();
        break;

      case "list":
        _professorFrontend.listClass();
        break;

      case "openEnrollments":
        // This command requires a second argument
        if (tokens.length < 2) {
          System.err.printf(_error_fmt, "Missing second argument. Specify classe's capacity");
          return;
        } else if (tokens.length != 2) {
          System.err.printf(_error_fmt, "Wrong format. Try: openEnrollments [capacity]");
          return;
        }

        // Parsing second argument
        final int capacity;
        try {
          capacity = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
          System.err.printf(_error_fmt, "Second argument (capacity) must be an integer.");
          return;
        }

        if (capacity < 0) {
          System.err.printf(_error_fmt, "Second argument (capacity) must be greater than 0.");
          return;
        }

        _professorFrontend.openEnrollmentsRequest(capacity);
        break;

      case "cancelEnrollment":
        // This command requires a second argument
        if (tokens.length < 2) {
          System.out.printf(_error_fmt, "Missing first argument. Specify student's ID");
          return;
        } else if (tokens.length != 2) {
          System.err.printf(_error_fmt, "Wrong format. Try: cancelEnrollment [ID]");
          return;
        }

        // Parsing second argument
        final boolean valid_student_id = Pattern.compile("aluno\\d{4}").matcher(tokens[1]).find();
        if (!valid_student_id) {
          System.err.printf(_error_fmt, "Second argument (ID) must respect 'alunoXXXX' format.");
          return;
        }

        _professorFrontend.cancelEnrollment(tokens[1]);
        break;

      case "exit":
        _professorFrontend.exit();
        _scanner.close();
        System.exit(0);
        break;

      default:
        if ((tokens.length != 0) && (tokens[0].length() != 0)) {
          System.err.printf(_error_fmt, String.format("Unknown command: %s", tokens[0]));
        }
        break;
    }
  }
}