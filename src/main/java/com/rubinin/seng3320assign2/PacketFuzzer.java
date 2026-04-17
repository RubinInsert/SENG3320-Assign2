package com.rubinin.seng3320assign2;

public class PacketFuzzer {
    public static void main(String[] args) {
        int totalExecutions = 5000;
        int currentExecs = 0;
        SeedManager seedManager = new SeedManager();
        Mutator mutator = new Mutator();
        CrashAnalyzer analyzer = new CrashAnalyzer();
        Runner packetRunner = new Runner(analyzer);
        while (currentExecs < totalExecutions) {
            // Seed Selection. I.e. choosing a syntactically correct input
            String seed = seedManager.getNextSeed();

            // Calculating how many times the seed should be mutated
            int energy = seedManager.calculateEnergy(seed);

            for (int e = 0; e < energy && currentExecs < totalExecutions; e++) {
                // For each "energy" perform a mutation
                String fuzzInput = mutator.mutate(seed);

                try {
                    // Executes against target (PacketLab)
                    packetRunner.executeTarget(currentExecs, fuzzInput);
                } catch (Exception ex) { /* Log error */ }

                currentExecs++;
            }
        }
    }
}