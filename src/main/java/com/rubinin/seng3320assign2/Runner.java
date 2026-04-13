package com.rubinin.seng3320assign2;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class Runner {
    private static final String TARGET = "PacketLab";
    private static final String INPUT_FILE = "input.txt";
    private final CrashAnalyzer analyzer;
    public Runner(CrashAnalyzer analyzer){
        this.analyzer = analyzer;
    }
    public void executeTarget(int iteration, String inputData) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", TARGET, INPUT_FILE);
        pb.redirectErrorStream(true); // Merges stderr into stdout
        Process process = pb.start();

        // Monitor for hangs (Timeouts)
        boolean finished = process.waitFor(500, TimeUnit.MILLISECONDS);

        if (!finished) {
            String firstCommand = inputData.split("\\|")[0].trim();
            String timeoutSig = "TIMEOUT_" + firstCommand;
            if (!analyzer.uniqueCrashes.contains(timeoutSig)) {
                analyzer.uniqueCrashes.add(timeoutSig);
                System.out.println("[NEW UNIQUE TIMEOUT] Command: " + firstCommand);
                analyzer.saveCrash(iteration, inputData, "Timeout/Hang", "Process timed out after 2 seconds.");
            }
            process.destroyForcibly();
        } else if (process.exitValue() != 0) {
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

        }
    }

    public void writeInputToFile(String content) {
        try (PrintWriter out = new PrintWriter(new FileWriter(INPUT_FILE))) {
            out.println(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
