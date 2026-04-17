package com.rubinin.seng3320assign2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SeedManager {
    private final Random random = new Random();
    // Split the seeds into seperate arrays
    private static final List<String> packetSeeds = Arrays.asList(
            "PKT|10.0.0.1|10.0.0.2|DATA|4|ABCD",
            "PKT|10.0.0.2|10.0.0.3|CTRL|3|XYZ",
            "PKT|10.0.0.1|10.0.0.4|DATA|5|HELLO",
            "PKT|192.168.0.10|10.0.0.5|DATA|2|OK"
    );

    private static final List<String> directiveSeeds = Arrays.asList(
            "COUNT|DATA",
            "AVG|10.0.0.1",
            "SLICE|NETWORK|1|4",
            "MERGE|-|foo|bar",
            "REPEAT|AB|3",
            "LOOKUP|10.0.0.1",
            "WINDOW|5",
            "HEX|ff",
            "TOP|2"
    );

    private static final List<String> maliciousLines = Arrays.asList( // Contain additional directives that are more likely to cause crashes
            "AVG|99.99.99.99",
            "SPIN|-1",
            "LOOKUP|1.2.3.4",
            "MERGE||a|b",
            "WINDOW|10",
            "REPEAT|A|-1",
            "TOP|9999",
            "HI"
    );

    public String getNextSeed() {
        List<String> lines = new ArrayList<>();

        // Build program-like inputs with multiple lines, not single standalone commands.
        int packetCount = 1 + random.nextInt(3);
        int directiveCount = 1 + random.nextInt(5);

        for (int i = 0; i < packetCount; i++) {
            lines.add(packetSeeds.get(random.nextInt(packetSeeds.size())));
        }

        for (int i = 0; i < directiveCount; i++) {
            boolean chooseMalicious = random.nextInt(100) < 25;
            if (chooseMalicious) {
                lines.add(maliciousLines.get(random.nextInt(maliciousLines.size())));
            } else {
                lines.add(directiveSeeds.get(random.nextInt(directiveSeeds.size())));
            }
        }

        if (random.nextBoolean()) {
            Collections.shuffle(lines, random);
        }

        return String.join("\n", lines); // Join lines back into a contiguous seed
    }

    public int calculateEnergy(String seed) {
        int lineCount = Math.max(1, seed.split("\\R").length); // Split by any linebreaks
        return Math.max(10, lineCount * 6);
    }
}
