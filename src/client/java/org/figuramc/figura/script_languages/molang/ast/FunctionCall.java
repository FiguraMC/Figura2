package org.figuramc.figura.script_languages.molang.ast;

import org.figuramc.figura.script_languages.molang.compile.CompilationContext;
import org.figuramc.figura.script_languages.molang.func.MolangFunction;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

/**
 * Just a simple wrapper around a function and the args to that function.
 */
public class FunctionCall extends MolangExpr {

    private final MolangFunction func;
    private final List<MolangExpr> args;

    public FunctionCall(MolangFunction func, List<MolangExpr> args) {
        this.func = func;
        this.args = args;
    }

    @Override
    public int computeReturnCount() {
        return func.returnCount(args);
    }

    @Override
    public void compile(MethodVisitor visitor, int outputArrayIndex, CompilationContext context) {
        func.compile(visitor, args, outputArrayIndex, context);
    }
}
