package com.lowdragmc.lowdraglib2.uitest;

/**
 * One unit of work in a scenario. The runner executes at most one step body per rendered frame, so
 * every step observes a UI that has been drawn at least once since the previous step — which is what
 * makes hover, layout and the pose caches valid when the step reads them.
 *
 * <p>A step body may call {@link TestContext#repeat(String)} to ask to run again next frame. That is
 * the only waiting primitive in the framework: {@code waitUntil}, {@code awaitScreen}, server-thread
 * completion and pending captures are all built on it, so there is exactly one timeout mechanism and
 * one shape of timeout message.
 */
public final class Step {

    @FunctionalInterface
    public interface Body {
        void run(TestContext ctx) throws Exception;
    }

    public final String name;
    public final StepKind kind;
    public final Body body;
    /** Milliseconds to idle after this step. {@code -1} means "use the scenario default". */
    public long settleMs = -1;
    /** How long {@link TestContext#repeat(String)} may keep asking. {@code -1} means scenario default. */
    public long timeoutMs = -1;
    /** Label of the enclosing {@code group(..)} block, or {@code null}. */
    public String group;

    public Step(String name, StepKind kind, Body body) {
        this.name = name;
        this.kind = kind;
        this.body = body;
    }

    @Override
    public String toString() {
        return "[" + kind + "] " + name;
    }
}
