/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt;

import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;

public class LuaUserdata extends MarkedLuaValue {

	public final Object instance;
	public LuaTable metatable;

	public LuaUserdata(Object obj) {
		super(Constants.TUSERDATA);
		instance = obj;
	}

	public LuaUserdata(Object obj, LuaTable metatable) {
		super(Constants.TUSERDATA);
		instance = obj;
		this.metatable = metatable;
	}

	@Override
	public int hashCode() {
		return instance.hashCode();
	}

	public Object userdata() {
		return instance;
	}

	public Object toUserdata() {
		return instance;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T checkUserdata(LuaState state, Class<T> targetClass) throws LuaError {
		if (targetClass.isInstance(instance))
			return (T) instance;
		return super.checkUserdata(state, targetClass);
	}

	@Override
	public final LuaTable getMetatable(LuaState state) {
		return metatable;
	}

	@Override
	public final void setMetatable(LuaState state, LuaTable metatable) {
		this.metatable = metatable;
	}

	@Override
	public boolean equals(Object val) {
		return this == val || (val instanceof LuaUserdata other && metatable == other.metatable && instance.equals(other.instance));
	}

	@Override
	protected long traceNoMark(MemoryCounter counter, int depth) {
		// Trace instance if possible
		if (instance instanceof MemoryCountable countable) counter.trace(countable, depth);
		// Always trace metatable
		counter.trace(metatable, depth);
		return OBJECT_SIZE + POINTER_SIZE * 2;
	}
}
