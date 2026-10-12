// PhraseTable.java - Module 2: hash table with chaining. Phrase hash -> positions of that phrase in the uploaded file.
public class PhraseTable {
    public final int size;
    public final int[] head;   // first position stored in each slot (-1 = empty)
    public final int[] next;   // next position in the same slot (the chain)
    public final long[] key;   // hash of the phrase starting at each position

    public PhraseTable(long[] hashes) {
        size = 2 * hashes.length + 1;
        head = new int[size];
        next = new int[hashes.length];
        key = hashes;
        for (int i = 0; i < size; i++) head[i] = -1;
        for (int i = 0; i < hashes.length; i++) {
            int slot = (int) (hashes[i] % size);
            next[i] = head[slot];
            head[slot] = i;
        }
    }
}
