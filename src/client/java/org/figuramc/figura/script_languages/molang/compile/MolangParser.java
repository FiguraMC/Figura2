package org.figuramc.figura.script_languages.molang.compile;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_languages.molang.MolangInstance;
import org.figuramc.figura.script_languages.molang.ast.FunctionCall;
import org.figuramc.figura.script_languages.molang.ast.Literal;
import org.figuramc.figura.script_languages.molang.ast.MolangExpr;
import org.figuramc.figura.script_languages.molang.ast.VectorConstructor;
import org.figuramc.figura.script_languages.molang.ast.control_flow.*;
import org.figuramc.figura.script_languages.molang.ast.vars.ActorVariable;
import org.figuramc.figura.script_languages.molang.ast.vars.ActorVariableAssign;
import org.figuramc.figura.script_languages.molang.ast.vars.TempVariable;
import org.figuramc.figura.script_languages.molang.ast.vars.TempVariableAssign;
import org.figuramc.figura.script_languages.molang.func.ComparisonOperator;
import org.figuramc.figura.script_languages.molang.func.FloatFunction;
import org.figuramc.figura.script_languages.molang.func.MolangFunction;
import org.figuramc.figura.util.functional.BiThrowingSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class MolangParser {

    private final String source;
    private final MolangInstance<?> instance;
    private int current;

    private final Stack<Compound> scopes = new Stack<>();
    private int maxLocalVariables = 0; // Store maximum JVM local variables used, so temporaries can go past it

    // Only a MolangInstance should ever construct one of these.
    // Please don't try to use this class on your own.
    public MolangParser(String source, MolangInstance<?> instance) {
        this.source = source;
        this.instance = instance;
        this.current = 0;
    }
    
    public MolangExpr parseAll() throws AvatarError, MolangCompileException {
        if (current != 0) throw new UnsupportedOperationException("Cannot parse with a Parser multiple times!");
        return parse();
    }

    // Get the maximum local variables used at any point in this expr, so temporaries can go past it
    // Note this doesn't include the built-in local variables like "this"
    public int getMaxLocalVariables() {
        return maxLocalVariables;
    }

    // ---------------------
    // | PARSING OPERATORS |
    // ---------------------

    private MolangExpr parse() throws AvatarError, MolangCompileException {
        return parseTernary();
    }

    private MolangExpr parseTernary() throws AvatarError, MolangCompileException {
        MolangExpr res = parseLogicalOr();
        if (consume('?', true)) {
            if (res.isVector()) throw new MolangCompileException("figura.error.script.molang.compile.ternary_condition_expects_scalar", source, current - 1, current);
            MolangExpr ifTrue = parseLogicalOr();
            if (!consume(':', true)) throw new MolangCompileException("figura.error.script.molang.compile.expected_ternary_colon", source, current - 1, current);
            int falseStart = current;
            MolangExpr ifFalse = parseTernary();
            if (ifTrue.returnCount() != ifFalse.returnCount())
                throw new MolangCompileException("figura.error.script.molang.compile.ternary_branches_must_be_same_size", source, falseStart, current, ifTrue.returnCount(), ifFalse.returnCount());
            return new Ternary(res, ifTrue, ifFalse);
        }
        return res;
    }

    private MolangExpr parseLogicalOr() throws AvatarError, MolangCompileException {
        MolangExpr res = parseLogicalAnd();
        while (consume("||", true)) {
            int start = current - 2; int end = current;
            MolangExpr rhs = parseLogicalAnd();
            if (res.isVector() || rhs.isVector())
                throw new MolangCompileException("figura.error.script.molang.compile.logical_or_expects_scalars", source, start, end);
            res = new LogicalOr(res, rhs);
        }
        return res;
    }

    private MolangExpr parseLogicalAnd() throws AvatarError, MolangCompileException {
        MolangExpr res = parseEquality();
        while (consume("&&", true)) {
            int start = current - 2; int end = current;
            MolangExpr rhs = parseEquality();
            if (res.isVector() || rhs.isVector())
                throw new MolangCompileException("figura.error.script.molang.compile.logical_and_expects_scalars", source, start, end);
            res = new LogicalAnd(res, rhs);
        }
        return res;
    }

    private MolangExpr parseEquality() throws AvatarError, MolangCompileException {
        MolangExpr res = parseComparison();
        while (consume("==", true) || consume("!=", true))
            res = new FunctionCall(switch (last(2)) {
                case "==" -> ComparisonOperator.EQ_OP;
                case "!=" -> ComparisonOperator.NE_OP;
                default -> throw new IllegalStateException();
            }, List.of(res, parseComparison()));
        return res;
    }

    private MolangExpr parseComparison() throws AvatarError, MolangCompileException {
        MolangExpr res = parseSum();
        while (consumeAny("><", true)) {
            if (consume('=', false)) {
                res = new FunctionCall(switch (last(2)) {
                    case "<=" -> ComparisonOperator.LE_OP;
                    case ">=" -> ComparisonOperator.GE_OP;
                    default -> throw new IllegalStateException();
                }, List.of(res, parseSum()));
            } else {
                res = new FunctionCall(switch (last()) {
                    case "<" -> ComparisonOperator.LT_OP;
                    case ">" -> ComparisonOperator.GT_OP;
                    default -> throw new IllegalStateException();
                }, List.of(res, parseSum()));
            }
        }
        return res;
    }

    private MolangExpr parseSum() throws AvatarError, MolangCompileException {
        MolangExpr res = parseProduct();
        while (consumeAny("+-", true))
            res = new FunctionCall(switch (last()) {
                case "+" -> FloatFunction.ADD_OP;
                case "-" -> FloatFunction.SUB_OP;
                default -> throw new IllegalStateException();
            }, List.of(res, parseProduct()));
        return res;
    }

    private MolangExpr parseProduct() throws AvatarError, MolangCompileException {
        MolangExpr res = parseUnary();
        while (consumeAny("*/%", true))
            res = new FunctionCall(switch (last()) {
                case "*" -> FloatFunction.MUL_OP;
                case "/" -> FloatFunction.DIV_OP;
                case "%" -> FloatFunction.MOD_OP;
                default -> throw new IllegalStateException();
            }, List.of(res, parseUnary()));
        return res;
    }

    private MolangExpr parseUnary() throws AvatarError, MolangCompileException {
        if (consumeAny("-!", true))
            return new FunctionCall(switch (last()) {
                case "-" -> FloatFunction.NEG_OP;
                case "!" -> throw new UnsupportedOperationException("TODO");
                default -> throw new IllegalStateException();
            }, List.of(parseUnary()));
        return parseAtom();
    }

    // ---------
    // | ATOMS |
    // ---------

    private MolangExpr parseAtom() throws AvatarError, MolangCompileException {
        if (consumeDigit(true)) return finishNumber();
        if (consume("math.", true)) return finishMath();
        if (consume('q', true)) return finishQuery();
        if (consume('t', true)) return finishTemp();
        if (consume('v', true)) return finishActorVar();
        if (consume('(', true)) return finishParen();
        if (consume('{', true)) return finishBlock();
        if (consume('[', true)) return finishVectorConstructor();
        if (consume("return ", true)) {
            int pre = current - 7;
            // Ensure we're inside a block before returning
            if (scopes.isEmpty())
                throw new MolangCompileException("figura.error.script.molang.compile.return_outside_block", source, pre, current - 1);
            // Ensure return size lines up
            MolangExpr e = parse();
            if (e.isVector()) {
                // If it's a vector, ensure it doesn't conflict with existing vectors being returned
                int retCount = e.returnCount();
                int prevRetCount = scopes.peek().getCurrentReturnCount();
                if (prevRetCount == 1) {
                    scopes.peek().setCurrentReturnCount(retCount);
                } else if (prevRetCount != retCount) {
                    throw new MolangCompileException("figura.error.script.molang.compile.diff_return_sizes", source, pre, current, prevRetCount, retCount);
                }
            }
            return new Return(e);
        }
        throw new MolangCompileException("figura.error.script.molang.compile.expected_expression", source, current - 1, current);
    }

    // First digit was just consumed
    private MolangExpr finishNumber() throws MolangCompileException {
        int start = current - 1;
        boolean foundDot = false;
        while (true) {
            if (consume('.', false)) {
                if (foundDot) throw new MolangCompileException("figura.error.script.molang.compile.number_parse", source, start, current);
                foundDot = true;
            }
            if (!consumeDigit(false)) {
                return new Literal(Float.parseFloat(source.substring(start, current)));
            }
        }
    }

    // "math." was already parsed
    private MolangExpr finishMath() throws AvatarError, MolangCompileException {
        int start = current - 5;
        String s = expectIdent();
        int funcNameEnd = current;
        MolangFunction function = MolangFunction.ALL_MATH_FUNCTIONS.get(s);
        if (function == null) throw new MolangCompileException("figura.error.script.molang.compile.unknown_math", source, start, current, s);
        List<MolangExpr> args = parseParams();
        function.checkArgs(args, source, start, funcNameEnd);
        return new FunctionCall(function, args);
    }

    // "t" was already parsed
    private MolangExpr finishTemp() throws AvatarError, MolangCompileException {
        int start = current - 1;
        // Get variable name
        if (!(consume('.', false) || consume("emp.", false)))
            throw new MolangCompileException("figura.error.script.molang.compile.expected_temp_var", source, start, current);
        String varName = expectIdent();
        // Find existing variable
        Optional<TempVariable> existing = scopes.stream().map(x -> x.tempVars).flatMap(List::stream).filter(it -> it.name.equals(varName)).findAny();
        // Check if this is an assignment
        if (!check("==", true) && consume('=', true)) {
            int equals = current - 1;
            // Parse RHS
            MolangExpr rhs = parse();
            // If the variable already exists, ensure size matches then emit assignment for it
            if (existing.isPresent()) {
                TempVariable existingVar = existing.get();
                if (existingVar.size != rhs.returnCount())
                    throw new MolangCompileException("figura.error.script.molang.compile.incompatible_var_size", source, equals, equals + 1, "t." + varName, existingVar.size, rhs.returnCount());
                return new TempVariableAssign(existingVar, rhs);
            }
            // Otherwise, declare it
            TempVariable newVariable = declareTempVar(varName, rhs.returnCount(), start, equals);
            scopes.peek().tempVars.add(newVariable);
            // Return assignment
            return new TempVariableAssign(newVariable, rhs);
        } else {
            // If this isn't an assignment, but the var doesn't exist, error
            if (existing.isEmpty())
                throw new MolangCompileException("figura.error.script.molang.compile.nonexistent_temp_var", source, start, current, varName);
            // Return variable
            return existing.get();
        }
    }

    // "v" was parsed
    private MolangExpr finishActorVar() throws AvatarError, MolangCompileException {
        int start = current - 1;
        if (!(consume('.', false) || consume("ariable.", false)))
            throw new MolangCompileException("figura.error.script.molang.compile.expected_actor_var", source, start, current);
        // To support vectors, we need to know at compile time how many elements are in this variable.
        // We use the syntax "v.size_integer.name" to facilitate this. When the integer is not present, size is assumed to be 1.
        int varSize = 1;
        if (consumeDigit(false)) {
            int countStart = current - 1;
            while (consumeDigit(false));
            String s = source.substring(countStart, current);
            varSize = Integer.parseInt(s);
            if (varSize <= 1) throw new MolangCompileException("figura.error.script.molang.compile.var_size_too_low", source, countStart, current);
            if (!consume('$', false)) throw new MolangCompileException("figura.error.script.molang.compile.expect_dollar_after_var_size", source, countStart, current, s);
        }
        String varName = (varSize == 1 ? expectIdent() : varSize + "$" + expectIdent());
        ActorVariable variable = instance.getOrCreateActorVariable(varName, varSize); // Get the variable
        if (!check("==", true) && consume('=', true)) {
            int equals = current - 1;
            MolangExpr rhs = parse();
            if (rhs.returnCount() != variable.size)
                throw new MolangCompileException("figura.error.script.molang.compile.incompatible_var_size", source, equals, equals + 1, "v." + varName, variable.size, rhs.returnCount());
            return new ActorVariableAssign(variable, parse());
        } else {
            return variable;
        }
    }

    // "q" was parsed
    private MolangExpr finishQuery() throws AvatarError, MolangCompileException {
        int start = current - 1;
        if (!(consume('.', false) || consume("uery.", false)))
            throw new MolangCompileException("figura.error.script.molang.compile.expected_query", source, start, current);
        String queryName = expectIdent();
        int afterFuncName = current;
        MolangInstance.Query<?> query = instance.getQuery(queryName);
        if (query == null) throw new MolangCompileException("figura.error.script.molang.compile.unknown_query", source, start, current, queryName);
        return query.bind(this, parseParams(), source, start, afterFuncName);
    }

    // ( was already consumed
    private MolangExpr finishParen() throws AvatarError, MolangCompileException {
        MolangExpr res = parse();
        if (!consume(')', true))
            throw new MolangCompileException("figura.error.script.molang.compile.expected_close_paren", source, current - 1, current);
        return res;
    }

    // { was already consumed
    private MolangExpr finishBlock() throws AvatarError, MolangCompileException {
        Compound c = pushScope();
        separatedList(';', '}', true, () -> c.exprs.add(parse()));
        popScope();
        return c;
    }

    // [ was already consumed
    private MolangExpr finishVectorConstructor() throws AvatarError, MolangCompileException {
        int start = current - 1;
        List<MolangExpr> exprs = separatedList(',', ']', true, this::parse);
        if (exprs.size() <= 1)
            throw new MolangCompileException("figura.error.script.molang.compile.vector_constructor_expects_two_args", source, start, current);
        return new VectorConstructor(exprs);
    }

    // ------------------
    // | FUNCTION CALLS |
    // ------------------

    private List<MolangExpr> parseParams() throws AvatarError, MolangCompileException {
        if (!consume('(', true)) return List.of();
        return separatedList(',', ')', false, this::parse);
    }

    private <T> List<T> separatedList(char separator, char end, boolean allowTrailingSeparator, BiThrowingSupplier<T, AvatarError, MolangCompileException> parser) throws AvatarError, MolangCompileException {
        if (consume(end, true)) return List.of();
        ArrayList<T> res = new ArrayList<>();
        while (true) {
            res.add(parser.get());
            if (!consume(separator, true)) {
                if (!consume(end, true))
                    throw new MolangCompileException("figura.error.script.molang.compile.expected_list", source, current - 1, current, separator, end);
                return res;
            } else if (allowTrailingSeparator) {
                if (consume(end, true)) return res;
            }
        }
    }

    // -------------------
    // | EXPOSED HELPERS |
    // -------------------

    public Compound pushScope() {
        Compound res = new Compound();
        scopes.push(res);
        return res;
    }

    public void popScope() {
        scopes.pop().finish();
    }

    // Pass error locations. When calling from outside the parser, just pass -1 for varStart and equalsSign, since it can't error.
    public TempVariable declareTempVar(String name, int size, int varStart, int equalsSign) throws MolangCompileException {
        // If there are no scopes, error
        if (scopes.isEmpty())
            throw new MolangCompileException("figura.error.script.molang.compile.temp_var_outside_block", source, varStart, equalsSign, name);
        // Add it to scope. Find the next unused index:
        int nextIndex = scopes.reversed().stream().map(x -> x.tempVars).map(List::reversed).flatMap(List::stream).filter(v -> v.isVector() == (size != 1)).findFirst().map(it -> it.getLogicalLocation() + it.size).orElse(0);
        if (size == 1) maxLocalVariables = Math.max(maxLocalVariables, nextIndex + 1);
        return new TempVariable(name, size, nextIndex);
    }

    // ----------
    // | LEXING |
    // ----------

    private boolean consumeAny(String toks, boolean skipWhitespace) {
        if (skipWhitespace) skipWhitespace();
        if (current == source.length()) return false;
        return (toks.indexOf(source.charAt(current)) != -1) && advance();
    }

    private boolean consume(char tok, boolean skipWhitespace) {
        if (skipWhitespace) skipWhitespace();
        if (current == source.length()) return false;
        return source.charAt(current) == tok && advance();
    }

    private boolean consume(String tok, boolean skipWhitespace) {
        if (skipWhitespace) skipWhitespace();
        int start = current;
        for (int i = 0; i < tok.length(); i++) {
            if (!consume(tok.charAt(i), false)) {
                current = start;
                return false;
            }
        }
        return true;
    }

    private boolean check(String tok, boolean skipWhitespace) {
        int start = current;
        boolean success = consume(tok, skipWhitespace);
        if (success) current = start;
        return success;
    }

    private boolean consumeDigit(boolean skipWhitespace) {
        if (skipWhitespace) skipWhitespace();
        if (current == source.length()) return false;
        return isDigit(source.charAt(current)) && advance();
    }
    private boolean consumeIdentChar(boolean skipWhitespace) {
        if (skipWhitespace) skipWhitespace();
        if (current == source.length()) return false;
        return isIdentChar(source.charAt(current)) && advance();
    }

    private String expectIdent() throws MolangCompileException {
        int start = current;
        while (consumeIdentChar(false));
        if (start == current) throw new MolangCompileException("figura.error.script.molang.compile.expected_name", source, current - 1, current);
        return source.substring(start, current);
    }

    private void skipWhitespace() {
        while (current < source.length() && isWhitespace(source.charAt(current)))
            current++;
    }

    private boolean advance() {
        current++;
        return true;
    }

    private String last() {
        return last(1);
    }
    private String last(int count) {
        return source.substring(current - count, current);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentChar(char c) {
        return c >= 'a' && c <= 'z' || c == '.' || c == '_';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

}
