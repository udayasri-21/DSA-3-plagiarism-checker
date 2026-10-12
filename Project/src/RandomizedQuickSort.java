// RandomizedQuickSort.java - Module 6: randomised quicksort. A random pivot gives expected O(n log n) time
// for every input. Sorts index numbers so that the largest key comes first.
public class RandomizedQuickSort {
    public static void sortDescending(int[] idx, double[] key) { sort(idx, key, 0, idx.length - 1); }

    private static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    private static void sort(int[] a, double[] key, int lo, int hi) {
        if (lo >= hi) return;
        swap(a, lo + RandomUtil.randomIndex(hi - lo + 1), hi);   // random pivot
        double pivot = key[a[hi]];
        int p = lo;
        for (int j = lo; j < hi; j++) if (key[a[j]] > pivot) swap(a, p++, j);
        swap(a, p, hi);
        sort(a, key, lo, p - 1);
        sort(a, key, p + 1, hi);
    }
}
