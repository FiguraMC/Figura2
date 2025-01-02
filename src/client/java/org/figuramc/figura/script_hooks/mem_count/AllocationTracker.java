package org.figuramc.figura.script_hooks.mem_count;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A class which tracks allocations made by an Avatar, and keeps an eye on memory usage.
 * An instance of this will be known by a lot of locations, and nullable.
 * If the AllocationTracker is null, that indicates the allocation should not be tracked.
 */
public class AllocationTracker {

    // The current amount of memory thought to be allocated
    private long totalAllocated;
    // The maximum allocation before an error (or trace) occurs
    private long maxAllocation;

    // The roots which will be traced when memory fills up.
    private final Set<MemoryCountable> roots = Collections.newSetFromMap(new IdentityHashMap<>());

    // Create a new tracker with the given maximum bytes.
    public AllocationTracker(long maxAllocation) {
        this.maxAllocation = maxAllocation;
    }

    // Set the maximum allocation, and re-trace if necessary.
    public void setMaxAllocation(long maxAllocation) {
        this.maxAllocation = maxAllocation;
        if (maxAllocation < totalAllocated) trace(0);
    }

    // Add a new object as a root.
    // TODO, maybe trace object and verify below max?
    public void addRoot(MemoryCountable countable) {
        this.roots.add(countable);
    }

    // Allocates the given amount of memory, incrementing totalAllocated.
    // If totalAllocated goes beyond maxAllocated, trace roots.
    public void allocate(long amount) {
        totalAllocated += amount;
        if (totalAllocated > maxAllocation) trace(amount);
    }

    // Trace roots to find the actual amount of allocation.
    // If the result of tracing roots is beyond maxAllocated, error.
    // Otherwise, reset totalAllocated down to the actual allocated amount.
    // TODO... maybe handle overflows? Overflows should never happen in this
    //         case with a long, though, and we need this to be FAST.
    private void trace(long newAllocation) {
        long actualAllocated = MemoryCounter.countBytes(this.roots) + newAllocation;
        if (actualAllocated > maxAllocation)
            throw new RuntimeException("Allocation limit of " + maxAllocation + " bytes exceeded"); // TODO better error
        totalAllocated = actualAllocated;
    }

}
