// Engine.java - the controller. It calls the syllabus algorithms (each one is in its own file):
//   MillerRabin (random prime) -> RabinKarp + PhraseTable (phrase matching) -> KMP (whole sentences)
//   -> SuffixArray (longest common passage) -> EditDistance + MaxFlow (re-worded sentences)
//   -> ParallelRunner (all reference files at the same time) -> RandomizedQuickSort (ranking)
public class Engine {

    static final int K = 5;              // phrase length in words
    static final double REWORD = 0.70;   // sentence similarity needed to call a sentence "re-worded"

    public static class Result {
        public boolean[] exact, reworded;           // per word of the uploaded file
        public int[] refKind;                       // per word of the REFERENCE file: 0 none, 1 exact, 2 re-worded
        public int exactCount, rewordCount, matched;
        public double percent;
        public int longestCommon, lcStart;          // longest common passage (words) and where it starts
        public int sentCopied, sentReworded;        // whole sentences found by KMP / matched by max-flow
    }

    public static class Report {
        public Doc suspect; public Doc[] refs; public Result[] res;
        public int[] order;                         // reference files, most similar first
        public int[] kind;                          // per word: 0 none, 1 exact copy, 2 re-worded
        public int[] src;                           // per word: rank of the file it came from, -1 if none
        public int exactTotal, rewordTotal;
        public int longestPassage, longestPassageRef;
        public double overallPercent;
        public long mod, base; public int workers;
    }

    public static String verdict(double p) { return p >= 50 ? "HIGH" : p >= 20 ? "MEDIUM" : "LOW"; }

    // ---------- compare the uploaded file with ONE reference file ----------
    static Result compare(Doc s, RabinKarp rk, PhraseTable table, Doc ref, int separator) {
        int n = s.ids.length;
        Result r = new Result();
        r.exact = new boolean[n];
        r.reworded = new boolean[n];
        r.refKind = new int[ref.ids.length];
        int S = s.sentStart.length - 1, R = ref.sentStart.length - 1;

        // 1) Module 2 - Rabin-Karp: identical K-word phrases
        long[] rh = rk.phraseHashes(ref.ids);
        for (int j = 0; j < rh.length; j++) {
            int slot = (int) (rh[j] % table.size);
            for (int i = table.head[slot]; i != -1; i = table.next[i])
                if (table.key[i] == rh[j] && rk.samePhrase(s.ids, i, ref.ids, j))
                    for (int x = 0; x < K; x++) { r.exact[i + x] = true; r.refKind[j + x] = 1; }
        }

        // 2) Module 2 - KMP: whole sentences copied word for word (also catches short sentences)
        for (int k = 0; k < S; k++) {
            int a = s.sentStart[k], b = s.sentStart[k + 1];
            if (b - a < 4) continue;
            int[] slice = new int[b - a];
            for (int i = a; i < b; i++) slice[i - a] = s.ids[i];
            int pos = KMP.indexOf(ref.ids, slice);
            if (pos >= 0) {
                r.sentCopied++;
                for (int i = a; i < b; i++) r.exact[i] = true;
                for (int i = pos; i < pos + slice.length; i++) r.refKind[i] = 1;
            }
        }

        // 3) Module 3 - edit distance finds candidate pairs of similar sentences
        int[] ci = new int[64], cq = new int[64];
        int cn = 0;
        for (int k = 0; k < S; k++) {
            int a = s.sentStart[k], b = s.sentStart[k + 1], len = b - a;
            if (len < 4) continue;
            int cov = 0;
            for (int i = a; i < b; i++) if (r.exact[i]) cov++;
            if (cov * 10 >= len * 9) continue;                         // already an exact copy
            for (int q = 0; q < R; q++) {
                int c = ref.sentStart[q], d = ref.sentStart[q + 1], lb = d - c;
                if (lb * 2 < len || len * 2 < lb) continue;
                if (EditDistance.similarity(s.ids, a, b, ref.ids, c, d) >= REWORD) {
                    if (cn == ci.length) {
                        int[] n1 = new int[cn * 2], n2 = new int[cn * 2];
                        for (int z = 0; z < cn; z++) { n1[z] = ci[z]; n2[z] = cq[z]; }
                        ci = n1; cq = n2;
                    }
                    ci[cn] = k; cq[cn] = q; cn++;
                }
            }
        }

        // 4) Module 4 - max-flow (bipartite matching): one uploaded sentence <-> one reference sentence
        if (cn > 0) {
            int src = 0, sink = 1 + S + R;
            MaxFlow g = new MaxFlow(sink + 1, S + R + cn);
            for (int k = 0; k < S; k++) g.addEdge(src, 1 + k, 1);
            for (int q = 0; q < R; q++) g.addEdge(1 + S + q, sink, 1);
            int[] edge = new int[cn];
            for (int c = 0; c < cn; c++) edge[c] = g.addEdge(1 + ci[c], 1 + S + cq[c], 1);
            g.maxFlow(src, sink);
            for (int c = 0; c < cn; c++) {
                if (g.flowOn(edge[c]) != 1) continue;
                r.sentReworded++;
                for (int i = s.sentStart[ci[c]]; i < s.sentStart[ci[c] + 1]; i++) if (!r.exact[i]) r.reworded[i] = true;
                for (int i = ref.sentStart[cq[c]]; i < ref.sentStart[cq[c] + 1]; i++) if (r.refKind[i] == 0) r.refKind[i] = 2;
            }
        }

        // 5) Module 2 - suffix array + LCP: the longest passage both files share
        int[] lc = SuffixArray.longestCommon(s.ids, ref.ids, separator);
        r.longestCommon = lc[0];
        r.lcStart = lc[1];

        for (int i = 0; i < n; i++) { if (r.exact[i]) r.exactCount++; else if (r.reworded[i]) r.rewordCount++; }
        r.matched = r.exactCount + r.rewordCount;
        r.percent = n == 0 ? 0 : 100.0 * r.matched / n;
        return r;
    }

    // ---------- check one uploaded text against all reference texts ----------
    public static Report analyze(String suspectName, String suspectText, String[] refNames, String[] refTexts)
            throws InterruptedException {
        // Module 5/6 - random prime modulus (Miller-Rabin) and random base: universal-hashing idea
        long mod = MillerRabin.randomPrime(500000000L, 1000000000L);
        long base = RandomUtil.randomRange(1000, mod - 1000);
        final RabinKarp rk = new RabinKarp(mod, base, K);

        WordDictionary dict = new WordDictionary();
        final Doc s = TextProcessor.build(suspectName, suspectText, dict);
        final int m = refNames.length;
        final Doc[] refs = new Doc[m];
        for (int i = 0; i < m; i++) refs[i] = TextProcessor.build(refNames[i], refTexts[i], dict);
        final int separator = dict.size();                              // a number no word has

        final PhraseTable table = new PhraseTable(rk.phraseHashes(s.ids));
        final Result[] res = new Result[m];
        final int workers = m < 8 ? m : 8;
        ParallelRunner.forEach(m, workers, i -> res[i] = compare(s, rk, table, refs[i], separator));  // Module 6

        Report rep = new Report();
        rep.suspect = s; rep.refs = refs; rep.res = res; rep.mod = mod; rep.base = base; rep.workers = workers;

        int[] order = new int[m];
        double[] key = new double[m];
        for (int i = 0; i < m; i++) { order[i] = i; key[i] = res[i].percent; }
        RandomizedQuickSort.sortDescending(order, key);                 // Module 6
        rep.order = order;

        int n = s.ids.length;
        rep.kind = new int[n]; rep.src = new int[n];
        for (int i = 0; i < n; i++) rep.src[i] = -1;
        for (int rank = 0; rank < m; rank++) {
            Result r = res[order[rank]];
            for (int i = 0; i < n; i++)
                if (rep.src[i] == -1 && (r.exact[i] || r.reworded[i])) { rep.src[i] = rank; rep.kind[i] = r.exact[i] ? 1 : 2; }
        }
        for (int i = 0; i < n; i++) { if (rep.kind[i] == 1) rep.exactTotal++; else if (rep.kind[i] == 2) rep.rewordTotal++; }
        rep.overallPercent = n == 0 ? 0 : 100.0 * (rep.exactTotal + rep.rewordTotal) / n;
        for (int i = 0; i < m; i++) if (res[i].longestCommon > rep.longestPassage) { rep.longestPassage = res[i].longestCommon; rep.longestPassageRef = i; }
        return rep;
    }

    // =====================================================================================
    // WHOLE CLASS: every student is compared with every other student (all pairs, in parallel)
    // =====================================================================================
    public static class ClassReport {
        public Doc[] docs;
        public Result[][] res;             // res[i][j] = how much of student i matches student j
        public int[][] order;              // for student i: the other students, most similar first
        public int[][] kind, src;          // per student, per word: 0/1/2 and the student it matches
        public int[] exactTotal, rewordTotal;
        public double[] overall;           // % of student i's words that match ANY other student
        public long mod, base; public int workers;
    }

    public static ClassReport analyzeClass(String[] names, String[] texts) throws InterruptedException {
        long mod = MillerRabin.randomPrime(500000000L, 1000000000L);
        long base = RandomUtil.randomRange(1000, mod - 1000);
        final RabinKarp rk = new RabinKarp(mod, base, K);
        WordDictionary dict = new WordDictionary();
        final int N = names.length;
        final Doc[] docs = new Doc[N];
        for (int i = 0; i < N; i++) docs[i] = TextProcessor.build(names[i], texts[i], dict);
        final int sep = dict.size();
        final PhraseTable[] tables = new PhraseTable[N];
        for (int i = 0; i < N; i++) tables[i] = new PhraseTable(rk.phraseHashes(docs[i].ids));

        final Result[][] res = new Result[N][N];
        final int workers = N * N < 8 ? N * N : 8;
        ParallelRunner.forEach(N * N, workers, idx -> {                 // Module 6: N x N comparisons in parallel
            int i = idx / N, j = idx % N;
            if (i != j) res[i][j] = compare(docs[i], rk, tables[i], docs[j], sep);
        });

        ClassReport c = new ClassReport();
        c.docs = docs; c.res = res; c.mod = mod; c.base = base; c.workers = workers;
        c.order = new int[N][]; c.kind = new int[N][]; c.src = new int[N][];
        c.exactTotal = new int[N]; c.rewordTotal = new int[N]; c.overall = new double[N];
        for (int i = 0; i < N; i++) {
            int[] ord = new int[N - 1];
            double[] key = new double[N];
            int k = 0;
            for (int j = 0; j < N; j++) if (j != i) { ord[k++] = j; key[j] = res[i][j].percent; }
            RandomizedQuickSort.sortDescending(ord, key);
            c.order[i] = ord;
            int n = docs[i].ids.length;
            c.kind[i] = new int[n]; c.src[i] = new int[n];
            for (int w = 0; w < n; w++) c.src[i][w] = -1;
            for (int q = 0; q < ord.length; q++) {
                Result r = res[i][ord[q]];
                for (int w = 0; w < n; w++)
                    if (c.src[i][w] == -1 && (r.exact[w] || r.reworded[w])) { c.src[i][w] = ord[q]; c.kind[i][w] = r.exact[w] ? 1 : 2; }
            }
            for (int w = 0; w < n; w++) { if (c.kind[i][w] == 1) c.exactTotal[i]++; else if (c.kind[i][w] == 2) c.rewordTotal[i]++; }
            c.overall[i] = n == 0 ? 0 : 100.0 * (c.exactTotal[i] + c.rewordTotal[i]) / n;
        }
        return c;
    }
}
