// KMP.java - Module 2: Knuth-Morris-Pratt string matching (failure function), O(n + m).
// Used to find a WHOLE sentence of the uploaded file inside a reference file.
public class KMP {
    // failure[i] = length of the longest proper prefix of p[0..i] that is also a suffix of it
    public static int[] failure(int[] p) {
        int[] f = new int[p.length];
        int k = 0;
        for (int i = 1; i < p.length; i++) {
            while (k > 0 && p[i] != p[k]) k = f[k - 1];
            if (p[i] == p[k]) k++;
            f[i] = k;
        }
        return f;
    }

    // position where the pattern first appears in the text, or -1
    public static int indexOf(int[] text, int[] p) {
        if (p.length == 0 || p.length > text.length) return -1;
        int[] f = failure(p);
        int k = 0;
        for (int i = 0; i < text.length; i++) {
            while (k > 0 && text[i] != p[k]) k = f[k - 1];
            if (text[i] == p[k]) k++;
            if (k == p.length) return i - p.length + 1;
        }
        return -1;
    }
}
