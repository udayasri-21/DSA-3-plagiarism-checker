// Server.java - backend. Serves the web page and the API. (java.util is used only here, not in the algorithms.)
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.*;

public class Server {

    static class Item { String name; byte[] data; String text; }

    static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else if (c == '\t') b.append("\\t");
            else if (c < 32) b.append(' ');
            else b.append(c);
        }
        return b.append('"').toString();
    }

    static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        OutputStream os = ex.getResponseBody();
        os.write(body);
        os.close();
    }

    static void error(HttpExchange ex, String msg) throws IOException {
        send(ex, 400, "application/json; charset=utf-8", ("{\"error\":" + json(msg) + "}").getBytes(StandardCharsets.UTF_8));
    }

    static String param(String query, String key) throws IOException {
        if (query == null) return "";
        for (String p : query.split("&")) {
            int e = p.indexOf('=');
            if (e > 0 && p.substring(0, e).equals(key)) return URLDecoder.decode(p.substring(e + 1), "UTF-8");
        }
        return "";
    }

    // ---- read .txt and .docx ----
    static String readDocx(byte[] data) throws IOException {
        ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(data));
        ZipEntry e;
        while ((e = z.getNextEntry()) != null) {
            if (e.getName().equals("word/document.xml")) {
                String xml = new String(z.readAllBytes(), StandardCharsets.UTF_8);
                xml = xml.replace("</w:p>", "\n").replace("<w:br/>", "\n").replace("<w:tab/>", " ");
                xml = xml.replaceAll("<[^>]+>", "");
                return xml.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
            }
        }
        return "";
    }

    static String toText(String name, byte[] data) throws IOException {
        if (name.toLowerCase().endsWith(".docx")) return readDocx(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    // highlighted ranges of a document: [[startChar, endChar, kind, source], ...]  (kind 1 = exact, 2 = re-worded)
    static String ranges(Doc d, int[] kind, int[] src) {
        StringBuilder sb = new StringBuilder("[");
        int n = d.words.length, i = 0;
        boolean first = true;
        while (i < n) {
            if (kind[i] == 0) { i++; continue; }
            int j = i, s = src == null ? 0 : src[i];
            while (j + 1 < n && kind[j + 1] == kind[i] && (src == null ? 0 : src[j + 1]) == s) j++;
            if (!first) sb.append(',');
            first = false;
            sb.append('[').append(d.wStart[i]).append(',').append(d.wEnd[j]).append(',').append(kind[i]).append(',').append(s).append(']');
            i = j + 1;
        }
        return sb.append(']').toString();
    }

    // Body: one line per file ->  role|name|base64(file bytes)      role = s (file to check) or r (comparison file)
    static void check(HttpExchange ex) throws Exception {
        boolean random = param(ex.getRequestURI().getRawQuery(), "random").equals("1");
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Item suspect = null;
        ArrayList<Item> refs = new ArrayList<>();
        int unreadable = 0;
        for (String line : body.split("\n")) {
            line = line.trim();
            int a = line.indexOf('|'), b = line.lastIndexOf('|');
            if (a < 0 || b <= a) continue;
            Item it = new Item();
            it.name = line.substring(a + 1, b);
            it.data = Base64.getDecoder().decode(line.substring(b + 1));
            try { it.text = toText(it.name, it.data); } catch (Exception e) { unreadable++; continue; }
            if (line.charAt(0) == 's') suspect = it; else refs.add(it);
        }
        if (random) {
            if (refs.size() < 2) { error(ex, "Choose a folder that has at least 2 files."); return; }
            suspect = refs.remove(RandomUtil.reservoirSample(refs.size()));       // Module 6: reservoir sampling
        }
        if (suspect == null) { error(ex, "Choose the file you want to check."); return; }
        if (suspect.text.trim().isEmpty()) { error(ex, "The file you submitted has no readable text."); return; }

        int selfSkipped = 0;                                    // the submitted file may also be inside the chosen folder
        for (int i = refs.size() - 1; i >= 0; i--) {
            Item r = refs.get(i);
            if (r.text.trim().isEmpty()) { refs.remove(i); unreadable++; continue; }
            String base = r.name.contains("/") ? r.name.substring(r.name.lastIndexOf('/') + 1) : r.name;
            if (base.equals(suspect.name) && Arrays.equals(r.data, suspect.data)) { refs.remove(i); selfSkipped++; }
        }
        int m = refs.size();
        if (m < 1) { error(ex, "The chosen folder has no readable .txt or .docx files to compare with."); return; }
        String[] names = new String[m], texts = new String[m];
        for (int i = 0; i < m; i++) { names[i] = refs.get(i).name; texts[i] = refs.get(i).text; }

        long t0 = System.nanoTime();
        Engine.Report r = Engine.analyze(suspect.name, suspect.text, names, texts);
        double ms = (System.nanoTime() - t0) / 1e6;

        int n = r.suspect.words.length;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"suspect\":").append(json(suspect.name));
        sb.append(",\"totalWords\":").append(n);
        sb.append(",\"refCount\":").append(m);
        sb.append(",\"random\":").append(random);
        sb.append(",\"selfSkipped\":").append(selfSkipped).append(",\"unreadable\":").append(unreadable);
        sb.append(",\"ms\":").append(String.format(java.util.Locale.ROOT, "%.1f", ms));
        sb.append(",\"overallPercent\":").append(String.format(java.util.Locale.ROOT, "%.1f", r.overallPercent));
        sb.append(",\"overallVerdict\":").append(json(Engine.verdict(r.overallPercent)));
        sb.append(",\"exactWords\":").append(r.exactTotal);
        sb.append(",\"rewordWords\":").append(r.rewordTotal);
        sb.append(",\"longestRun\":").append(r.longestPassage);
        sb.append(",\"longestRunSrc\":").append(r.longestPassage > 0 ? json(r.refs[r.longestPassageRef].name) : "\"\"");
        sb.append(",\"mod\":").append(r.mod).append(",\"base\":").append(r.base).append(",\"workers\":").append(r.workers);
        int sc = 0, sr = 0;
        for (int i = 0; i < r.res.length; i++) { sc += r.res[i].sentCopied; sr += r.res[i].sentReworded; }
        sb.append(",\"sentCopied\":").append(sc).append(",\"sentReworded\":").append(sr);
        sb.append(",\"text\":").append(json(r.suspect.text));
        sb.append(",\"allRanges\":").append(ranges(r.suspect, r.kind, r.src));
        sb.append(",\"results\":[");
        for (int rank = 0; rank < r.order.length; rank++) {
            int d = r.order[rank];
            Engine.Result x = r.res[d];
            if (rank > 0) sb.append(',');
            int[] sk = new int[n];
            for (int i = 0; i < n; i++) sk[i] = x.exact[i] ? 1 : x.reworded[i] ? 2 : 0;
            StringBuilder ps = new StringBuilder();
            for (int i = x.lcStart; i < x.lcStart + x.longestCommon && i < x.lcStart + 25; i++) ps.append(r.suspect.words[i]).append(' ');
            if (x.longestCommon > 25) ps.append("...");
            sb.append("{\"name\":").append(json(r.refs[d].name));
            sb.append(",\"refWords\":").append(r.refs[d].words.length);
            sb.append(",\"matched\":").append(x.matched);
            sb.append(",\"exact\":").append(x.exactCount);
            sb.append(",\"reworded\":").append(x.rewordCount);
            sb.append(",\"percent\":").append(String.format(java.util.Locale.ROOT, "%.1f", x.percent));
            sb.append(",\"longestRun\":").append(x.longestCommon);
            sb.append(",\"sentCopied\":").append(x.sentCopied).append(",\"sentReworded\":").append(x.sentReworded);
            sb.append(",\"passage\":").append(json(ps.toString().trim()));
            sb.append(",\"verdict\":").append(json(Engine.verdict(x.percent)));
            sb.append(",\"sr\":").append(ranges(r.suspect, sk, null));
            sb.append(",\"refText\":").append(x.matched > 0 ? json(r.refs[d].text) : "\"\"");
            sb.append(",\"rr\":").append(x.matched > 0 ? ranges(r.refs[d], x.refKind, null) : "[]");
            sb.append("}");
        }
        sb.append("]}");
        send(ex, 200, "application/json; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ---------- whole class: every student against every other student ----------
    static void classCheck(HttpExchange ex) throws Exception {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        ArrayList<Item> list = new ArrayList<>();
        int unreadable = 0;
        for (String line : body.split("\n")) {
            line = line.trim();
            int a = line.indexOf('|'), b = line.lastIndexOf('|');
            if (a < 0 || b <= a) continue;
            Item it = new Item();
            it.name = line.substring(a + 1, b);
            it.data = Base64.getDecoder().decode(line.substring(b + 1));
            try { it.text = toText(it.name, it.data); } catch (Exception e) { unreadable++; continue; }
            if (it.text.trim().isEmpty()) { unreadable++; continue; }
            list.add(it);
        }
        int N = list.size();
        if (N < 2) { error(ex, "Choose a folder that has at least 2 readable .txt or .docx files."); return; }
        String[] names = new String[N], texts = new String[N];
        for (int i = 0; i < N; i++) { names[i] = list.get(i).name; texts[i] = list.get(i).text; }

        long t0 = System.nanoTime();
        Engine.ClassReport c = Engine.analyzeClass(names, texts);
        double ms = (System.nanoTime() - t0) / 1e6;

        int copied = 0, high = 0;
        for (int i = 0; i < N; i++) { if (c.overall[i] >= 20) copied++; if (c.overall[i] >= 50) high++; }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"count\":").append(N).append(",\"unreadable\":").append(unreadable);
        sb.append(",\"ms\":").append(String.format(java.util.Locale.ROOT, "%.1f", ms));
        sb.append(",\"copied\":").append(copied).append(",\"high\":").append(high);
        sb.append(",\"mod\":").append(c.mod).append(",\"base\":").append(c.base).append(",\"workers\":").append(c.workers);
        sb.append(",\"names\":[");
        for (int i = 0; i < N; i++) { if (i > 0) sb.append(','); sb.append(json(names[i])); }
        sb.append("],\"texts\":[");
        for (int i = 0; i < N; i++) { if (i > 0) sb.append(','); sb.append(json(texts[i])); }
        sb.append("],\"students\":[");
        for (int i = 0; i < N; i++) {
            if (i > 0) sb.append(',');
            Doc d = c.docs[i];
            sb.append("{\"name\":").append(json(names[i]));
            sb.append(",\"words\":").append(d.words.length);
            sb.append(",\"matched\":").append(c.exactTotal[i] + c.rewordTotal[i]);
            sb.append(",\"exact\":").append(c.exactTotal[i]).append(",\"reworded\":").append(c.rewordTotal[i]);
            sb.append(",\"overall\":").append(String.format(java.util.Locale.ROOT, "%.1f", c.overall[i]));
            sb.append(",\"verdict\":").append(json(Engine.verdict(c.overall[i])));
            sb.append(",\"sources\":[");
            boolean f = true;
            for (int q = 0; q < c.order[i].length; q++) {
                int j = c.order[i][q];
                Engine.Result x = c.res[i][j];
                if (x.matched == 0) break;
                if (!f) sb.append(',');
                f = false;
                sb.append("{\"j\":").append(j).append(",\"percent\":").append(String.format(java.util.Locale.ROOT, "%.1f", x.percent)).append(",\"matched\":").append(x.matched).append('}');
            }
            sb.append("],\"ar\":").append(ranges(d, c.kind[i], c.src[i])).append('}');
        }
        sb.append("],\"matrix\":[");
        for (int i = 0; i < N; i++) {
            if (i > 0) sb.append(',');
            sb.append('[');
            for (int j = 0; j < N; j++) {
                if (j > 0) sb.append(',');
                sb.append(i == j ? "-1" : String.format(java.util.Locale.ROOT, "%.1f", c.res[i][j].percent));
            }
            sb.append(']');
        }
        sb.append("],\"pairs\":[");
        boolean first = true;
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
            if (i == j || c.res[i][j].matched == 0) continue;
            Engine.Result x = c.res[i][j];
            int n = c.docs[i].words.length;
            int[] sk = new int[n];
            for (int w = 0; w < n; w++) sk[w] = x.exact[w] ? 1 : x.reworded[w] ? 2 : 0;
            StringBuilder ps = new StringBuilder();
            for (int w = x.lcStart; w < x.lcStart + x.longestCommon && w < x.lcStart + 25; w++) ps.append(c.docs[i].words[w]).append(' ');
            if (x.longestCommon > 25) ps.append("...");
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"i\":").append(i).append(",\"j\":").append(j);
            sb.append(",\"matched\":").append(x.matched).append(",\"exact\":").append(x.exactCount).append(",\"reworded\":").append(x.rewordCount);
            sb.append(",\"percent\":").append(String.format(java.util.Locale.ROOT, "%.1f", x.percent));
            sb.append(",\"longestRun\":").append(x.longestCommon).append(",\"passage\":").append(json(ps.toString().trim()));
            sb.append(",\"sr\":").append(ranges(c.docs[i], sk, null));
            sb.append(",\"rr\":").append(ranges(c.docs[j], x.refKind, null)).append('}');
        }
        sb.append("]}");
        send(ex, 200, "application/json; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> {
            try { send(ex, 200, "text/html; charset=utf-8", Files.readAllBytes(Paths.get("web", "index.html"))); }
            catch (Exception e) { send(ex, 500, "text/plain", ("Cannot find web/index.html. Run the command from inside the plagiarism-checker folder. " + e).getBytes()); }
        });
        server.createContext("/api/check", ex -> {
            try { check(ex); } catch (Exception e) { send(ex, 500, "application/json", ("{\"error\":" + json(String.valueOf(e)) + "}").getBytes()); }
        });
        server.createContext("/api/class", ex -> {
            try { classCheck(ex); } catch (Exception e) { send(ex, 500, "application/json", ("{\"error\":" + json(String.valueOf(e)) + "}").getBytes()); }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("Plagiarism Checker is running.  Open  http://localhost:" + port + "  in your browser.");
    }
}
