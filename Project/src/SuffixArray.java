// SuffixArray.java - Module 2: suffix array (prefix doubling) and LCP array (Kasai, linear time).
// Used to find the LONGEST PASSAGE (longest run of words) that two documents share.
public class SuffixArray {

    // ---- suffix array by prefix doubling: sort suffixes by their first 1, 2, 4, 8 ... words ----
    public static int[] build(int[] s) {
        int n = s.length;
        int[] sa = new int[n], rank = new int[n], tmp = new int[n], buf = new int[n];
        for (int i = 0; i < n; i++) { sa[i] = i; rank[i] = s[i]; }
        for (int k = 1; ; k <<= 1) {
            mergeSort(sa, buf, 0, n - 1, rank, k, n);
            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++) tmp[sa[i]] = tmp[sa[i - 1]] + (compare(sa[i - 1], sa[i], rank, k, n) < 0 ? 1 : 0);
            for (int i = 0; i < n; i++) rank[i] = tmp[i];
            if (rank[sa[n - 1]] == n - 1 || k >= n) break;
        }
        return sa;
    }

    static int compare(int a, int b, int[] rank, int k, int n) {
        if (rank[a] != rank[b]) return rank[a] - rank[b];
        int ra = a + k < n ? rank[a + k] : -1;
        int rb = b + k < n ? rank[b + k] : -1;
        return ra - rb;
    }

    static void mergeSort(int[] a, int[] buf, int lo, int hi, int[] rank, int k, int n) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        mergeSort(a, buf, lo, mid, rank, k, n);
        mergeSort(a, buf, mid + 1, hi, rank, k, n);
        int i = lo, j = mid + 1, t = lo;
        while (i <= mid && j <= hi) buf[t++] = compare(a[i], a[j], rank, k, n) <= 0 ? a[i++] : a[j++];
        while (i <= mid) buf[t++] = a[i++];
        while (j <= hi) buf[t++] = a[j++];
        for (t = lo; t <= hi; t++) a[t] = buf[t];
    }

    // ---- LCP array (Kasai): lcp[r] = common prefix length of suffix sa[r-1] and suffix sa[r] ----
    public static int[] lcp(int[] s, int[] sa) {
        int n = s.length;
        int[] inv = new int[n], lcp = new int[n];
        for (int i = 0; i < n; i++) inv[sa[i]] = i;
        int h = 0;
        for (int i = 0; i < n; i++) {
            if (inv[i] > 0) {
                int j = sa[inv[i] - 1];
                while (i + h < n && j + h < n && s[i + h] == s[j + h]) h++;
                lcp[inv[i]] = h;
                if (h > 0) h--;
            } else h = 0;
        }
        return lcp;
    }

    // Longest run of words that appears in BOTH a and b.
    // Joins a + separator + b, builds the suffix array and LCP, then looks at neighbouring suffixes
    // where one starts in a and the other in b. Returns {length, startInA, startInB}.
    public static int[] longestCommon(int[] a, int[] b, int separator) {
        int na = a.length, nb = b.length;
        if (na == 0 || nb == 0) return new int[] {0, 0, 0};
        int[] s = new int[na + 1 + nb];
        for (int i = 0; i < na; i++) s[i] = a[i];
        s[na] = separator;                                   // a number that appears nowhere else
        for (int i = 0; i < nb; i++) s[na + 1 + i] = b[i];
        int[] sa = build(s);
        int[] lcp = lcp(s, sa);
        int best = 0, sa0 = 0, sb0 = 0;
        for (int r = 1; r < s.length; r++) {
            int x = sa[r - 1], y = sa[r];
            boolean xa = x < na, ya = y < na;
            if (xa == ya || x == na || y == na) continue;    // need one suffix from each document
            if (lcp[r] > best) {
                best = lcp[r];
                sa0 = xa ? x : y;
                sb0 = (xa ? y : x) - na - 1;
            }
        }
        return new int[] {best, sa0, sb0};
    }
}
