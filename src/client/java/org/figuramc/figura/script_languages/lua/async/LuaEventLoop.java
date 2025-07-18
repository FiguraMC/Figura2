package org.figuramc.figura.script_languages.lua.async;

import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.unwind.SuspendedTask;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;

/**
 * Manages a priority queue for "waiting" events to be resumed at a later time.
 *
 */
// TODO expose event loops as first class items to Lua!
// Note: This doesn't work and fails instrumentation, so it's commented out. Implementing first-class in Lua directly would be easier and less error-prone.
public class LuaEventLoop /*extends MarkedObjectBase*/ {

//    private final PriorityQueue<Entry> queue = new PriorityQueue<>();
//
//    /**
//     * This doesn't matter very much.
//     * Higher values are hypothetically more efficient, but less precise with respect to floating point error.
//     * Default is 100.
//     */
//    private final double threshold;
//    private double accum;
//    private int insertionIndex = Integer.MIN_VALUE; // For disambiguation
//
//    public LuaEventLoop() {
//        this(100);
//    }
//    public LuaEventLoop(double threshold) {
//        this.threshold = threshold;
//    }
//
//    /**
//     * Handle a yield that occurred while running a task in this event loop.
//     */
//    public void handleYield(LuaState state, SuspendedTask<Void> task, UnwindThrowable yieldException) throws LuaError {
//        while (true) {
//            assert yieldException.isYield();
//            // Get the yielded value
//            LuaValue yieldResult = yieldException.getArgs().first();
//            if (yieldResult.isNumber()) {
//                // If they yielded a number, schedule a resume that many ticks from now
//                double ticksToWait = yieldResult.checkNumber(state).toDouble();
//                enqueue(ticksToWait, task);
//                return;
//            } else if (yieldResult instanceof LuaUserdata userdata && userdata.instance instanceof LuaPromise toAwait) {
//                // If they yielded a promise, hook this task onto it
//                try {
//                    toAwait.thenRun(task);
//                    return;
//                } catch (UnwindThrowable yieldedAgain) {
//                    // If it yielded again, repeat the process.
//                    // This is basically recursion, but I used a while loop to hopefully reduce issues with stack overflow.
//                    yieldException = yieldedAgain;
//                }
//            } else {
//                throw new LuaError("Yielding inside async() should pass a number of ticks to wait, or a Promise to await", state.allocationTracker);
//            }
//        }
//    }
//
//    /**
//     * Enqueue a new event, which will run after the given amount of time has been advanced.
//     */
//    public void enqueue(double timeToWait, SuspendedTask<Void> toResume) {
//        queue.add(new Entry(timeToWait, toResume, insertionIndex++));
//    }
//
//    /**
//     * Advance the event loop by the given amount of units.
//     */
//    public void advance(LuaState state, double amount) throws LuaError {
//        // Advance time.
//        accum += amount;
//        // Run the queue.
//        while (true) {
//            // If we're out of things to run, break
//            if (queue.isEmpty()) break;
//            // If it's not time to run the next item yet, break
//            if (queue.peek().timeToWait > accum) break;
//            // Poll a task and resume it
//            SuspendedTask<Void> task = queue.poll().task;
//            try {
//                task.resume(Constants.NONE);
//            } catch (UnwindThrowable unwind) {
//                handleYield(state, task, unwind);
//            }
//        }
//        // If necessary, decrement elements in the queue according to threshold.
//        // Because we decrement the time of ALL events in the queue, this shouldn't
//        // break the Comparable ordering unless a floating point error occurs with
//        // very precise timings, which isn't the end of the world.
//        if (accum >= threshold) {
//            double diff = threshold * Math.floor(accum / threshold);
//            for (Entry e : queue)
//                e.timeToWait -= diff;
//        }
//    }
//
//    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) {
//        throw new UnsupportedOperationException("TODO(?)");
//    }
//
//    /**
//     * One entry in the priority queue. Has an amount of time to wait, as well as a thread to resume once it's completed.
//     */
//    private static class Entry extends MarkedObjectBase implements Comparable<Entry> {
//
//        private double timeToWait;
//        private final SuspendedTask<Void> task; // TODO make tasks traceable... this is going to be extremely painful isn't it... maybe come up with another idea for this?
//        private final int insertionIndex; // Possible issues if over 4 billion things are added to the loop over its lifetime. TODO fix if it's ever a genuine issue
//
//        public Entry(double timeToWait, SuspendedTask<Void> task, int insertionIndex) {
//            this.timeToWait = timeToWait;
//            this.task = task;
//            this.insertionIndex = insertionIndex;
//        }
//
//        @Override
//        public int compareTo(@NotNull Entry o) {
//            // Time to wait is more important, so sooner events are earlier in the queue
//            if (timeToWait != o.timeToWait) return Double.compare(timeToWait, o.timeToWait);
//            // If two events have the same time, use their insertion order
//            return Integer.compare(insertionIndex, o.insertionIndex);
//        }
//
//        @Override
//        protected long traceNoMark(MemoryCounter counter, int depth) {
//            return 48;
//        }
//    }
//
//    @Override
//    protected long traceNoMark(MemoryCounter counter, int depth) {
//        for (Entry e : queue) counter.trace(e, depth);
//        return 64 + queue.size() * POINTER_SIZE;
//    }
}
