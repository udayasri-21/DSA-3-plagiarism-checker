// WordDictionary.java - Module 1/2: our own hash table (open addressing) that gives every word a number.
public class WordDictionary {
    private String[] keys = new String[1024];
    private int[] vals = new int[1024];
    private int count = 0;

    private static int hash(String w) {
        int h = 0;
        for (int i = 0; i < w.length(); i++) h = h * 31 + w.charAt(i);
        return h & 0x7fffffff;
    }

    public int size() { return count; }

    // returns the number of the word; gives a new number if the word is new
    public int idOf(String w) {
        if (count * 2 >= keys.length) grow();
        int i = hash(w) % keys.length;
        while (keys[i] != null) {
            if (keys[i].equals(w)) return vals[i];
            i = (i + 1) % keys.length;
        }
        keys[i] = w; vals[i] = count;
        return count++;
    }

    private void grow() {
        String[] ok = keys; int[] ov = vals;
        keys = new String[ok.length * 2]; vals = new int[ok.length * 2];
        for (int j = 0; j < ok.length; j++) {
            if (ok[j] == null) continue;
            int i = hash(ok[j]) % keys.length;
            while (keys[i] != null) i = (i + 1) % keys.length;
            keys[i] = ok[j]; vals[i] = ov[j];
        }
    }
}
