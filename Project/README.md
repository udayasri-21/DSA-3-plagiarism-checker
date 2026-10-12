# Word-Level Plagiarism Checker (DSA-3 Project)

Upload ONE document. It is checked against ALL the files in the `reference` folder
and shows: words matched, similarity with every file, and the final plagiarism result.

## Folder structure (one Java file for every algorithm)
```
plagiarism-checker/
  src/
    RabinKarp.java          Module 2  Rabin-Karp rolling hash
    PhraseTable.java        Module 2  hash table with chaining
    WordDictionary.java     Module 1/2 own hash table: word -> number
    KMP.java                Module 2  Knuth-Morris-Pratt (whole sentences)
    SuffixArray.java        Module 2  suffix array + LCP array (longest common passage)
    EditDistance.java       Module 3  edit distance (dynamic programming)
    MaxFlow.java            Module 4  Dinic max-flow (sentence matching)
    MillerRabin.java        Module 5/6 randomised primality test (random prime modulus)
    RandomUtil.java         Module 6  random numbers, reservoir sampling
    RandomizedQuickSort.java Module 6 randomised quicksort (ranking)
    ParallelRunner.java     Module 6  parallel worker threads
    TextProcessor.java      Module 1  words and sentences
    Doc.java                a processed document
    Engine.java             controller: calls all the above
    Server.java             backend (web server + API)
  web/index.html            the web page (frontend)
  sample-reference-folder/  sample folder to test with (you can select ANY folder from the web page)
  sample-file-to-check/     sample file to submit
  sample-class-answers/     example: answers of Student 1 to Student 10 (some copied, some original)
  .vscode/                  VS Code run settings
  run.bat / run.sh          one-click run without VS Code
```

## How to run in VS Code (step by step)
1. Install the **JDK 17 or newer** (https://adoptium.net). Check: `java -version` and `javac -version`.
2. Install VS Code and the extension **"Extension Pack for Java"**.
3. Unzip, then **File > Open Folder** and open the `plagiarism-checker` folder itself.
4. Open the terminal in VS Code (Ctrl + `) and type:

Windows:
```
mkdir out
javac -d out src\*.java
java -cp out Server 8080
```
Mac / Linux:
```
mkdir -p out
javac -d out src/*.java
java -cp out Server 8080
```
5. When you see `Plagiarism Checker is running`, open **http://localhost:8080** in Chrome or Edge.
6. Stop with **Ctrl + C**.

(The server also shows the web page, so this one command starts the backend and the frontend.)
Shortcut: Windows `run.bat`, Mac/Linux `./run.sh`, or open `src/Server.java` and click **Run**.

## How to use the web page
1. Click **Select folder** and choose the folder you want to compare with (try `sample-reference-folder`).
   Every .txt and .docx file inside it is used. Sub-folders are included.
2. Click **Submit file** (or drag a file into the box) and choose the file to check (try `sample-file-to-check/test_submission.txt`).
3. Click **Check plagiarism**.
4. You get the final result, then a table showing the similarity with every document.
5. Click any document in the table: your file (left) and that document (right) are shown side by side with the
   matching words highlighted in BOTH. Pink = exact copy, yellow = re-worded copy.
6. "Show all sources on my file" highlights every copied part of your file; hover to see which document it matches.
7. "Pick a random file from the folder" checks one random file of the folder against all the others.

## Whole-class mode: Student 1, 2, 3 ... 10 (how many copied and how much)
1. On the web page click the tab **"Check the whole class"**.
2. Click **Select folder** and choose the folder that has one answer file per student (try `sample-class-answers`).
3. Click **Check the whole class**. Every answer is compared with every other answer.
4. You see: how many students copied, a table with the copied % of each student and WHOM they match,
   a matrix (who matches whom), and the two answers side by side with the matching words highlighted.

Result for the example folder `sample-class-answers` (10 students):
- Students 6, 8 and 10 wrote original answers (0% copied).
- Student 1 and Student 3 match (Student 3 copied most of Student 1's answer and added one new sentence).
- Student 2 and Student 5 match (Student 5 re-worded Student 2's answer).
- Student 4 and Student 9 match, and Student 7 mixes sentences from Student 4 and Student 1.
- 7 of 10 students have copied content. The checker shows who MATCHES whom; when one student copies another,
  both show a high match, so the teacher decides who copied.

### Common problems
- "Cannot find web/index.html": the terminal is not inside the `plagiarism-checker` folder (type `dir` or `ls`: you should see src, web).
- Port already in use: `java -cp out Server 9000` and open http://localhost:9000.
- After changing a .java file: stop the server (Ctrl + C) and run the javac and java lines again. After changing web/index.html just refresh the browser.

## How it works (DSA-3 syllabus concepts and where they are used)
| Module | Concept | File | Used for |
|---|---|---|---|
| 2 String Algorithms | Rabin-Karp, polynomial rolling hash, modular arithmetic, collision check | RabinKarp.java | Finds identical 5-word phrases in O(length) time |
| 2 | Hash table with chaining | PhraseTable.java, WordDictionary.java | O(1) lookup of phrases and words |
| 2 | KMP failure function | KMP.java | Finds whole copied sentences inside a reference file |
| 2 | Suffix array and LCP array | SuffixArray.java | Longest passage that two files share |
| 3 Advanced DP | Edit distance (Levenshtein) | EditDistance.java | Detects re-worded sentences (70% or more similar) |
| 4 Network Flow | Max-flow, Dinic, bipartite matching | MaxFlow.java | One-to-one pairing of uploaded sentences with reference sentences |
| 5 and 6 | Miller-Rabin primality test | MillerRabin.java | Picks a random prime modulus for the hash every run |
| 6 Randomised | Universal-hashing idea (random base), randomised quicksort, reservoir sampling | RandomUtil.java, RandomizedQuickSort.java | Random hash, ranking files, random file button |
| 6 Parallel | Work and span, data-parallel jobs | ParallelRunner.java | Compares with up to 8 files at the same time |

Not used: Module 5 NP-completeness proofs (theory only, no program needed) and Module 3 sequence alignment (Smith-Waterman).

Similarity % = (words of your file that match a reference file) / (total words of your file) x 100
Final plagiarism % = words that match ANY reference file / total words (a word is counted once).
Verdict: below 20% LOW, 20 to 49% MEDIUM, 50% and above HIGH. The teacher takes the final decision.
