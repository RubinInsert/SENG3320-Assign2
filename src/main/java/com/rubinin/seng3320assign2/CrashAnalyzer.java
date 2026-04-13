package com.rubinin.seng3320assign2;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class CrashAnalyzer {
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
        String filename = "crash_iteration_" + iteration + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("--- CRASH REPORT ---");
            out.println("Reason: " + reason);
            out.println("\n--- ERROR OUTPUT / STACK TRACE ---");
            out.println(stackTrace.isEmpty() ? "No output captured." : stackTrace);
            out.println("\n--- REPRODUCING INPUT ---");
            out.println(input);
        }
    }
}
