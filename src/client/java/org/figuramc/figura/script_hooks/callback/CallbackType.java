package org.figuramc.figura.script_hooks.callback;

// Types that can be sent through a callback; statically checked and converted.
// (Algebraic Type)
public sealed interface CallbackType {

    // "Void" / "Nothing" / null
    /* null            */ final class Unit implements CallbackType { public static final Unit INSTANCE = new Unit(); private Unit() {} }

    // Primitives
    /* Boolean         */ final class Bool implements CallbackType { public static final Bool INSTANCE = new Bool(); private Bool() {} }
    /* Double          */ final class F64 implements CallbackType { public static final F64 INSTANCE = new F64(); private F64() {} }

    // Objects
    /* FiguraModelPart */ final class FiguraPart implements CallbackType { public static final FiguraPart INSTANCE = new FiguraPart(); private FiguraPart() {} }

    // Generic types
    /* ArrayList       */ record List(CallbackType element) implements CallbackType {}
    /* HashMap         */ record Map(CallbackType key, CallbackType value) implements CallbackType {}
    /* Object[]        */ record Tuple(CallbackType... elements) implements CallbackType {}
    /* ScriptCallback  */ record Func(CallbackType returnType, CallbackType... paramTypes) implements CallbackType {}
    /* inner or null   */ record Nullable(CallbackType inner) implements CallbackType {}

}
