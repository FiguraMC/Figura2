package org.figuramc.figura.script_hooks.callback;

// Types that can be sent through a callback; statically checked and converted.
// (Algebraic Type)
public sealed interface CallbackType {

    // "Void" / "Nothing" / null
    /* null            */ final class Unit implements CallbackType { public static final Unit INSTANCE = new Unit(); private Unit() {} }

    // Primitives
    /* Any             */ final class Any implements CallbackType { public static final Any INSTANCE = new Any(); private Any() {} }
    /* Boolean         */ final class Bool implements CallbackType { public static final Bool INSTANCE = new Bool(); private Bool() {} }
    /* Float           */ final class F32 implements CallbackType { public static final F32 INSTANCE = new F32(); private F32() {} }
    /* Double          */ final class F64 implements CallbackType { public static final F64 INSTANCE = new F64(); private F64() {} }
    /* String          */ final class Str implements CallbackType { public static final Str INSTANCE = new Str(); private Str() {} }

    // Objects
    /* FiguraModelPart */ final class FiguraPart implements CallbackType { public static final FiguraPart INSTANCE = new FiguraPart(); private FiguraPart() {} }

    // Generic types
    /* ArrayList       */ record List(CallbackType element) implements CallbackType {}
    /* HashMap         */ record Map(CallbackType key, CallbackType value) implements CallbackType {}
    /* Object[]        */ record Tuple(CallbackType... elements) implements CallbackType {}
    /* ScriptCallback  */ record Func(CallbackType returnType, CallbackType... paramTypes) implements CallbackType {}
    /* inner or null   */ record Nullable(CallbackType inner) implements CallbackType {}

}
