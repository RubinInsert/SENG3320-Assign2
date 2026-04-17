package com.rubinin.seng3320assign2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class Runner {
    private static final String TARGET = "PacketLab";
    private static final String INPUT_FILE = "input.txt";
    private final CrashAnalyzer analyzer;

    //Should this just be creating its own analyser?
    public Runner(CrashAnalyzer analyzer){
        this.analyzer = analyzer;
    }

    public void executeTarget(int iteration, String inputData) throws IOException, InterruptedException {
        // Writes to the input.txt
        writeInputToFile(inputData);
        
        ProcessBuilder pb = new ProcessBuilder("java", TARGET, INPUT_FILE);
        pb.redirectErrorStream(true); // Merges stderr into stdout
        Process process = pb.start();

        // Monitor for hangs (Timeouts)
        boolean finished = process.waitFor(500, TimeUnit.MILLISECONDS);

        if (!finished) {
            String threadDump = captureThreadDump(process.pid()); // Runs jcmd on target for a thread dump (for stack trace)
            String timeoutSig = analyzer.getHangSignature(threadDump); // Gets specifically the signature to do with packet lab

            if (!analyzer.uniqueCrashes.contains(timeoutSig)) {
                analyzer.uniqueCrashes.add(timeoutSig);
                System.out.println("[NEW UNIQUE TIMEOUT] Signature: " + timeoutSig);

                String details = analyzer.getHangReportSnippet(threadDump);
                analyzer.saveCrash(iteration, inputData, "Timeout/Hang", details);
            }

            process.destroyForcibly();
            process.waitFor(100, TimeUnit.MILLISECONDS);
            return;
        }
        
        if (process.exitValue() != 0) {
            // Capture the output (which now includes the stack trace)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            // Monitor for crashes (Non-zero exit codes)
            String crashSignature = analyzer.getCrashSignature(output.toString());
            if (!analyzer.uniqueCrashes.contains(crashSignature)) {
                analyzer.uniqueCrashes.add(crashSignature);
                System.out.println("[NEW UNIQUE CRASH] Iteration " + iteration);
                analyzer.saveCrash(iteration, inputData, "Exit Code: " + process.exitValue(), output.toString());
            }
            return;
        }
    }

    private void writeInputToFile(String content) {
        try {
            Path path = Paths.get(INPUT_FILE);
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String captureThreadDump(long pid) {
        String javaHome = System.getProperty("java.home", ""); // Get directory to java tools
        String jcmd = Paths.get(javaHome, "bin", windowsToolName("jcmd")).toString(); // Get directory to jcmd tool
        ProcessBuilder pb = new ProcessBuilder(jcmd, String.valueOf(pid), "Thread.print"); // Command to capture thread dump of the target program
        pb.redirectErrorStream(true); // merge stderr with stdout

        try {
            Process proc = pb.start();
            boolean finished = proc.waitFor(2, TimeUnit.SECONDS);

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n"); // Capture the output of the jcmd command
                }
            }

            if (!finished) {
                proc.destroyForcibly(); // Ensure we don't leave hanging jcmd processes if it takes too long
            }

            String text = output.toString();
            if (looksLikeThreadDump(text)) { // Does the output match a basic thread dump regex
                return text;
            }
            return "";
        } catch (Exception ex) {
            return "";
        }
    }

    private boolean looksLikeThreadDump(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        return output.contains("Full thread dump") // Things in the thread dump that should exist
                || output.contains("java.lang.Thread.State")
                || output.contains("\"main\"");
    }

    private String windowsToolName(String baseName) { // If on windows add ".exe", otherwise return as is.
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return baseName + ".exe";
        }
        return baseName;
    }

}
