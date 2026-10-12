// Doc.java - one document after pre-processing: words, word numbers and sentence boundaries.
public class Doc {
    public String name;
    public String text;        // the original text (used to show highlights)
    public String[] words;     // lower-case words
    public int[] ids;          // word numbers (same word = same number), from WordDictionary
    public int[] wStart, wEnd; // where each word starts and ends in the original text
    public int[] sentStart;    // sentence k = words[sentStart[k] .. sentStart[k+1])
}
