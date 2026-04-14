package com.rubinin.seng3320assign2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class CrashAnalyzer {
    private static final String OUTPUT_DIR = "crashes";

    public final Set<String> uniqueCrashes = new HashSet<>(); // Contains the hash of any previously seen crash (utilizes Error type and location as signature)
    public String getCrashSignature(String stackTrace) {
        if (stackTrace.isEmpty() || !stackTrace.contains("Exception")) return "UnknownError";

        String type = "";
        String location = "";
        String[] lines = stackTrace.split("\n");

        for (String line : lines) {
            // Capture the Exception Type, but ignore the specific message after the colon
            if (line.contains("Exception")) {
                // "java.lang.NegativeArraySizeException: -1" becomes "java.lang.NegativeArraySizeException"
                type = line.split(":")[0].trim();
            }

            // Capture the exact line in the target program
            if (line.contains("at PacketLab.")) {
                location = line.trim();
                break;
            }
        }
        return type + " @ " + location;
    }

    public void saveCrash(int iteration, String input, String reason, String stackTrace) throws IOException {    
        // Attempt to create the crash folder
        Path crashFolder;
        try {
            crashFolder = Paths.get(OUTPUT_DIR, "iteration_" + iteration);
            Files.createDirectories(crashFolder);
        } catch (Exception e) {
            System.err.print("Error creating output directory: ");
            System.err.println(e);
            return;
        }

        //Write crash report
        Path crashReportPath = crashFolder.resolve("Crash Report.txt");
        try (BufferedWriter out = Files.newBufferedWriter(crashReportPath)) {
            out.write("--- CRASH REPORT ---\n");
            out.write("Reason: " + reason);
            out.write("\n\n--- ERROR OUTPUT / STACK TRACE ---\n");
            out.write(stackTrace.isEmpty() ? "No output captured." : stackTrace);
            out.write("\n--- REPRODUCING INPUT ---\n");
            out.write(input);
        }
        catch (Exception e) {
            System.err.println("Error creating crash report: ");
            System.err.println(e);
            return;
        }

        //Write input file
        Path inputTextPath = crashFolder.resolve("input.txt");
        Files.writeString(inputTextPath, input, StandardCharsets.UTF_8);
    }
}
