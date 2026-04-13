package com.rubinin.seng3320assign2;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PacketFuzzer {
    private static final Random random = new Random();
    public static void main(String[] args) {
        int totalExecutions = 10000;
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

                // Writes to the input.txt
                packetRunner.writeInputToFile(fuzzInput);
                try {
                    // Executes against target (PacketLab)
                    packetRunner.executeTarget(currentExecs, fuzzInput);
                } catch (Exception ex) { /* Log error */ }

                currentExecs++;
            }
        }
    }
}