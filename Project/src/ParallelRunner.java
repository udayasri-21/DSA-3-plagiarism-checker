// ParallelRunner.java - Module 6: parallel algorithms. The comparisons with different reference files do not
// depend on each other (an embarrassingly-parallel, data-parallel job).
// Work T1 = O(N x L) in total; span T_inf = O(L) for one file; W workers share the N jobs.
public class ParallelRunner {
    public interface Job { void run(int index); }

    public static void forEach(int count, int workers, Job job) throws InterruptedException {
        Thread[] th = new Thread[workers];
        for (int w = 0; w < workers; w++) {
            final int id = w, step = workers;
            th[w] = new Thread(() -> { for (int i = id; i < count; i += step) job.run(i); });
            th[w].start();
        }
        for (int w = 0; w < workers; w++) th[w].join();
    }
}
