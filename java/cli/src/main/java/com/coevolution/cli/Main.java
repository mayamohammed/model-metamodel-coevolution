package com.coevolution.cli;

import com.coevolution.cli.commands.PredictCommand;
import com.coevolution.cli.commands.DiffCommand;
import com.coevolution.cli.commands.ReportCommand;

public class Main {

    private static final String VERSION = "1.0.0";
    private static final String API_URL =
        System.getProperty("api.url", "http://localhost:5000");

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(0);
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "predict":
                new PredictCommand(API_URL).execute(args);
                break;
            case "diff":
                new DiffCommand(API_URL).execute(args);
                break;
            case "report":
                new ReportCommand(API_URL).execute(args);
                break;
            case "version":
                System.out.println("coevolution-cli v" + VERSION);
                break;
            case "help":
            case "--help":
            case "-h":
                printUsage();
                break;
            default:
                System.err.println("[ERROR] Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("=" .repeat(55));
        System.out.println("  Metamodel Coevolution CLI v" + VERSION);
        System.out.println("=" .repeat(55));
        System.out.println("  Commands:");
        System.out.println("    predict  --added-classes N ...");
        System.out.println("             Predict metamodel change type");
        System.out.println();
        System.out.println("    diff     --v1 <file1> --v2 <file2>");
        System.out.println("             Compare two metamodel versions");
        System.out.println();
        System.out.println("    report   --input <csv> --output <dir>");
        System.out.println("             Generate prediction report");
        System.out.println();
        System.out.println("    version  Show version");
        System.out.println("    help     Show this help");
        System.out.println("=" .repeat(55));
        System.out.println("  API URL: " + API_URL);
        System.out.println("  Override: java -Dapi.url=http://host:5000 ...");
        System.out.println("=" .repeat(55));
    }
}