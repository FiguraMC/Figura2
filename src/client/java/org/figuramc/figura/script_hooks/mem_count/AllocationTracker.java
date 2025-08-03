package org.figuramc.figura.script_hooks.mem_count;

import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatars.AvatarError;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class which tracks allocations made by an Avatar, and keeps an eye on memory usage.
 * An instance of this will be known by a lot of locations, and nullable.
 * If the AllocationTracker is null, that indicates the allocation should not be tracked.
 * <p>
 * Because GC is unpredictable, we can't be exactly sure how much memory is used/reachable
 * at any given time. Instead, we'll use a "grace period" to reduce the likelihood of scripts
 * being punished wrongly.
 * Each AllocationTracker has a maximum allowed amount, but it also has two grace values.
 * Say that we give our tracker 500 MB, but also a grace period of 3 seconds and 100 MB.
 * In this case, when memory goes above 500, we'll run System.gc().
 * This *should* cause references to be cleared/enqueued soon, on most popular JVMs, making them eligible for cleaning.
 * Then, we'll wait 3 seconds, for the phantom references to hopefully be processed in the off-thread.
 * If, after 3 seconds, we're still above 500, we'll error out.
 * To prevent people from overallocating and abusing the grace period, if at any point memory goes above 600, we immediately error.
 * The grace period parameters, particularly the time, may need to be tuned.
 */
public class AllocationTracker {

    // Constants used for size estimation
    public static final int
        OBJECT_SIZE = 8, // Object overhead
        REFERENCE_SIZE = 4, // Assume compacted references
        LONG_SIZE = 8, INT_SIZE = 4, SHORT_SIZE = 2, CHAR_SIZE = 2, BYTE_SIZE = 1, BOOLEAN_SIZE = 1,
        DOUBLE_SIZE = 8, FLOAT_SIZE = 4;


    // The cleaner used for tracking allocation amounts.
    private static final Cleaner ALLOC_CLEANER = Cleaner.create();

    // The current amount of memory thought to be reachable by the tracker.
    // This is atomic, because many threads can mess with it at once, including allocators incrementing it through the tracker,
    // and objects being cleaned clearing it as well.
    // Calling System.gc() *should* update this value and decrease it, if any unreachable objects are GC'ed by that call.
    private final AtomicLong totalAllocated;
    // The maximum allocation before an error (or trace) occurs
    private final long maxAllocation;
    // Grace period time in nanoseconds
    private final long gracePeriodNanos;
    // Max allocation, plus the grace period extra memory
    private final long trueMaxAllocation;
    // Timestamp when the grace period began, or -1 if the grace period is not active.
    private long gracePeriodBegan;

    // Create a new tracker with the given maximum bytes and grace period stats.
    // To have a tracker with no grace, set gracePeriodExtraSpace to 0 (the nanos won't matter in this case)
    public AllocationTracker(int maxAllocation, long gracePeriodNanos, int gracePeriodExtraSpace) {
        this.totalAllocated = new AtomicLong(0);
        this.maxAllocation = maxAllocation;
        this.gracePeriodNanos = gracePeriodNanos;
        this.trueMaxAllocation = (long) maxAllocation + (long) gracePeriodExtraSpace;
        this.gracePeriodBegan = -1;
    }

    public void track(byte[] byteArray) throws AvatarOOMException {
        track(byteArray, OBJECT_SIZE + byteArray.length * BYTE_SIZE);
    }

    public void track(int[] intArray) throws AvatarOOMException {
        track(intArray, OBJECT_SIZE + intArray.length * INT_SIZE);
    }

    public void track(Object[] objectArray) throws AvatarOOMException {
        track(objectArray, OBJECT_SIZE + objectArray.length * REFERENCE_SIZE);
    }

    public void track(String string) throws AvatarOOMException {
        track(string, OBJECT_SIZE + REFERENCE_SIZE + BYTE_SIZE + INT_SIZE + BOOLEAN_SIZE + string.length() * CHAR_SIZE); // Assume 2 bytes per char (non-compressed String)
    }

    // Allocate the given amount for the given Object.
    // Returns a State object, which can optionally be increased/decreased in size.
    public State track(Object obj, int amount) throws AvatarOOMException {
        State state = new State(amount);
        incrementMemory(amount);
        ALLOC_CLEANER.register(obj, state);
        return state;
    }

    // Increment memory by the given amount, and prepare to error if we're beyond our cap.
    private void incrementMemory(int amount) throws AvatarOOMException {
        // Get the new amount allocated:
        long newAmountAllocated = totalAllocated.addAndGet(amount);
        FiguraMod.LOGGER.info("Allocated {} bytes. Current usage: {} bytes", amount, newAmountAllocated);
        // If we're above the regular cap:
        if (newAmountAllocated > maxAllocation) {
            // If we're above the true cap, error right away.
            if (newAmountAllocated > trueMaxAllocation) {
                throw new AvatarOOMException();
            } else if (gracePeriodBegan == -1) {
                // If we're not currently in the grace period, begin the grace period.
                System.gc(); // Trigger a System.gc()
                gracePeriodBegan = System.nanoTime();
            } else if (System.nanoTime() - gracePeriodBegan >= gracePeriodNanos) {
                // If the grace period has ended, but we're still above the max allocation, error out.
                throw new AvatarOOMException();
            }
        } else {
            // If we're not above the cap, reset the grace period.
            gracePeriodBegan = -1;
        }
    }

    // Cleaner state object.
    // When allocating an object with a particular known size, create a State with that size, and register it.
    // When creating the state, we increment the current amount of memory allocated.
    // When the state is run (the object is cleaned), we decrement the amount of memory allocated.
    public class State implements Runnable {

        private int memoryReserved; // The amount of memory reserved by this

        public State(int memoryReserved) {
            this.memoryReserved = memoryReserved;
        }

        // Modify the amount of memory used by this allocation.
        public void changeSize(int change) throws AvatarOOMException {
            if (change > 0) {
                // The allocation got bigger, increment memory
                memoryReserved += change;
                AllocationTracker.this.incrementMemory(change);
            } else if (change < 0) {
                // The allocation got smaller, decrement memory
                memoryReserved += change;
                // Decrementing memory can't cause a problem, so do it directly
                AllocationTracker.this.totalAllocated.getAndAdd(change);
            }
        }

        @Override
        public void run() {
            AllocationTracker.this.totalAllocated.getAndAdd(-memoryReserved);
        }
    }

    public static class AvatarOOMException extends AvatarError {
        public AvatarOOMException() {
            super("figura.error.out_of_memory");
        }
    }


}
