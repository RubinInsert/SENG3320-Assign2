package com.rubinin.seng3320assign2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SeedManager {
    private final Random random = new Random();
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
    public String getNextSeed() {
        return seedCorpus.get(random.nextInt(seedCorpus.size()));
    }
    public int calculateEnergy(String seed) {
        // More complex (longer) seeds get more "energy" (more mutation attempts)
        return Math.max(10, seed.length() / 2);
    }
}
