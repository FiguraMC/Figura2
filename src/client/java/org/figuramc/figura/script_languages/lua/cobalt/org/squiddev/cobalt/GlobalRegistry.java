package org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt;

import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.jetbrains.annotations.Nullable;

/**
 * The global registry, a store of Lua values
 */
public final class GlobalRegistry {

	private final @Nullable AllocationTracker allocTracker;

	private final LuaTable table;

	GlobalRegistry(@Nullable AllocationTracker allocTracker) throws AllocationTracker.AvatarOOMException {
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
	public LuaTable getSubTable(LuaString name) throws LuaError, AllocationTracker.AvatarOOMException {
		LuaValue value = table.rawget(name);
		if (value instanceof LuaTable table) return table;

		LuaTable newValue = new LuaTable(allocTracker);
		table.rawset(name, newValue);
		return newValue;
	}

}
