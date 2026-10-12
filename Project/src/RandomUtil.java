// RandomUtil.java - Module 6: randomised algorithms. Our own random generator and reservoir sampling.
public class RandomUtil {
    private static long rng = System.nanoTime() ^ 0x5DEECE66DL;

    // linear congruential generator
    public static synchronized long nextRandom() {
        rng = rng * 6364136223846793005L + 1442695040888963407L;
        return rng >>> 1;
    }

    // random number in [0, n)
    public static int randomIndex(int n) { return (int) ((nextRandom() >>> 16) % n); }

    // random long in [lo, hi]
    public static long randomRange(long lo, long hi) { return lo + nextRandom() % (hi - lo + 1); }

    // Reservoir sampling: picks one item uniformly from a stream of n items, seeing each item only once.
    // Item i replaces the chosen one with probability 1/(i+1), so every item ends with chance 1/n.
    public static int reservoirSample(int n) {
        int chosen = 0;
        for (int i = 1; i < n; i++) if (randomIndex(i + 1) == 0) chosen = i;
        return chosen;
    }
}
