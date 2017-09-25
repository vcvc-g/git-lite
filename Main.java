package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */

    public static void main(String... args) {
        Commands c = new Commands();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String command = args[0];
        String currDir = System.getProperty("user.dir");
        File[] directories = new File(currDir).listFiles(File::isDirectory);
        boolean initialized = false;
        for (File f : directories) {
            if (f.getName().equals(".gitlet")) {
                initialized = true;
            }
        }
        if (!initialized && !command.equals("init")) {
            System.out.println("Not in an initialized gitlet directory.");
            System.exit(0);
        }
        List<String> method1list = new ArrayList<>();
        method1list.add("init");
        method1list.add("add");
        method1list.add("commit");
        method1list.add("rm");
        method1list.add("log");
        method1list.add("global-log");
        method1list.add("find");
        if (method1list.contains(command)) {
            methodHelper1(c, command, args);
        } else {
            methodHelper2(c, command, args);
        }
    }

    public static void methodHelper1(Commands c, String command, String... args) {
        switch (command) {
            case "init":
                if (args.length > 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.init();
                break;
            case "add":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.add(args[1]);
                break;
            case "commit":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                if (!(args[1] instanceof String) || args[1].length() == 0) {
                    System.out.print("Please enter a commit message.");
                    System.exit(0);
                }
                try {
                    c.commit(args[1]);
                } catch (IllegalStateException e) {
                    System.out.print("Serializing error");
                }
                break;
            case "rm":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.rm(args[1]);
                break;
            case "log":
                if (args.length > 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.log();
                break;
            case "global-log":
                if (args.length > 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.globalLog();
                break;
            case "find":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.find(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    public static void methodHelper2(Commands c, String command, String... args) {
        switch (command) {
            case "status":
                if (args.length > 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.status();
                break;
            case "checkout":
                if (args[1].equals("--")) {
                    c.checkout(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    c.checkout2(args[1], args[3]);
                } else if (args.length == 2) {
                    c.checkout3(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.branch(args[1]);
                break;
            case "rm-branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.rmBranch(args[1]);
                break;
            case "reset":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.reset(args[1]);
                break;
            case "merge":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                c.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
}
