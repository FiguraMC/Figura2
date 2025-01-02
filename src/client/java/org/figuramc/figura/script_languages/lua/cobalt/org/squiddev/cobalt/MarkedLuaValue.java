package org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt;

import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;

/**
 * Intermediary class for LuaValues that use memory marking.
 */
public abstract class MarkedLuaValue extends LuaValue {

    // The memory tracing mark. Start at -1.
    private int mark = -1;

    protected MarkedLuaValue(int type) {
        super(type);
    }


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
