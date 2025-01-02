package org.figuramc.figura.script_hooks.mem_count;

/**
 * A base class for convenience that handles marking for you.
 *
 * Note that marking is currently the only way for an object to only
 * be counted once, if referenced multiple times.
 */
public abstract class MarkedObjectBase implements MemoryCountable {

    // The memory tracing mark. Start at -1.
    private int mark = -1;

    @Override
    public final long count(MemoryCounter counter, int depth) {
        // If mark is equal already, quit out right away.
        if (mark == counter.mask) return 0;
        // Set mark to equal, so next time around we know it's already counted.
        mark = counter.mask;
        // Class-specific behavior.
        return traceNoMark(counter, depth);
    }

    // Class-specific behavior, conveniently doesn't need to worry about marking logic.
    protected abstract long traceNoMark(MemoryCounter counter, int depth);
}
