// EditDistance.java - Module 3: Levenshtein edit distance by dynamic programming, on WORDS.
// Overlapping subproblems: d[i][j] = best cost to turn the first i words of A into the first j words of B.
// Only two rows are kept in memory, so the space is O(length).
public class EditDistance {
    public static int distance(int[] a, int as, int ae, int[] b, int bs, int be) {
        int la = ae - as, lb = be - bs;
        int[] prev = new int[lb + 1], cur = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            cur[0] = i;
            for (int j = 1; j <= lb; j++) {
                int cost = a[as + i - 1] == b[bs + j - 1] ? 0 : 1;
                int v = prev[j - 1] + cost;                  // replace or keep
                if (prev[j] + 1 < v) v = prev[j] + 1;        // delete
                if (cur[j - 1] + 1 < v) v = cur[j - 1] + 1;  // insert
                cur[j] = v;
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[lb];
    }

    // 1.0 = same sentence, 0.0 = nothing in common
    public static double similarity(int[] a, int as, int ae, int[] b, int bs, int be) {
        int la = ae - as, lb = be - bs;
        int longest = la > lb ? la : lb;
        if (longest == 0) return 1.0;
        return 1.0 - (double) distance(a, as, ae, b, bs, be) / longest;
    }
}
