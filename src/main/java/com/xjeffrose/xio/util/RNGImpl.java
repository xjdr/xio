package com.xjeffrose.xio.util;

public class RNGImpl implements RNG {
  @Override
  public Double nextDouble() {
    return null;
  }

  @Override
  public int nextInt(int n) {
    return 0;
  }

  @Override
  public int nextInt() {
    return 0;
  }

  @Override
  public Long nextLong(Long n) {
    return null;
  }

//  /**
//   * See [[Rngs]] for Java compatible APIs.
//   */
//    def apply(): RNG = RNG(new java.util.Random)
//    def apply(seed: Long): RNG = RNG(new java.util.Random(seed))
//    def apply(r: scala.util.Random): RNG = RNG(r.self)
//    def apply(r: java.util.Random): RNG = new RNG {
//      def nextDouble(): Double = r.nextDouble()
//      def nextInt(n: Int): Int = r.nextInt(n)
//      def nextInt(): Int = r.nextInt()
//      def nextLong(n: Long): Long = {
//          require(n > 0)
//
//      // This is the algorithm used by Java's random number generator
//      // internally.
//      //   http://docs.oracle.com/javase/6/docs/api/java/util/Random.html#nextInt(int)
//      if ((n & -n) == n)
//        return r.nextLong() % n
//
//      var bits = 0L
//      var v = 0L
//      do {
//        bits = (r.nextLong() << 1) >>> 1
//        v = bits%n
//      } while (bits-v+(n-1) < 0L)
//      v
//      }
//    }
//    val threadLocal: RNG = new RNG {
//      def nextDouble(): Double = ThreadLocalRandom.current().nextDouble()
//      def nextInt(n: Int): Int = ThreadLocalRandom.current().nextInt(0, n)
//      def nextInt(): Int = ThreadLocalRandom.current().nextInt()
//      def nextLong(n: Long): Long = ThreadLocalRandom.current().nextLong(n)
//    }
//  }

  /** Java compatible forwarders. */
//  object Rngs {
//    val threadLocal: RNG = RNG.threadLocal
//  }
}
