// MaxFlow.java - Module 4: maximum flow with Dinic's algorithm (blocking flows on level graphs).
// Used for BIPARTITE MATCHING of sentences: each uploaded sentence is paired with at most one reference
// sentence and each reference sentence is used at most once (source -> left -> right -> sink, all capacity 1).
public class MaxFlow {
    private final int n;
    private int edges = 0;
    private final int[] head, nxt, to, cap, level, iter;

    public MaxFlow(int nodes, int maxEdges) {
        n = nodes;
        head = new int[n]; level = new int[n]; iter = new int[n];
        for (int i = 0; i < n; i++) head[i] = -1;
        nxt = new int[maxEdges * 2]; to = new int[maxEdges * 2]; cap = new int[maxEdges * 2];
    }

    // adds edge u->v and its reverse edge; returns the index of the forward edge
    public int addEdge(int u, int v, int c) {
        to[edges] = v; cap[edges] = c; nxt[edges] = head[u]; head[u] = edges++;
        to[edges] = u; cap[edges] = 0; nxt[edges] = head[v]; head[v] = edges++;
        return edges - 2;
    }

    // flow that was sent through forward edge e (= capacity now stored on its reverse edge)
    public int flowOn(int e) { return cap[e ^ 1]; }

    private boolean bfs(int s, int t) {
        for (int i = 0; i < n; i++) level[i] = -1;
        int[] q = new int[n];
        int qh = 0, qt = 0;
        q[qt++] = s; level[s] = 0;
        while (qh < qt) {
            int u = q[qh++];
            for (int e = head[u]; e != -1; e = nxt[e])
                if (cap[e] > 0 && level[to[e]] < 0) { level[to[e]] = level[u] + 1; q[qt++] = to[e]; }
        }
        return level[t] >= 0;
    }

    private int dfs(int u, int t, int f) {
        if (u == t) return f;
        for (; iter[u] != -1; iter[u] = nxt[iter[u]]) {
            int e = iter[u];
            if (cap[e] > 0 && level[to[e]] == level[u] + 1) {
                int d = dfs(to[e], t, f < cap[e] ? f : cap[e]);
                if (d > 0) { cap[e] -= d; cap[e ^ 1] += d; return d; }
            }
        }
        return 0;
    }

    public int maxFlow(int s, int t) {
        int flow = 0;
        while (bfs(s, t)) {
            for (int i = 0; i < n; i++) iter[i] = head[i];
            int f;
            while ((f = dfs(s, t, Integer.MAX_VALUE)) > 0) flow += f;
        }
        return flow;
    }
}
