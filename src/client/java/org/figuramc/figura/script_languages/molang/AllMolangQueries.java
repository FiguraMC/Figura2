package org.figuramc.figura.script_languages.molang;

import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.avatars.components.MolangStateComponent;
import org.figuramc.figura.script_languages.molang.ast.FunctionCall;
import org.figuramc.figura.script_languages.molang.ast.Literal;
import org.figuramc.figura.script_languages.molang.ast.MolangExpr;
import org.figuramc.figura.script_languages.molang.ast.control_flow.Compound;
import org.figuramc.figura.script_languages.molang.ast.control_flow.LogicalAnd;
import org.figuramc.figura.script_languages.molang.ast.control_flow.LogicalOr;
import org.figuramc.figura.script_languages.molang.ast.control_flow.Return;
import org.figuramc.figura.script_languages.molang.ast.vars.TempVariable;
import org.figuramc.figura.script_languages.molang.ast.vars.TempVariableAssign;
import org.figuramc.figura.script_languages.molang.compile.MolangCompileException;
import org.figuramc.figura.script_languages.molang.func.ComparisonOperator;

import java.util.HashMap;
import java.util.List;

/**
 * Class for holding all the queries defined by Figura
 */
public class AllMolangQueries {

    // Queries on various actor types
    public static final HashMap<String, MolangInstance.Query<? super Object>> BASIC_QUERIES = new HashMap<>();
    public static final HashMap<String, MolangInstance.Query<? super MolangStateComponent>> AVATAR_QUERIES = new HashMap<>();


    static {
        // Default molang queries
        BASIC_QUERIES.put("all", (parser, args, source, funcNameStart, funcNameEnd) -> {
            // query.all(a, b, c, d) desugars to { temp = a; return temp == b && temp == c && temp == d }
            if (args.size() < 3)
                throw new MolangCompileException("figura.error.script.molang.compile.wrong_arg_count", source, funcNameStart, funcNameEnd, "query.all", "at least 3", args.size());
            List<Integer> distinctSizes = args.stream().map(MolangExpr::returnCount).filter(x -> x != 1).distinct().toList();
            if (distinctSizes.size() >= 2)
                throw new MolangCompileException("figura.error.script.molang.compile.vector_args_same_size", source, funcNameStart, funcNameEnd, "query.all", distinctSizes.get(0), distinctSizes.get(1));
            Compound res = parser.pushScope();
            TempVariable temp = parser.declareTempVar("$temp", args.getFirst().returnCount(), -1, -1);
            res.exprs.add(new TempVariableAssign(temp, args.getFirst()));
            MolangExpr andChain = new FunctionCall(ComparisonOperator.EQ_OP, List.of(temp, args.getLast())); // temp == d
            for (int i = args.size() - 2; i > 0; i--) {
                andChain = new LogicalAnd(new FunctionCall(ComparisonOperator.EQ_OP, List.of(temp, args.get(i))), andChain);
            }
            res.exprs.add(new Return(andChain));
            parser.popScope();
            return res;
        });
        BASIC_QUERIES.put("any", (parser, args, source, funcNameStart, funcNameEnd) -> {
            // query.any(a, b, c, d) desugars to { temp = a; return temp == b || temp == c || temp == d }
            if (args.size() < 3)
                throw new MolangCompileException("figura.error.script.molang.compile.wrong_arg_count", source, funcNameStart, funcNameEnd, "query.any", "at least 3", args.size());
            List<Integer> distinctSizes = args.stream().map(MolangExpr::returnCount).filter(x -> x != 1).distinct().toList();
            if (distinctSizes.size() >= 2)
                throw new MolangCompileException("figura.error.script.molang.compile.vector_args_same_size", source, funcNameStart, funcNameEnd, "query.any", distinctSizes.get(0), distinctSizes.get(1));
            Compound res = parser.pushScope();
            TempVariable temp = parser.declareTempVar("$temp", args.getFirst().returnCount(), -1, -1);
            res.exprs.add(new TempVariableAssign(temp, args.getFirst()));
            MolangExpr orChain = new FunctionCall(ComparisonOperator.EQ_OP, List.of(temp, args.getLast())); // temp == d
            for (int i = args.size() - 2; i > 0; i--) {
                orChain = new LogicalOr(new FunctionCall(ComparisonOperator.EQ_OP, List.of(temp, args.get(i))), orChain);
            }
            res.exprs.add(new Return(orChain));
            parser.popScope();
            return res;
        });
        BASIC_QUERIES.put("in_range", (parser, args, source, funcNameStart, funcNameEnd) -> {
            // query.in_range(a, b, c) desugars to { temp = a; return a >= b && a <= c }
            if (args.size() != 3)
                throw new MolangCompileException("figura.error.script.molang.compile.wrong_arg_count", source, funcNameStart, funcNameEnd, "query.in_range", 3, args.size());
            List<Integer> distinctSizes = args.stream().map(MolangExpr::returnCount).filter(x -> x != 1).distinct().toList();
            if (distinctSizes.size() >= 2)
                throw new MolangCompileException("figura.error.script.molang.compile.vector_args_same_size", source, funcNameStart, funcNameEnd, "query.in_range", distinctSizes.get(0), distinctSizes.get(1));
            Compound res = parser.pushScope();
            TempVariable temp = parser.declareTempVar("$temp", args.getFirst().returnCount(), -1, -1);
            res.exprs.add(new TempVariableAssign(temp, args.getFirst()));
            res.exprs.add(new Return(
                    new LogicalAnd(
                            new FunctionCall(ComparisonOperator.GE_OP, List.of(temp, args.get(1))),
                            new FunctionCall(ComparisonOperator.LE_OP, List.of(temp, args.get(2)))
                    )
            ));
            parser.popScope();
            return res;
        });
        BASIC_QUERIES.put("approx_eq", BASIC_QUERIES.get("all")); // Epsilon is implementation defined, so we'll say it is 0 :D (todo consider changing this)
        BASIC_QUERIES.put("count", (__, args, ___, ____, _____) -> new Literal(args.stream().mapToInt(MolangExpr::returnCount).sum())); // Unpacks any vector args

        // Molang queries for avatar state
        AVATAR_QUERIES.putAll(BASIC_QUERIES);
        AVATAR_QUERIES.put("anim_time", QueryFactory.fromActorMethod("anim_time", MolangStateComponent.class, "anim_time", 0, 1));
        AVATAR_QUERIES.put("life_time", QueryFactory.fromActorMethod("life_time", MolangStateComponent.class, "life_time", 0, 1));
        AVATAR_QUERIES.put("time_stamp", QueryFactory.fromActorMethod("time_stamp", MolangStateComponent.class, "time_stamp", 0, 1));
        AVATAR_QUERIES.put("modified_distance_moved", QueryFactory.fromActorMethod("modified_distance_moved", MolangStateComponent.class, "modified_distance_moved", 0, 1));
        AVATAR_QUERIES.put("modified_move_speed", QueryFactory.fromActorMethod("modified_move_speed", MolangStateComponent.class, "modified_move_speed", 0, 1));
        AVATAR_QUERIES.put("death_ticks", QueryFactory.fromActorMethod("death_ticks", MolangStateComponent.class, "death_ticks", 0, 1));

    }



}
