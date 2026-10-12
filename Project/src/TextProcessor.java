// TextProcessor.java - Module 1: reading text: lower-case, remove punctuation, split into words and sentences.
public class TextProcessor {
    public static Doc build(String name, String text, WordDictionary dict) {
        String[] tmp = new String[text.length() / 2 + 2];
        int[] ws = new int[text.length() / 2 + 2], we = new int[text.length() / 2 + 2];
        int[] ss = new int[text.length() / 2 + 3];
        int n = 0, ns = 0, cur = 0;
        StringBuilder w = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            char ch = (i < text.length()) ? Character.toLowerCase(text.charAt(i)) : '\n';
            if (Character.isLetterOrDigit(ch)) { if (w.length() == 0) cur = i; w.append(ch); continue; }
            if (w.length() > 0) { tmp[n] = w.toString(); ws[n] = cur; we[n] = i; n++; w.setLength(0); }
            boolean end = (ch == '.' || ch == '!' || ch == '?' || ch == '\n');
            if (end && n > ss[ns]) { ns++; ss[ns] = n; }
        }
        Doc d = new Doc();
        d.name = name;
        d.text = text;
        d.words = new String[n];
        d.ids = new int[n];
        d.wStart = new int[n];
        d.wEnd = new int[n];
        for (int i = 0; i < n; i++) { d.words[i] = tmp[i]; d.ids[i] = dict.idOf(tmp[i]); d.wStart[i] = ws[i]; d.wEnd[i] = we[i]; }
        d.sentStart = new int[ns + 1];
        for (int i = 0; i <= ns; i++) d.sentStart[i] = ss[i];
        return d;
    }
}
