/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the termsof the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also
http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.util.*;
import java.util.concurrent.*;

/**
 * Worker that queues and processes KIF file checks sequentially,
 * returning results via Future to avoid concurrency issues in KifFileChecker.
 */
public class KifCheckWorker {

    private static final BlockingQueue<Job> queue = new LinkedBlockingQueue<>();

    static {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    Job job = queue.take();
                    try {
                        // Updated: check() now returns List<ErrRec>
                        List<KifFileChecker.ErrRec> result = job.checker.check(job.contents);
                        job.future.complete(result);
                    } catch (Exception e) {
                        job.future.completeExceptionally(e);
                    }
                } catch (InterruptedException ignored) {
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Submit a KIF buffer to be checked asynchronously.
     *
     * @param contents KIF contents as string
     * @return Future that will complete with a list of ErrRec objects
     */
    public static Future<List<KifFileChecker.ErrRec>> submit(String contents) {
        Job job = new Job(contents, new KifFileChecker());
        queue.add(job);
        return job.future;
    }

    private static class Job {
        final String contents;
        final KifFileChecker checker;
        final CompletableFuture<List<KifFileChecker.ErrRec>> future = new CompletableFuture<>();

        Job(String contents, KifFileChecker checker) {
            this.contents = contents;
            this.checker = checker;
        }
    }
}