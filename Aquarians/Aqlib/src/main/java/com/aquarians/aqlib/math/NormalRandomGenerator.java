package com.aquarians.aqlib.math;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Random;

public class NormalRandomGenerator {

    public NormalRandomGenerator(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public enum Algorithm {
        ONE,
        UNIFORM_0_1, // uniform between [0, 1]
        UNIFORM_M1_1, // uniform between [-1, 1]
        NORMAL_M1_1, // normal between [-1, 1]
        NORMAL_0_1, // normal between [0, 1]
    }

    private final Algorithm algorithm;
    private Random random = new Random();
    private NormalDistribution normal = new NormalDistribution();

    public double sample() {
        if (Algorithm.ONE == algorithm) {
            return 1.0;
        } else if (Algorithm.UNIFORM_0_1 == algorithm) {
            return random.nextDouble();
        } else if (Algorithm.UNIFORM_M1_1 == algorithm) {
            return 2.0 * random.nextDouble() - 1.0;
        } else if (Algorithm.NORMAL_M1_1 == algorithm) {
            double z = normal.sample();
            z = Math.max(z, -3.0);
            z = Math.min(z, 3.0);
            return ((z + 3.0) / 3.0) - 1.0;
        } else if (Algorithm.NORMAL_0_1 == algorithm) {
            double z = normal.sample();
            z = Math.max(z, -3.0);
            z = Math.min(z, 3.0);
            return (z + 3.0) / 6.0;
        }

        return 0.0;
    }

}
