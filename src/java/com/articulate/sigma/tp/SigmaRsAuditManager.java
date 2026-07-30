package com.articulate.sigma.tp;

import com.articulate.sigma.KB;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Manages asynchronous sigma-rs consistency audits. */
public final class SigmaRsAuditManager implements AutoCloseable {

    public enum State {
        IDLE, RUNNING, COMPLETED, FAILED
    }

    public record Status(
        State state,
        Instant startedAt,
        Instant completedAt,
        SigmaRsAuditRunner.Result result,
        String error
    ) {
        /***************************************************************
         * Returns an idle audit status.
         * @return idle audit status
         */
        public static Status idle() {
            return new Status(State.IDLE, null, null, null, null);
        }
    }

    private final ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, Status> statuses =
        new ConcurrentHashMap<>();

    private final SigmaRsAuditRunner runner;

    /***************************************************************
     * Creates an audit manager for a sigma-rs executable.
     * @param executable sigma-rs executable path
     */
    public SigmaRsAuditManager(Path executable) {
        runner = new SigmaRsAuditRunner(executable);
    }

    /***************************************************************
     * Returns the current status for a knowledge base.
     * @param kbName knowledge-base name
     * @return current audit status
     */
    public Status status(String kbName) {
        return statuses.getOrDefault(kbName, Status.idle());
    }

    /***************************************************************
     * Starts an audit unless one is already running.
     * @param kb knowledge base to audit
     * @param options audit execution options
     * @return whether a new audit was started
     */
    public synchronized boolean start(
        KB kb,
        SigmaRsAuditRunner.Options options
    ) {
        Status current = status(kb.name);

        if (current.state() == State.RUNNING) {
            return false;
        }

        List<Path> constituents = kb.constituents.stream()
            .map(Path::of)
            .filter(path -> path.toString().endsWith(".kif"))
            .toList();

        Instant started = Instant.now();

        statuses.put(
            kb.name,
            new Status(State.RUNNING, started, null, null, null)
        );

        executor.submit(() -> {
            try {
                SigmaRsAuditRunner.Result result =
                    runner.audit(constituents, null, options);

                statuses.put(
                    kb.name,
                    new Status(
                        State.COMPLETED,
                        started,
                        Instant.now(),
                        result,
                        null
                    )
                );
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                statuses.put(
                    kb.name,
                    new Status(
                        State.FAILED,
                        started,
                        Instant.now(),
                        null,
                        "Audit was interrupted"
                    )
                );
            }
            catch (Exception exception) {
                statuses.put(
                    kb.name,
                    new Status(
                        State.FAILED,
                        started,
                        Instant.now(),
                        null,
                        exception.getMessage()
                    )
                );
            }
        });

        return true;
    }

    /***************************************************************
     * Closes the audit executor.
     */
    @Override
    public void close() {
        executor.close();
    }
}
