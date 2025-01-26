package org.figuramc.figura.script_languages.lua.math.vector;

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

@SuppressWarnings("DuplicatedCode")
public class Vector4API {

    // Wrap a Vector4d into a userdata given the metatables
    public static LuaUserdata wrap(Vector4d vec, FiguraMetatables metatables) {
        return userdataOf(vec, metatables.vec4);
    }

    // Return a LuaTable which acts as the metatable for this type
    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

        // Methods

        // In-place mutation

        /* add */ {
            metatable.rawset("add", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).add(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            metatable.rawset("addX", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x += v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("addY", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y += v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("addZ", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z += v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("addW", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w += v2.checkDouble(s);
                return v1;
            }));
        }
        /* sub */ {
            metatable.rawset("sub", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).sub(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            metatable.rawset("subX", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x -= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("subY", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y -= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("subZ", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z -= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("subW", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w -= v2.checkDouble(s);
                return v1;
            }));
        }
        /* mul (and scale) */ {
            metatable.rawset("mul", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).mul(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            metatable.rawset("scale", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).mul(v2.checkDouble(s));
                return v1;
            }));
            metatable.rawset("mulX", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x *= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("mulY", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y *= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("mulZ", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z *= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("mulW", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w *= v2.checkDouble(s);
                return v1;
            }));
        }
        /* div */ {
            metatable.rawset("div", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).div(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            metatable.rawset("divX", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x /= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("divY", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y /= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("divZ", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z /= v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("divW", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w /= v2.checkDouble(s);
                return v1;
            }));
        }
        /* mod */ {
            metatable.rawset("mod", LibFunction.create((s, v1, v2) -> {
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
            metatable.rawset("modX", LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.x = OperationHelper.mod(vec.x, v2.checkDouble(s));
                return v1;
            }));
            metatable.rawset("modY", LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.y = OperationHelper.mod(vec.y, v2.checkDouble(s));
                return v1;
            }));
            metatable.rawset("modZ", LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.z = OperationHelper.mod(vec.z, v2.checkDouble(s));
                return v1;
            }));
            metatable.rawset("modW", LibFunction.create((s, v1, v2) -> {
                Vector4d vec = v1.checkUserdata(s, Vector4d.class);
                vec.w = OperationHelper.mod(vec.w, v2.checkDouble(s));
                return v1;
            }));
        }
        /* set */ {
            metatable.rawset("set", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).set(v2.checkUserdata(s, Vector4d.class));
                return v1;
            }));
            metatable.rawset("setX", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).x = v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("setY", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).y = v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("setZ", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).z = v2.checkDouble(s);
                return v1;
            }));
            metatable.rawset("setW", LibFunction.create((s, v1, v2) -> {
                v1.checkUserdata(s, Vector4d.class).w = v2.checkDouble(s);
                return v1;
            }));
        }
        // Normalize this vector.
        metatable.rawset("normalize", LibFunction.create((s, v) -> {
            v.checkUserdata(s, Vector4d.class).normalize();
            return v;
        }));
        // Clamp this vector's length between min and max. Both args are optional.
        metatable.rawset("clamp", LibFunction.create((s, v, optmin, optmax) -> {
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
        metatable.rawset("dot", LibFunction.create((s, v1, v2) ->
                valueOf(v1.checkUserdata(s, Vector4d.class).dot(v2.checkUserdata(s, Vector4d.class)))
        ));
        // Copy this vector
        metatable.rawset("copy", LibFunction.create((s, v) ->
                wrap(new Vector4d(v.checkUserdata(s, Vector4d.class)), metatables)
        ));
        // Get a normalized copy of this vector
        metatable.rawset("unit", LibFunction.create((s, v) ->
                wrap(new Vector4d(v.checkUserdata(s, Vector4d.class)).normalize(), metatables)
        ));
        // Length (or use the # operator!)
        metatable.rawset("len", LibFunction.create((s, v) ->
                valueOf(v.checkUserdata(s, Vector4d.class).length())
        ));
        // Length squared
        metatable.rawset("len2", LibFunction.create((s, v) ->
                valueOf(v.checkUserdata(s, Vector4d.class).lengthSquared())
        ));
        // Get a copy with length clamped between min and max. Both args are optional.
        metatable.rawset("clamped", LibFunction.create((s, v, optmin, optmax) -> {
            Vector4d vec = v.checkUserdata(s, Vector4d.class);
            double min = optmin.optDouble(s, Double.NaN);
            double len2 = vec.lengthSquared();
            if (len2 < min*min) return wrap(new Vector4d(vec).normalize(min), metatables);
            else {
                double max = optmax.optDouble(s, Double.NaN);
                if (len2 > max*max) return wrap(new Vector4d(vec).normalize(max), metatables);
            }
            return v;
        }));
        // Return the number of elements in the vector.
        metatable.rawset("count", LibFunction.create(s -> valueOf(4)));
        // Unpack the elements of the vector into Lua varargs.
        metatable.rawset("unpack", LibFunction.createV((s, args) -> {
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
            return wrap(new Vector4d(a).add(b), metatables);
        }));
        metatable.rawset(SUB, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return wrap(new Vector4d(a).sub(b), metatables);
        }));
        metatable.rawset(MUL, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return wrap(new Vector4d(a).mul(b), metatables);
        }));
        metatable.rawset(DIV, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return wrap(new Vector4d(a).div(b), metatables);
        }));
        metatable.rawset(MOD, LibFunction.create((s, v1, v2) -> {
            Vector4d a = v1.optUserdata(s, Vector4d.class, null);
            Vector4d b = v2.optUserdata(s, Vector4d.class, null);
            if (a == null) a = new Vector4d(v1.checkDouble(s));
            if (b == null) b = new Vector4d(v2.checkDouble(s));
            return wrap(new Vector4d(
                    OperationHelper.mod(a.x, b.x),
                    OperationHelper.mod(a.y, b.y),
                    OperationHelper.mod(a.z, b.z),
                    OperationHelper.mod(a.w, b.w)
            ), metatables);
        }));
        metatable.rawset(UNM, LibFunction.create((s, v) -> {
            Vector4d a = v.checkUserdata(s, Vector4d.class);
            return wrap(new Vector4d(a).negate(), metatables);
        }));

        // # operator can also be used for vector length.
        metatable.rawset(LEN, LibFunction.create((s, v) ->
                valueOf(v.checkUserdata(s, Vector4d.class).length())
        ));
        // Type name, used in error messages
        metatable.rawset(NAME, valueOf("Vec4", t));

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

        // Indexing. Check metatable first, otherwise turn into swizzle.
        metatable.rawset(INDEX, LibFunction.create((s, v, k) -> {
            // Check if it's a method, if so return it
            LuaValue method = metatable.rawget(k);
            if (!method.isNil()) return method;

            // Otherwise, custom index
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
            // Key should be a string then
            LuaString key = k.checkLuaString(s);
            // Otherwise, get the swizzle going
            try {
                return switch (key.length()) {
                    case 1 -> valueOf(getSwizzle(self, (char) key.charAt(0)));
                    case 2 -> Vector2API.wrap(new Vector2d(
                            getSwizzle(self, (char) key.charAt(0)),
                            getSwizzle(self, (char) key.charAt(1))
                    ), metatables);
                    case 3 -> Vector3API.wrap(new Vector3d(
                            getSwizzle(self, (char) key.charAt(0)),
                            getSwizzle(self, (char) key.charAt(1)),
                            getSwizzle(self, (char) key.charAt(2))
                    ), metatables);
                    case 4 -> Vector4API.wrap(new Vector4d(
                            getSwizzle(self, (char) key.charAt(0)),
                            getSwizzle(self, (char) key.charAt(1)),
                            getSwizzle(self, (char) key.charAt(2)),
                            getSwizzle(self, (char) key.charAt(3))
                    ), metatables);
                    default -> NIL; // Not a swizzle, return nil
                };
            } catch (InvalidSwizzleException ex) {
                // If swizzle failed, just return nil
                return NIL;
            }
        }));

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
    // r,g,b,a = x,y,z,w
    // u,v = x,y
    // underscore = 0
    // i = 1
    // capital letters negate the value
    private static double getSwizzle(Vector4d v, char c) throws InvalidSwizzleException {
        return switch (c) {
            case '_' -> 0;
            case 'i' -> 1;
            case 'I' -> -1;
            case 'x', 'r', 'u' -> v.x;
            case 'y', 'g', 'v' -> v.y;
            case 'z', 'b' -> v.z;
            case 'w', 'a' -> v.w;
            case 'X', 'R', 'U' -> -v.x;
            case 'Y', 'G', 'V' -> -v.y;
            case 'Z', 'B' -> -v.z;
            case 'W', 'A' -> -v.w;
            default -> throw new InvalidSwizzleException(); // Signal to caller to return nil
        };
    }
    private static void setSwizzle(Vector4d v, char c, double num, @Nullable AllocationTracker tracker) throws LuaError {
        switch (c) {
            case '_' -> {}
            case 'x', 'r', 'u' -> v.x = num;
            case 'y', 'g', 'v' -> v.y = num;
            case 'z', 'b' -> v.z = num;
            case 'w', 'a' -> v.w = num;
            case 'X', 'R', 'U' -> v.x = -num;
            case 'Y', 'G', 'V' -> v.y = -num;
            case 'Z', 'B' -> v.z = -num;
            case 'W', 'A' -> v.w = -num;
            default -> throw new LuaError("Invalid swizzle character to Vec4: '" + c + "'", tracker); // Swizzle assignment should error on invalid
        }
    }
    private static class InvalidSwizzleException extends Exception {}

}
