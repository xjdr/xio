package com.xjeffrose.xio.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A random number generator. Java's divergent interfaces
 * forces our hand here: ThreadLocalRandom does not conform
 * to java.util.Random. We bridge this gap.
 */
public interface RNG {
    /**
     * Generate a random Double between `0.0` and `1.0`, inclusive.
     */
    Double nextDouble();

    /**
     * Generate a random Int betwen 0 (inclusive) and `n` (exclusive).
     *
     * @param n the upper bound (exclusive). Must be a positive value.
     */
    int nextInt(int n );

    /**
     * Generate a random Int across the entire allowed integer values
     * from `Int.MinValue` to `Int.MaxValue`, inclusive.
     */
    int nextInt();

    /**
     * Generate a random Long betwen 0 (inclusive) and `n` (exclusive).
     *
     * @param n the upper bound (exclusive). Must be a positive value.
     */
    Long nextLong(Long n);
}

