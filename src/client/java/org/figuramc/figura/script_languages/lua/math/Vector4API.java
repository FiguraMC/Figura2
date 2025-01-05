package org.figuramc.figura.script_languages.lua.math;

import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.*;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

public class Vector4API {

    // Return a LuaTable which acts as the metatable for this type
    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        AllocationTracker t = state.allocationTracker;
        LuaTable methods = tableOf(t);
        LuaTable metatable = tableOf(t);

        // Methods

        // In-place mutation
        /* add */ {
            methods.rawset(valueOf("add", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).add(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            methods.rawset(valueOf("addX", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x += v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("addY", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y += v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("addZ", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z += v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("addW", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w += v2.checkDouble(s);
                return v1;
            }));
        }
        /* sub */ {
            methods.rawset(valueOf("sub", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).sub(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            methods.rawset(valueOf("subX", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x -= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("subY", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y -= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("subZ", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z -= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("subW", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w -= v2.checkDouble(s);
                return v1;
            }));
        }
        /* mul (and scale) */ {
            methods.rawset(valueOf("mul", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).mul(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            methods.rawset(valueOf("scale", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).mul(v2.checkDouble(s));
                return v1;
            }));
            methods.rawset(valueOf("mulX", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x *= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("mulY", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y *= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("mulZ", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z *= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("mulW", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w *= v2.checkDouble(s);
                return v1;
            }));
        }
        /* div */ {
            methods.rawset(valueOf("div", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).div(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            methods.rawset(valueOf("divX", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x /= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("divY", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y /= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("divZ", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z /= v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("divW", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w /= v2.checkDouble(s);
                return v1;
            }));
        }
        /* mod */ {
            methods.rawset(valueOf("mod", t), LibFunction.create((s, v1, v2) -> {
                Vector4d a = v1.checkUserdata(s, Vector4d.class);
                Vector4d b = v2.checkUserdata(s, Vector4d.class);
                a.set(
                        OperationHelper.mod(a.x, b.x),
                        OperationHelper.mod(a.y, b.y),
                        OperationHelper.mod(a.z, b.z),
                        OperationHelper.mod(a.w, b.w)
                );
                return v1;
            }));
            methods.rawset(valueOf("modX", t), LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.x = OperationHelper.mod(vec.x, v2.checkDouble(s));
                return v1;
            }));
            methods.rawset(valueOf("modY", t), LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.y = OperationHelper.mod(vec.y, v2.checkDouble(s));
                return v1;
            }));
            methods.rawset(valueOf("modZ", t), LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.z = OperationHelper.mod(vec.z, v2.checkDouble(s));
                return v1;
            }));
            methods.rawset(valueOf("modW", t), LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.w = OperationHelper.mod(vec.w, v2.checkDouble(s));
                return v1;
            }));
        }
        /* set */ {
            methods.rawset(valueOf("set", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).set(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            methods.rawset(valueOf("setX", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x = v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("setY", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y = v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("setZ", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z = v2.checkDouble(s);
                return v1;
            }));
            methods.rawset(valueOf("setW", t), LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w = v2.checkDouble(s);
                return v1;
            }));
        }
        // Normalize this vector.
        methods.rawset(valueOf("normalize", t), LibFunction.create((s, v) -> {
            v.checkUserdata(s, Vector4d.class).normalize();
            return v;
        }));
        // Clamp this vector's length between min and max. Both args optional.
        methods.rawset(valueOf("clamp", t), LibFunction.create((s, v, optmin, optmax) -> {
            Vector4d vec = v.checkUserdata(s, Vector4d.class);
            double min = optmin.optDouble(s, Double.NaN);
            double len2 = vec.lengthSquared();
            if (len2 < min*min) vec.normalize(min);
            else {
                double max = optmax.optDouble(s, Double.NaN);
                if (len2 > max*max) vec.normalize(max);
            }
            return v;
        }));

        // Other special operations that don't mutate

        // Dot product of this vector with another
        methods.rawset(valueOf("dot", t), LibFunction.create((s, v1, v2) ->
                valueOf(v1.checkUserdata(s, Vector4d.class).dot(v2.checkUserdata(s, Vector4d.class)))
        ));
        // Copy this vector
        methods.rawset(valueOf("copy", t), LibFunction.create((s, v) ->
                userdataOf(new Vector4d(v.checkUserdata(s, Vector4d.class)), metatable)
        ));
        // Get a normalized copy of this vector
        methods.rawset(valueOf("unit", t), LibFunction.create((s, v) ->
                userdataOf(new Vector4d(v.checkUserdata(s, Vector4d.class)).normalize(), metatable)
        ));
        // Length
        methods.rawset(valueOf("len", t), LibFunction.create((s, v) ->
                valueOf(v.checkUserdata(s, Vector4d.class).length())
        ));
        // Length squared
        methods.rawset(valueOf("len2", t), LibFunction.create((s, v) ->
                valueOf(v.checkUserdata(s, Vector4d.class).lengthSquared())
        ));
        // Get a copy with length clamped between min and max. Both args are optional.
        methods.rawset(valueOf("clamped", t), LibFunction.create((s, v, optmin, optmax) -> {
            Vector4d vec = v.checkUserdata(s, Vector4d.class);
            double min = optmin.optDouble(s, Double.NaN);
            double len2 = vec.lengthSquared();
            if (len2 < min*min) return userdataOf(new Vector4d(vec).normalize(min), metatable);
            else {
                double max = optmax.optDouble(s, Double.NaN);
                if (len2 > max*max) return userdataOf(new Vector4d(vec).normalize(max), metatable);
            }
            return v;
        }));
        // Return the number of elements in the vector.
        methods.rawset(valueOf("count", t), LibFunction.create(s -> valueOf(4)));
        // Unpack the elements of the vector into Lua varargs.
        methods.rawset(valueOf("unpack", t), LibFunction.createV((s, args) -> {
            Vector4d vec = args.first().checkUserdata(s, Vector4d.class);
            return varargsOf(valueOf(vec.x), valueOf(vec.y), valueOf(vec.z), valueOf(vec.w));
        }));

        // Operators:

        // Does arithmetic with two vectors, or a vector and a number.
        metatable.rawset(ADD, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return userdataOf(new Vector4d(a).add(b), metatable);
        }));
        metatable.rawset(SUB, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return userdataOf(new Vector4d(a).sub(b), metatable);
        }));
        metatable.rawset(MUL, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return userdataOf(new Vector4d(a).mul(b), metatable);
        }));
        metatable.rawset(DIV, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return userdataOf(new Vector4d(a).div(b), metatable);
        }));
        metatable.rawset(MOD, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return userdataOf(new Vector4d(
                    OperationHelper.mod(a.x, b.x),
                    OperationHelper.mod(a.y, b.y),
                    OperationHelper.mod(a.z, b.z),
                    OperationHelper.mod(a.w, b.w)
            ), metatable);
        }));
        metatable.rawset(UNM, LibFunction.create((s, v) -> {
            Vector4d a = v.checkUserdata(s, Vector4d.class);
            return userdataOf(new Vector4d(a).negate(), metatable);
        }));

        // # operator can also be used for vector length.
        metatable.rawset(LEN, LibFunction.create((s, v) ->
                valueOf(v.checkUserdata(s, Vector4d.class).length())
        ));

        // Returns whether all components are less than or equal to the other. (<= and >= operators)
        metatable.rawset(LE, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.checkUserdata(s, Vector4d.class);
            Vector4d b = v2.checkUserdata(s, Vector4d.class);
            return valueOf(a.x <= b.x && a.y <= b.y && a.z <= b.z && a.w <= b.w);
        }));
        // Returns whether all components are strictly less than the other. (< and > operators)
        metatable.rawset(LT, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.checkUserdata(s, Vector4d.class);
            Vector4d b = v2.checkUserdata(s, Vector4d.class);
            return valueOf(a.x < b.x && a.y < b.y && a.z < b.z && a.w < b.w);
        }));
        // Returns whether the vectors are equal. (== and ~= operators)
        metatable.rawset(EQ, LibFunction.create((s, v1, v2) ->
                valueOf(v1.checkUserdata(s, Vector4d.class).equals(v2.checkUserdata(s, Vector4d.class)))
        ));

        // Indexing. Check methods first, otherwise turn into swizzle.
        metatable.rawset(INDEX, methods);
        methods.setMetatable(state, tableOf(t, INDEX, LibFunction.create((s, v, k) -> {
            Vector4d self = v.checkUserdata(s, Vector4d.class);
            // Numeric key, index it like an array
            if (k.type() == TNUMBER) {
                return valueOf(switch (k.toInteger()) {
                    case 1 -> self.x;
                    case 2 -> self.y;
                    case 3 -> self.z;
                    case 4 -> self.w;
                    default -> throw ErrorFactory.argError(s, k, "integer 1 to 4");
                });
            }
            // Key should be a string, do some swizzling
            LuaString key = k.checkLuaString(s);
            @Nullable AllocationTracker tr = s.allocationTracker;
            return switch (key.length()) {
                case 1 -> valueOf(getSwizzle(self, (char) key.charAt(0), tr));
                case 2 -> userdataOf(new Vector2d(
                        getSwizzle(self, (char) key.charAt(0), tr),
                        getSwizzle(self, (char) key.charAt(1), tr)
                ), metatables.vec2);
                case 3 -> userdataOf(new Vector3d(
                        getSwizzle(self, (char) key.charAt(0), tr),
                        getSwizzle(self, (char) key.charAt(1), tr),
                        getSwizzle(self, (char) key.charAt(2), tr)
                ), metatables.vec3);
                case 4 -> userdataOf(new Vector4d(
                        getSwizzle(self, (char) key.charAt(0), tr),
                        getSwizzle(self, (char) key.charAt(1), tr),
                        getSwizzle(self, (char) key.charAt(2), tr),
                        getSwizzle(self, (char) key.charAt(3), tr)
                ), metatable);
                default -> throw new LuaError("Invalid swizzle - length must be 1 to 4 chars, got '" + k + "'", tr);
            };
        })));

        // Newindex, always swizzle.
        metatable.rawset(NEWINDEX, LibFunction.create((s, vec, key, value) -> {
            Vector4d self = vec.checkUserdata(s, Vector4d.class);
            // Numeric key, index like an array
            if (key.type() == TNUMBER) {
                switch (key.toInteger()) {
                    case 1 -> self.x = value.checkDouble(s);
                    case 2 -> self.y = value.checkDouble(s);
                    case 3 -> self.z = value.checkDouble(s);
                    case 4 -> self.w = value.checkDouble(s);
                    default -> throw ErrorFactory.argError(s, key, "integer 1 to 4");
                }
                return NIL;
            }
            LuaString k = key.checkLuaString(s);
            @Nullable AllocationTracker tr = s.allocationTracker;
            switch (k.length()) {
                case 1 -> setSwizzle(self, (char) k.charAt(0), value.checkDouble(s), tr);
                case 2 -> {
                    Vector2d rhs = value.checkUserdata(s, Vector2d.class);
                    setSwizzle(self, (char) k.charAt(0), rhs.x, tr);
                    setSwizzle(self, (char) k.charAt(1), rhs.y, tr);
                }
                case 3 -> {
                    Vector3d rhs = value.checkUserdata(s, Vector3d.class);
                    setSwizzle(self, (char) k.charAt(0), rhs.x, tr);
                    setSwizzle(self, (char) k.charAt(1), rhs.y, tr);
                    setSwizzle(self, (char) k.charAt(2), rhs.z, tr);
                }
                case 4 -> {
                    Vector4d rhs = value.checkUserdata(s, Vector4d.class);
                    setSwizzle(self, (char) k.charAt(0), rhs.x, tr);
                    setSwizzle(self, (char) k.charAt(1), rhs.y, tr);
                    setSwizzle(self, (char) k.charAt(2), rhs.z, tr);
                    setSwizzle(self, (char) k.charAt(3), rhs.w, tr);
                }
                default -> throw new LuaError("Invalid swizzle - length must be 1 to 4 chars, got '" + k + "'", tr);
            }
            return NIL;
        }));

        // Return the metatable
        return metatable;
    }

    // Swizzle helper functions, get/set.

    // x,y,z,w work as expected for swizzle
    // underscore = 0
    // u = 1
    // capital letters negate the value
    private static double getSwizzle(Vector4d v, char c, @Nullable AllocationTracker tracker) throws LuaError {
        return switch (c) {
            case '_' -> 0;
            case 'u' -> 1;
            case 'x' -> v.x;
            case 'y' -> v.y;
            case 'z' -> v.z;
            case 'w' -> v.w;
            case 'U' -> -1;
            case 'X' -> -v.x;
            case 'Y' -> -v.y;
            case 'Z' -> -v.z;
            case 'W' -> -v.w;
            default -> throw new LuaError("Invalid swizzle character to Vec4: '" + c + "'", tracker);
        };
    }
    private static void setSwizzle(Vector4d v, char c, double num, @Nullable AllocationTracker tracker) throws LuaError {
        switch (c) {
            case '_' -> {}
            case 'x' -> v.x = num;
            case 'y' -> v.y = num;
            case 'z' -> v.z = num;
            case 'w' -> v.w = num;
            case 'X' -> v.x = -num;
            case 'Y' -> v.y = -num;
            case 'Z' -> v.z = -num;
            case 'W' -> v.w = -num;
            default -> throw new LuaError("Invalid swizzle character to Vec4: '" + c + "'", tracker);
        }
    }

}
