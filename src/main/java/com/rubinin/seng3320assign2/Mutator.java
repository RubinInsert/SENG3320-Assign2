package com.rubinin.seng3320assign2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Mutator {
    private final Random random = new Random();
    private static final Integer[] INT_PARTITIONS = new Integer[] {Integer.MAX_VALUE, 0, -1};
    private static final String[] EXTRA_LINES = new String[] { // For the insert extra line mutation
            "COUNT|DATA",
            "AVG|10.0.0.1",
            "LOOKUP|1.2.3.4",
            "WINDOW|10",
            "SPIN|-1",
            "HI"
    };

    public String mutate(String seed) {
        List<String> lines = new ArrayList<>(Arrays.asList(seed.split("\\R", -1)));
        if (lines.isEmpty() || (lines.size() == 1 && lines.get(0).isEmpty())) { // If after splitting, there is no inputs. Just add some dummy input
            lines.clear();
            lines.add("HI");
        }

        int lineIdx = random.nextInt(lines.size()); // Randomly select a line to apply mutation
        String selectedLine = lines.get(lineIdx);

        int strategy = random.nextInt(9); // Randomly select a mutation strategy
        switch (strategy) {
            case 0: // Char flipping on one random line
                if (!selectedLine.isEmpty()) {
                    int idx = random.nextInt(selectedLine.length());
                    char randomChar = (char) (random.nextInt(95) + 32);
                    lines.set(lineIdx, selectedLine.substring(0, idx) + randomChar + selectedLine.substring(idx + 1));
                }
                break;
            case 1: // Value Arithmetic on one line
                lines.set(lineIdx, selectedLine.replaceAll("\\d+", String.valueOf(INT_PARTITIONS[random.nextInt(INT_PARTITIONS.length)])));
                break;
            case 2: // Line Duplication or Deletion
                if (random.nextBoolean()) {
                    lines.add(lineIdx, selectedLine);
                } else if (lines.size() > 1) {
                    lines.remove(lineIdx);
                } else {
                    lines.set(0, "");
                }
                break;
            case 3: // Delimiter tampering on one line
                lines.set(lineIdx, selectedLine.replace("|", random.nextBoolean() ? "||" : " "));
                break;
            case 4: // Command name mutation on one line
                lines.set(lineIdx, mutateCommandName(selectedLine));
                break;
            case 5: // Field Deletion on one line
                lines.set(lineIdx, deleteRandomField(selectedLine));
                break;
            case 6: // Payload mutation on one line
                lines.set(lineIdx, mutatePayload(selectedLine));
                break;
            case 7: // Insert an extra random line
                int insertAt = random.nextInt(lines.size() + 1);
                lines.add(insertAt, EXTRA_LINES[random.nextInt(EXTRA_LINES.length)]);
                break;
            case 8: // Swap line order
                if (lines.size() > 1) {
                    int otherIdx = random.nextInt(lines.size());
                    String tmp = lines.get(lineIdx);
                    lines.set(lineIdx, lines.get(otherIdx));
                    lines.set(otherIdx, tmp);
                }
                break;
            default:
                break;
        }

        return String.join("\n", lines); // Rejoin lines into a single seed.
    }

    private String mutateCommandName(String line) { // Helper function for multi-line mutation strat
        String[] parts = line.split("\\|");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            parts[0] = parts[0].toLowerCase();
            return String.join("|", parts);
        }
        return line;
    }

    private String deleteRandomField(String line) { // Helper function for multi-line mutation strat
        String[] fields = line.split("\\|");
        if (fields.length > 1) {
            int removeIdx = random.nextInt(fields.length);
            List<String> list = new ArrayList<>(Arrays.asList(fields));
            list.remove(removeIdx);
            return String.join("|", list);
        }
        return line;
    }

    private String mutatePayload(String line) { // Helper function for multi-line mutation strat
        String[] p = line.split("\\|");
        if (p.length <= 1) {
            return line;
        }

        int payloadIdx = 1;
        if (p[0].equals("PKT") && p.length > 5) {
            payloadIdx = 5;
        } else if (p[0].equals("MERGE") && p.length > 3) {
            payloadIdx = random.nextBoolean() ? 2 : 3;
        }

        String[] maliciousPayloads = {
                "",
                "A".repeat(2000),
                "!@#$%^&*()",
                "10.0.0.1",
                "DATA"
        };
        p[payloadIdx] = maliciousPayloads[random.nextInt(maliciousPayloads.length)];
        return String.join("|", p);
    }
}
