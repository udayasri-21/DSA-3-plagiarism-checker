// MillerRabin.java - Module 5/6: randomised primality test (Monte Carlo).
// Used to choose a random PRIME modulus for the Rabin-Karp hash in every run.
public class MillerRabin {
    static long modPow(long b, long e, long m) {
        long r = 1; b %= m;
        while (e > 0) {
            if ((e & 1) == 1) r = r * b % m;
            b = b * b % m;
            e >>= 1;
        }
        return r;
    }

    // n must be below 2^31 so that products fit in a long
    public static boolean isPrime(long n, int rounds) {
        if (n < 2) return false;
        if (n % 2 == 0) return n == 2;
        if (n % 3 == 0) return n == 3;
        long d = n - 1; int s = 0;
        while (d % 2 == 0) { d /= 2; s++; }
        for (int round = 0; round < rounds; round++) {
            long a = RandomUtil.randomRange(2, n - 2);        // random witness
            long x = modPow(a, d, n);
            if (x == 1 || x == n - 1) continue;
            boolean composite = true;
            for (int i = 1; i < s; i++) {
                x = x * x % n;
                if (x == n - 1) { composite = false; break; }
            }
            if (composite) return false;
        }
        return true;                                           // prime with very high probability
    }

    public static long randomPrime(long lo, long hi) {
        while (true) {
            long c = RandomUtil.randomRange(lo, hi) | 1;
            if (isPrime(c, 12)) return c;
        }
    }
}
