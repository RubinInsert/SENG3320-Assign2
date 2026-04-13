package com.rubinin.seng3320assign2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Mutator {
    private final Random random = new Random();
    private static final Integer[] INT_PARTITIONS = new Integer[] {Integer.MAX_VALUE, 0, -1};
    public String mutate(String seed) {
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
}
