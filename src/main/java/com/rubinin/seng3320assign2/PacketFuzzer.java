package com.rubinin.seng3320assign2;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PacketFuzzer {
    private static final String TARGET = "PacketLab";
    private static final String INPUT_FILE = "input.txt";
    private static final Random random = new Random();
    private static final Set<String> uniqueCrashes = new HashSet<>(); // Contains the hash of any previously seen crash (utilizes Error type and location as signature)
    private static int globalCounter = 0;
    public static void main(String[] args) {
        int iterations = 1000; // Total test runs
        System.out.println("Starting fuzzer for " + TARGET + "...");

        for (int i = 1; i <= iterations; i++) {
            // Generate: Create random input based on the PKT format
            StringBuilder sb = new StringBuilder();
            if (random.nextInt(10) < 3) {
                sb.append(generateEachChoiceInput()).append("\n");
                globalCounter++;
            } else {
                if (random.nextInt(5) == 0) {
                    sb.append(generateInteractionPair()).append("\n");
                } else {
                    int numLines = random.nextInt(10) + 1;
                    for (int j = 0; j < numLines; j++) {
                        sb.append(generateEachChoiceInput()).append("\n");
                        globalCounter++;
                    }
                }
            }
            String fuzzInput = sb.toString();
            writeInputToFile(fuzzInput);

            // Execute and Monitor: Run the program and check for crashes
            try {
                executeTarget(i, fuzzInput);
            } catch (Exception e) {
                System.err.println("Error during execution at iteration " + i + ": " + e.getMessage());
            }
        }
    }
    // Add non-numeric strings to trigger NumberFormatExceptions
    private static final String[] INVALID_NUMBERS = {"abc", "1.2.3", " ", "!", "0xZZ"};

    // Add malformed line structures
    private static final String[] MALFORMED_LINES = {"PKT", "AVG|", "SLICE|A|B", "MERGE|||", "REPEAT|A|"};
    private static String generateEachChoiceInput() {
        int cmdChoice = globalCounter % 18;

        // FIX: Adding 'globalCounter / 18' shifts the index so we hit every partition eventually
        int partitionIndex = (globalCounter % INT_PARTITIONS.length + (globalCounter / 18)) % INT_PARTITIONS.length;
        Object boundary = INT_PARTITIONS[partitionIndex];

        String ip = IP_PARTITIONS[globalCounter % IP_PARTITIONS.length];
        String str = STR_PARTITIONS[globalCounter % STR_PARTITIONS.length];

        switch(cmdChoice) {
            case 0: return String.format("PKT|%s|192.168.1.1|DATA|%d|%s", ip, boundary, str);
            case 1: return "COUNT|" + (globalCounter % 2 == 0 ? "DATA" : "CTRL");
            case 2: return "AVG|" + ip;
            case 3: return "SLICE|" + str + "|" + boundary + "|" + getPartitionedInt();
            case 4: return "MERGE|" + str + "|" + str + "|" + str;
            case 5: return "REPEAT|" + str + "|" + boundary;
            case 6: return "LOOKUP|" + ip;
            case 7: return "WINDOW|" + boundary;
            case 8: return "HEX|" + (globalCounter % 2 == 0 ? Integer.toHexString(globalCounter) : "0xZZ");
            case 9: return "TOP|" + boundary;
            case 10: return str;
            case 11: return "SPIN|" + boundary;
            case 12: return "AVG|10.0.0.99";
            case 13: return "LOOKUP|99.99.99.99";
            case 14: return "X";
            case 15: return "TOP|100";
            case 16: // Triggers NumberFormatException in numeric commands
                return "WINDOW|" + INVALID_NUMBERS[globalCounter % INVALID_NUMBERS.length];
            case 17: // Triggers ArrayIndexOutOfBounds by sending incomplete lines
                return MALFORMED_LINES[globalCounter % MALFORMED_LINES.length];
            default: return "HELP";
        }
    }
    // Define partitions as constant arrays
    private static final Object[] INT_PARTITIONS = {0, -1, 1, Integer.MAX_VALUE, Integer.MIN_VALUE, 255};
    private static final String[] IP_PARTITIONS = {"10.0.0.1", "256.256.256.256", "", "0.0.0.0", "127.0.0.1"};
    private static final String[] STR_PARTITIONS = {"A", "", "LONG_STRING_REPEATED_".repeat(10), "!@#$%^&*()", "PKT|INVALID|FORMAT"};

    private static Object getPartitionedInt() {
        return INT_PARTITIONS[random.nextInt(INT_PARTITIONS.length)];
    }
    private static String generateInteractionPair() {
        // Now uses the systematic generator instead of random
        String p1 = generateEachChoiceInput();
        globalCounter++;
        String p2 = generateEachChoiceInput();
        globalCounter++;
        return p1 + "\n" + p2;
    }

    private static void writeInputToFile(String content) {
        try (PrintWriter out = new PrintWriter(new FileWriter(INPUT_FILE))) {
            out.println(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeTarget(int iteration, String inputData) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", TARGET, INPUT_FILE);
        pb.redirectErrorStream(true); // Merges stderr into stdout
        Process process = pb.start();

        // Monitor for hangs (Timeouts)
        boolean finished = process.waitFor(500, TimeUnit.MILLISECONDS);





        if (!finished) {
            String firstCommand = inputData.split("\\|")[0].trim();
            String timeoutSig = "TIMEOUT_" + firstCommand;
            if (!uniqueCrashes.contains(timeoutSig)) {
                uniqueCrashes.add(timeoutSig);
                System.out.println("[NEW UNIQUE TIMEOUT] Command: " + firstCommand);
                saveCrash(iteration, inputData, "Timeout/Hang", "Process timed out after 2 seconds.");
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
            String crashSignature = getCrashSignature(output.toString());
            if (!uniqueCrashes.contains(crashSignature)) {
                uniqueCrashes.add(crashSignature);
                System.out.println("[NEW UNIQUE CRASH] Iteration " + iteration);
                saveCrash(iteration, inputData, "Exit Code: " + process.exitValue(), output.toString());
            }

        }
    }
    private static String getCrashSignature(String stackTrace) {
        if (stackTrace.isEmpty() || !stackTrace.contains("Exception")) return "UnknownError";

        String type = "";
        String location = "";
        String[] lines = stackTrace.split("\n");

        for (String line : lines) {
            // 1. Capture the Exception Type, but ignore the specific message after the colon
            if (line.contains("Exception")) {
                // "java.lang.NegativeArraySizeException: -1" becomes "java.lang.NegativeArraySizeException"
                type = line.split(":")[0].trim();
            }

            // 2. Capture the exact line in the target program
            if (line.contains("at PacketLab.")) {
                location = line.trim();
                break;
            }
        }
        return type + " @ " + location;
    }
    private static void saveCrash(int iteration, String input, String reason, String stackTrace) throws IOException {
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