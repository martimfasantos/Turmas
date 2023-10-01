package pt.ulisboa.tecnico.classes.student;

import java.util.Scanner;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class Student {

  private static String _id;
  private static String _name;

  private static StudentFrontend _studentFrontend;
  private static Scanner _scanner;

  // error messages
  static final String _error_fmt = "ERROR: %s\n";

  public static void main(String[] args) {

    if (!parseArguments(args)) {
      System.exit(0);
    }

    // Setup Frontend (receives IP and port of naming server)
    _studentFrontend = new StudentFrontend("localhost", 5000);

    // Read command line
    _scanner = new Scanner(System.in);

    // Parse Commands Loop
    while (true) {
      System.out.printf("%n> ");
      parseCommand(_scanner.nextLine());
    }
  }


  /**
   * Parses the input arguments given when executing the program
   *
   * @param args
   */
  private static boolean parseArguments(String[] args) {
    if (args.length < 2) {
      System.err.printf(_error_fmt, "Wrong format. Try $ aluno [ID] [Student Name]");
      return false;
    }

    // Parse Student ID
    String student_id = args[0];
    if (!Pattern.compile("aluno\\d{4}").matcher(student_id).matches()) {
      System.err.printf(_error_fmt, "The first argument (ID) should have the 'alunoXXXX' format.");
      return false;
    }

    // Parse Name
    StringJoiner student_name = new StringJoiner(" ");
    for (int i = 1; i < args.length; i++) {
      student_name.add(args[i]);
    }

    if (student_name.length() < 3 || student_name.length() > 30) {
      System.err.printf(_error_fmt, "The second argument (Student Name) should have between 3 and 30 characters");
      return false;
    }

    // Save credentials
    _id = student_id;
    _name = student_name.toString();

    return true;
  }


  /**
   * Parses a command and executes it
   *
   * @param line
   */
  private static void parseCommand(String line) {

    // Split command by space
    String[] tokens = line.split(" ");

    String command = tokens[0];

    switch (command) {
      case "list":
        _studentFrontend.listClass();
        break;

      case "enroll":
        _studentFrontend.enroll(_id, _name);
        break;

      case "exit":
        _scanner.close();
        _studentFrontend.exit();
        System.exit(0);
        break;

      default:
        if ((tokens.length != 0) && (tokens[0].length() != 0)) {
          System.err.printf(_error_fmt, String.format("Unknown command: %s\n", command));
        }
        break;
    }
  }
}
