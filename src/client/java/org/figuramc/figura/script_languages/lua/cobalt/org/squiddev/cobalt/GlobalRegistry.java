package org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt;

import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.jetbrains.annotations.Nullable;

/**
 * The global registry, a store of Lua values
 */
public final class GlobalRegistry extends MarkedObjectBase {

	private final @Nullable AllocationTracker allocTracker;

	private final LuaTable table;

	GlobalRegistry(@Nullable AllocationTracker allocTracker) {
		this.allocTracker = allocTracker;
		this.table = new LuaTable(allocTracker);
	}

	/**
	 * Get the underlying registry table.
	 *
	 * @return The global debug registry.
	 */
	public LuaTable get() {
		return table;
	}

	/**
	 * Get a subtable in the global {@linkplain #get()} registry table}. If the key exists but is not a table, then
	 * it will be overridden.
	 *
	 * @param name The name of the registry table.
	 * @return The subentry.
	 */
	public LuaTable getSubTable(LuaString name) throws LuaError {
		LuaValue value = table.rawget(name);
		if (value instanceof LuaTable table) return table;

		LuaTable newValue = new LuaTable(allocTracker);
		table.rawset(name, newValue);
		return newValue;
	}

	@Override
	protected long traceNoMark(MemoryCounter counter, int depth) {
		counter.trace(table, depth);
		return 32;
	}
}
