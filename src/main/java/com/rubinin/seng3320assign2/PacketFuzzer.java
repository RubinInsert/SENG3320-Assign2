package com.rubinin.seng3320assign2;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PacketFuzzer {
    private static final String TARGET = "PacketLab";
    private static final String INPUT_FILE = "input.txt";
    private static final Random random = new Random();
    private static final Set<String> uniqueCrashes = new HashSet<>(); // Contains the hash of any previously seen crash (utilizes Error type and location as signature)
    public static void main(String[] args) {
        int totalExecutions = 10000;
        int currentExecs = 0;

        while (currentExecs < totalExecutions) {
            // Seed Selection. I.e. choosing a syntactically correct input
            String seed = seedCorpus.get(random.nextInt(seedCorpus.size()));

            // Calculating how many times the seed should be mutated
            int energy = calculateEnergy(seed);

            for (int e = 0; e < energy && currentExecs < totalExecutions; e++) {
                // For each "energy" perform a mutation
                String fuzzInput = mutate(seed);

                // Writes to the input.txt
                writeInputToFile(fuzzInput);
                try {
                    // Executes against target (PacketLab)
                    executeTarget(currentExecs, fuzzInput);
                } catch (Exception ex) { /* Log error */ }

                currentExecs++;
            }
        }
    }
    private static final List<String> seedCorpus = new ArrayList<>(Arrays.asList(
            // Valid Inputs from Brief
            "PKT|10.0.0.1|10.0.0.2|DATA|4|ABCD",
            "PKT|10.0.0.2|10.0.0.3|CTRL|3|XYZ",
            "PKT|10.0.0.1|10.0.0.4|DATA|5|HELLO",
            "COUNT|DATA",
            "AVG|10.0.0.1",
            "SLICE|NETWORK|1|4",
            "MERGE|-|foo|bar",
            "REPEAT|AB|3",
            "LOOKUP|10.0.0.1",
            "HEX|ff",
            "TOP|2",
            // Syntactically Correct but Malicious
            "AVG|99.99.99.99", // an IP with no prior PKT record
            "SPIN|-1", // Negative number gets smaller and never equals zero
            "LOOKUP|1.2.3.4", // Cache for PKT is null
            "MERGE||a|b", // StringIndexOutOfBounds
            "WINDOW|10", // Off-by-one ArrayIndexOutOfBounds
            "REPEAT|A|-1", // NegativeArraySize
            "TOP|9999", // ArrayIndexOutOfBounds
            "HI" // StringIndexOutOfBounds
    ));
    private static final Integer[] INT_PARTITIONS = new Integer[] {Integer.MAX_VALUE, 0, -1};
    private static String mutate(String seed) {
        int strategy = random.nextInt(7);
        switch (strategy) {
            case 0: // Char flipping
                int idx = random.nextInt(seed.length());
                char randomChar = (char) (random.nextInt(95) + 32);
                return seed.substring(0, idx) + randomChar + seed.substring(idx + 1);
            case 1: // Value Arithmetic -> Replaces any sequence of digits with one of the INT_PARTITIONS
                return seed.replaceAll("\\d+", String.valueOf(INT_PARTITIONS[random.nextInt(INT_PARTITIONS.length)]));
            case 2: // Line Duplication or Deletion
                return random.nextBoolean() ? seed + "\n" + seed : "";
            case 3: // Delimiter tampering by adding an additional delimiter or removing
                return seed.replace("|", random.nextBoolean() ? "||" : " ");
            case 4: // Command name mutation, tests case sensitivity of commands
                String[] parts = seed.split("\\|");
                if (parts.length > 0) {
                    parts[0] = parts[0].toLowerCase(); // Test case sensitivity
                    return String.join("|", parts);
                }
                break;
            case 5: // Field Deletion
                String[] fields = seed.split("\\|");
                if (fields.length > 1) {
                    int removeIdx = random.nextInt(fields.length);
                    List<String> list = new ArrayList<>(Arrays.asList(fields));
                    list.remove(removeIdx);
                    return String.join("|", list);
                }
                break;
            case 6: // Payload Swapping/Mutation
                String[] p = seed.split("\\|");
                if (p.length > 1) {
                    // Determine which field is the "payload" based on the command. E.g. PKT stores it's payload in the 5th index (6th slot)
                    int payloadIdx = 1; // Default for most directives like REPEAT, SLICE, LOOKUP
                    if (p[0].equals("PKT") && p.length > 5) payloadIdx = 5;
                    else if (p[0].equals("MERGE") && p.length > 2) payloadIdx = random.nextBoolean() ? 2 : 3;

                    String[] maliciousPayloads = {
                            "",                      // Empty string
                            "A".repeat(2000),        // Large buffer to test REPEAT's char[] buffer
                            "!@#$%^&*()\0\n",      // Control and special characters
                            "10.0.0.1",              // Swapping an IP into a text field
                            "DATA"                   // Swapping a type into a text field
                    };
                    p[payloadIdx] = maliciousPayloads[random.nextInt(maliciousPayloads.length)]; // Replaces payload slot with a malicious input
                    return String.join("|", p);
                }
                break;
            default:
                return seed;
        }
        return seed;
    }
    private static int calculateEnergy(String seed) {
        // More complex (longer) seeds get more "energy" (more mutation attempts)
        return Math.max(10, seed.length() / 2);
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