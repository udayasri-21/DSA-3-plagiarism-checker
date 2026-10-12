// RabinKarp.java - Module 2: Rabin-Karp with a polynomial rolling hash.
// Every window of K words gets a number. Moving the window by one word takes O(1) time:
//   H_new = ((H_old - first * B^(K-1)) * B + new) mod M
public class RabinKarp {
    private final long mod, base, pow;
    private final int k;

    public RabinKarp(long mod, long base, int k) {
        this.mod = mod; this.base = base; this.k = k;
        long p = 1;
        for (int i = 0; i < k - 1; i++) p = p * base % mod;
        this.pow = p;
    }

    public long getMod() { return mod; }
    public long getBase() { return base; }

    // hash of every K-word phrase: result[i] = hash of ids[i .. i+K-1]
    public long[] phraseHashes(int[] ids) {
        int n = ids.length;
        if (n < k) return new long[0];
        long[] out = new long[n - k + 1];
        long h = 0;
        for (int i = 0; i < k; i++) h = (h * base + ids[i] + 1) % mod;
        out[0] = h;
        for (int i = 1; i + k <= n; i++) {
            h = (h - (ids[i - 1] + 1) * pow % mod + mod) % mod;    // remove the left word
            h = (h * base + ids[i + k - 1] + 1) % mod;            // add the right word
            out[i] = h;
        }
        return out;
    }

    // when two hashes are equal we still compare the words (removes false matches caused by collisions)
    public boolean samePhrase(int[] a, int i, int[] b, int j) {
        for (int x = 0; x < k; x++) if (a[i + x] != b[j + x]) return false;
        return true;
    }
}
