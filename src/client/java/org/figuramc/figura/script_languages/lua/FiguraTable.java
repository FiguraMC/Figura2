package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaFunction;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.*;

/**
 * Adds some functional operations to the table library for fast iteration on lists.
 */
@SuppressWarnings("DuplicatedCode") // Duplication can help with speed.
public class FiguraTable {

    public static void init(LuaState state) throws LuaError {
        // Get "table" lib
        LuaTable table = state.globals().rawget("table").checkTable(state);

        // Create a new list table, where each list element from the original table
        // was passed through the function.
        table.rawset("map", LibFunction.createS((s, di, args) -> {
            // Check args
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            // Create a new table to put the results into
            LuaTable newTable = new LuaTable(list.length(), 0, s.allocationTracker);
            // Iterate and fill the table
            int i = 1;
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaValue finalValue = value;
                LuaValue result = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue)).first();
                newTable.rawset(i, result);
            }
            return newTable;
        }));
        // map(), but passing the index as the second arg to the function.
        table.rawset("mapI", LibFunction.createS((s, di, args) -> {
            // Check args
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            // Create a new table to put the results into
            LuaTable newTable = new LuaTable(list.length(), 0, s.allocationTracker);
            // Iterate and fill the table
            int i = 1;
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaInteger li = LuaInteger.valueOf(i);
                final LuaValue finalValue = value;
                LuaValue result = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue, li)).first();
                newTable.rawset(i, result);
            }
            return newTable;
        }));
        // map(), but modifies the table in place instead of creating a new one.
        table.rawset("mapMut", LibFunction.createS((s, di, args) -> {
            // Check args
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            // Iterate and fill the table
            int i = 1;
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaValue finalValue = value;
                LuaValue result = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue)).first();
                list.rawset(i, result);
            }
            return list;
        }));
        // mapI(), but modifies the table in place instead of creating a new one.
        table.rawset("mapIMut", LibFunction.createS((s, di, args) -> {
            // Check args
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            // Iterate and fill the table
            int i = 1;
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                LuaInteger li = LuaInteger.valueOf(i);
                final LuaValue finalValue = value;
                LuaValue result = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue, li)).first();
                list.rawset(i, result);
            }
            return list;
        }));

        // Create a new list table, where only the list elements from the original
        // table that pass the predicate are kept.
        table.rawset("filter", LibFunction.createS((s, di, args) -> {
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            LuaTable newTable = new LuaTable(s.allocationTracker);
            int i = 1; // Index in list
            int j = 1; // Index in newTable
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaValue finalValue = value;
                boolean keep = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue)).first().toBoolean();
                if (keep) newTable.rawset(j++, value);
            }
            return newTable;
        }));
        // filter(), but the index is passed to the function as well
        table.rawset("filterI", LibFunction.createS((s, di, args) -> {
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            LuaTable newTable = new LuaTable(s.allocationTracker);
            int i = 1; // Index in list
            int j = 1; // Index in newTable
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaInteger li = LuaInteger.valueOf(i);
                final LuaValue finalValue = value;
                boolean keep = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue, li)).first().toBoolean();
                if (keep) newTable.rawset(j++, value);
            }
            return newTable;
        }));
        // filter(), but modifies the table in place instead of creating a new one.
        table.rawset("filterMut", LibFunction.createS((s, di, args) -> {
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            int i = 1; // Index in list
            int j = 1; // Index in output (same list, it's mut)
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaValue finalValue = value;
                boolean keep = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue)).first().toBoolean();
                if (keep) {
                    // If i == j, the value is already in the right place.
                    if (i != j) list.rawset(j, value);
                    j++;
                }
            }
            return list;
        }));
        // filterI(), but modifies the table in place instead of creating a new one.
        table.rawset("filterIMut", LibFunction.createS((s, di, args) -> {
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            int i = 1; // Index in list
            int j = 1; // Index in output (same list, it's mut)
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaInteger li = LuaInteger.valueOf(i);
                final LuaValue finalValue = value;
                boolean keep = SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue, li)).first().toBoolean();
                if (keep) {
                    // If i == j, the value is already in the right place.
                    if (i != j) list.rawset(j, value);
                    j++;
                }
            }
            return list;
        }));

        // Execute the function once on each value in the list, not doing anything with the result.
        table.rawset("forEach", LibFunction.createS((s, di, args) -> {
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            int i = 1;
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaValue finalValue = value;
                SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue));
            }
            return NONE;
        }));
        // forEach(), but passing the index as the second arg.
        table.rawset("forEachI", LibFunction.createS((s, di, args) -> {
            LuaTable list = args.arg(1).checkTable(s);
            LuaFunction func = args.arg(2).checkFunction(s);
            int i = 1;
            LuaValue value = list.rawget(i);
            for (; !value.isNil(); value = list.rawget(++i)) {
                final LuaInteger li = LuaInteger.valueOf(i);
                final LuaValue finalValue = value;
                SuspendedAction.run(di, () -> Dispatch.call(s, func, finalValue, li));
            }
            return NONE;
        }));

        // Run a fast numeric for loop with the given function.
        // forLoop(start, end, step, func)
        table.rawset("forLoop", LibFunction.createS((s, di, args) -> {
            double min = args.arg(1).checkDouble(s);
            double max = args.arg(2).checkDouble(s);
            double step = args.arg(3).checkDouble(s);
            LuaFunction func = args.arg(4).checkFunction(s);
            for (double v = min; v <= max; v += step) {
                final double fv = v;
                SuspendedAction.run(di, () -> Dispatch.call(s, func, ValueFactory.valueOf(fv)));
            }
            return NONE;
        }));

        // Create a table containing values from start to end, inclusive, by step.
        // range(start, end, step)
        // range(x) -> range(1, x, 1)
        // range(x, y) -> range(x, y, 1)
        table.rawset("range", LibFunction.createV((s, args) -> switch (args.count()) {
            case 1 -> range(s, 1, args.arg(1).checkDouble(s), 1);
            case 2 -> range(s, args.arg(1).checkDouble(s), args.arg(2).checkDouble(s), 1);
            case 3 -> range(s, args.arg(1).checkDouble(s), args.arg(2).checkDouble(s), args.arg(3).checkDouble(s));
            default -> throw new LuaError("Invalid number of args to table.range(): expected 1, 2, or 3, got " + args.count(), s.allocationTracker);
        }));

    }

    private static LuaTable range(LuaState state, double start, double end, double step) throws LuaError {
        // Validate args
        if (step == 0) throw new LuaError("Invalid step argument to table.range(): 0", state.allocationTracker);
        if ((start != end) && ((start > end) != (step < 0))) throw new LuaError("table.range() will never terminate with args " + start + ", " + end + ", " + step, state.allocationTracker);
        double numElems = ((end - start) / step) + 1;
        int count = (int) Math.ceil(numElems);
        if (count != numElems) throw new LuaError("table.range() too large - will have over 2 billion elements", state.allocationTracker);
        // Create and populate table
        LuaTable t = new LuaTable(count, 0, state.allocationTracker);
        int i = 1;
        for (double v = start; (step < 0) ? v >= end : v <= end; v += step)
            t.rawset(i++, valueOf(v));
        return t;
    }

}
