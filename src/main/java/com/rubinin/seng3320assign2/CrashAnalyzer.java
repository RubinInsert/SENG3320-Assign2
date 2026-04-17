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

    public String getHangSignature(String threadDump) {
        if (threadDump == null || threadDump.isBlank()) { // Generic fall back for if there isnt a thread dump
            return "HANG|NO_THREAD_DUMP";
        }

        String[] lines = threadDump.split("\\R"); // Split thread dump by lines
        StringBuilder packetFrames = new StringBuilder();
        int frameCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("at PacketLab.")) { // Look for the first line to do with packetlab
                if (packetFrames.length() > 0) {
                    packetFrames.append(" -> ");
                }
                // Keep exact line numbers from the dump to show where execution hung.
                packetFrames.append(trimmed); // Keep capturing the lines until we have 8 lines, or run out of lines.
                frameCount++;

                if (frameCount >= 8) {
                    break;
                }
            }
        }

        if (frameCount == 0) { // If there are no frames, just return a generic signature again
            return "HANG|THREAD_DUMP|NO_PACKETLAB_FRAME";
        }
        return "HANG|THREAD_DUMP|" + packetFrames; // Construct the hang signature
    }

    public String getHangReportSnippet(String threadDump) {
        if (threadDump == null || threadDump.isBlank()) { // Thread dump timed out or was empty.
            return "Process timed out after 500 ms. Thread dump unavailable.";
        }

        String[] lines = threadDump.split("\\R");
        StringBuilder snippet = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("at PacketLab.")) {
                snippet.append("\t").append(trimmed).append("\n");
            }
        }

        if (snippet.length() == 0) {
            return "Process timed out after 500 ms. No PacketLab frames found in thread dump.";
        }
        return snippet.toString().trim(); // Contains the stack trace for a hang in the program
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
