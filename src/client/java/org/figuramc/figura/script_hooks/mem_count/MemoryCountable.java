package org.figuramc.figura.script_hooks.mem_count;

// An interface implemented by things that are traceable in memory.
public interface MemoryCountable {

    // Helpful constants
    long OBJECT_SIZE = 16;
    long POINTER_SIZE = 8;

    // Returns the (non-recursive) amount of memory used by this object.
    // For inner objects, use counter.trace() on each of them.

    // The "mask" field on MemoryCounter is to improve efficiency. It begins at 0
    // and remains the same during a single counting process.
    // (of which only one can happen at a time, so no multithreading concerns).
    // The mask is incremented on each trace, meaning we should get a nice ~4 billion traces
    // before it breaks, which is probably fine.
    // For objects that can use marking, this should immediately return 0 if the mark
    // is already equal to the mask (meaning it was already traced), and the mark
    // should be set to the mask at the beginning of the function
    // (to avoid self-reference infinite loops).
    // The mark should start as -1, since this is guaranteed to not be reached by incrementing.

    // Note: marking is currently the only way for an object to avoid being counted multiple times
    // if referenced multiple times!
    long count(MemoryCounter counter, int depth);

}
