package org.figuramc.figura.script_languages.molang.ast;

import org.figuramc.figura.util.BytecodeUtil;
import org.figuramc.figura.script_languages.molang.compile.CompilationContext;
import org.objectweb.asm.MethodVisitor;

// A literal of a floating point value
public class Literal extends MolangExpr {

    public final float value;

    public Literal(float value) {
        this.value = value;
    }

    @Override
    protected int computeReturnCount() {
        return 1;
    }

    @Override
    public void compile(MethodVisitor visitor, int outputArrayIndex, CompilationContext context) {
        BytecodeUtil.constFloat(visitor, value);
    }
}
