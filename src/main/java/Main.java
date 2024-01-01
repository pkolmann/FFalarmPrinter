import at.kolmann.java.FFalarmPrinter.FFalarmPrinter;
import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();

        Option input = new Option("i", "input", true, "input file path");
        input.setRequired(false);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("FFalarmPrinter", options);

            System.exit(1);
        }

        String inputFilePath = cmd.getOptionValue("input");

//        System.out.println("Version: " + System.getProperty("java.version"));
        FFalarmPrinter ffAlarmPrinter = new FFalarmPrinter();
        ffAlarmPrinter.run(inputFilePath);
    }
}
