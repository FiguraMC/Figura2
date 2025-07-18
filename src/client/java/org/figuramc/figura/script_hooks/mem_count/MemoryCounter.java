package org.figuramc.figura.script_hooks.mem_count;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

// To perform memory counting, create a new MemoryCounter and begin.
public class MemoryCounter {

    // The current count of memory, incremented over time.
    private long count;
    // The mask which this counter will use. Check this for efficient traceables.
    public final int mask;
    // Objects which will need to be deeply traced in a new pass, because we ran out of depth.
    private final ArrayDeque<MemoryCountable> deepTraced = new ArrayDeque<>(128);
    // Set of traced objects which don't use marking.
    private final Set<Object> tracedUnmarked = Collections.newSetFromMap(new IdentityHashMap<>());

    private MemoryCounter(int mask) {
        this.mask = mask;
    }

    // Trace an object.
    public void trace(@Nullable MemoryCountable traceable, int depth) {
        if (traceable == null) return;
        if (depth == 0) deepTraced.addLast(traceable);
        else count += traceable.count(this, depth - 1);
    }

    // Trace string (we can't make it implement MemoryCountable)
    public void traceString(@Nullable String string) {
        if (string == null) return;
        count += string.length();
    }

    // Trace various unmarked objects
    public void traceUnmarked(byte[] arr) {
        if (tracedUnmarked.add(arr))
            count += arr.length;
    }


    // Count the bytes used by countable objects.
    // Uses a lock to ensure that only one operation is in progress at a time.
    // Without this, marked objects referenced by multiple threads could break.
    private static final Object LOCK = new Object();

    // This should get us a good 4 billion unique GC IDs before everything dies,
    // so that should work out in practice.
    private static int NEXT_MASK = 0;
    private static final int MAX_DEPTH = 10;

    // Function that will trace everything and count all reachable bytes from the given objects.
    public static long countBytes(Iterable<MemoryCountable> roots) {
        synchronized (LOCK) {
            // Error out instead of silently breaking, if mask somehow overflowed back to -1
            if (NEXT_MASK == -1)
                throw new IllegalStateException("You somehow triggered 4 billion memory counts! Not sure if I should be impressed or annoyed. Let Figura devs know they have to fix this, I guess.");
            MemoryCounter counter = new MemoryCounter(NEXT_MASK++);
            for (MemoryCountable root : roots)
                counter.trace(root, MAX_DEPTH);
            MemoryCountable traceable;
            while ((traceable = counter.deepTraced.pollLast()) != null)
                counter.trace(traceable, MAX_DEPTH);
            return counter.count;
        }
    }

}
