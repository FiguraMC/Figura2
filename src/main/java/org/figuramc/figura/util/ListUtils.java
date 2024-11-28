package org.figuramc.figura.util;

import org.figuramc.figura.util.exception.BiThrowingFunction;
import org.figuramc.figura.util.exception.ThrowingFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ListUtils {

    public static <T, R, E extends Throwable> ArrayList<R> map(Iterable<T> list, ThrowingFunction<T, R, E> func) throws E {
        ArrayList<R> result = new ArrayList<>();
        for (T t : list) result.add(func.apply(t));
        return result;
    }

    public static <T, R, E1 extends Throwable, E2 extends Throwable> ArrayList<R> mapBiThrowing(Iterable<T> list, BiThrowingFunction<T, R, E1, E2> func) throws E1, E2 {
        ArrayList<R> result = new ArrayList<>();
        for (T t : list) result.add(func.apply(t));
        return result;
    }

    public static <T, R, E extends Throwable> ArrayList<@NotNull R> mapNonNull(Iterable<T> list, ThrowingFunction<T, @Nullable R, E> func) throws E {
        ArrayList<R> result = new ArrayList<>();
        for (T t : list) {
            @Nullable R out = func.apply(t);
            if (out != null)
                result.add(out);
        }
        result.trimToSize();
        return result;
    }

    public static <T, E extends Throwable> ArrayList<T> filter(Iterable<T> list, ThrowingFunction<T, Boolean, E> predicate) throws E {
        ArrayList<T> result = new ArrayList<>();
        for (T t : list)
            if (predicate.apply(t))
                result.add(t);
        result.trimToSize();
        return result;
    }

    public static <T, E extends Throwable> boolean any(Iterable<T> list, ThrowingFunction<T, Boolean, E> predicate) throws E {
        for (T t : list)
            if (predicate.apply(t))
                return true;
        return false;
    }

    public static <T, E extends Throwable> boolean all(Iterable<T> list, ThrowingFunction<T, Boolean, E> predicate) throws E {
        for (T t : list)
            if (predicate.apply(t))
                return false;
        return true;
    }

    public static <T, E extends Throwable> @Nullable T maximal(Iterable<T> list, ThrowingFunction<T, Integer, E> scorer) throws E {
        int bestScore = Integer.MIN_VALUE;
        T best = null;
        for (T t : list)
            if (scorer.apply(t) > bestScore)
                best = t;
        return best;
    }

    public static <T, E extends Throwable> int indexOfMaximal(Iterable<T> list, ThrowingFunction<T, Integer, E> scorer) throws E {
        int bestIndex = -1;
        int index = 0;
        int best = Integer.MIN_VALUE;
        for (T t : list) {
            int score = scorer.apply(t);
            if (score > best) {
                bestIndex = index;
                best = score;
            }
            index++;
        }
        return bestIndex;
    }

    public static <T, E extends Throwable> int filteredIndexOfMaximal(Iterable<T> list, ThrowingFunction<T, Boolean, E> predicate, ThrowingFunction<T, Integer, E> scorer) throws E {
        int bestIndex = -1;
        int index = 0;
        int best = Integer.MIN_VALUE;
        for (T t : list) {
            if (predicate.apply(t)) {
                int score = scorer.apply(t);
                if (score > best) {
                    bestIndex = index;
                    best = score;
                }
            }
            index++;
        }
        return bestIndex;
    }

    public static <T, E extends Throwable> @Nullable T minimal(Iterable<T> list, ThrowingFunction<T, Integer, E> scorer) throws E {
        int bestScore = Integer.MAX_VALUE;
        T best = null;
        for (T t : list)
            if (scorer.apply(t) < bestScore)
                best = t;
        return best;
    }

    public static <T, E extends Throwable> int indexOfMinimal(Iterable<T> list, ThrowingFunction<T, Integer, E> scorer) throws E {
        int bestIndex = -1;
        int index = 0;
        int best = Integer.MAX_VALUE;
        for (T t : list) {
            int score = scorer.apply(t);
            if (score < best) {
                bestIndex = index;
                best = score;
            }
            index++;
        }
        return bestIndex;
    }

    public static <T, E extends Throwable> int filteredIndexOfMinimal(Iterable<T> list, ThrowingFunction<T, Boolean, E> predicate, ThrowingFunction<T, Integer, E> scorer) throws E {
        int bestIndex = -1;
        int index = 0;
        int best = Integer.MAX_VALUE;
        for (T t : list) {
            if (predicate.apply(t)) {
                int score = scorer.apply(t);
                if (score < best) {
                    bestIndex = index;
                    best = score;
                }
            }
            index++;
        }
        return bestIndex;
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T>... lists) {
        ArrayList<T> result = new ArrayList<>();
        for (List<T> list : lists) result.addAll(list);
        result.trimToSize();
        return result;
    }

    public static <T, E extends Throwable> int findIndex(List<T> list, ThrowingFunction<T, Boolean, E> predicate) throws E {
        int index = 0;
        for (T item : list) {
            if (predicate.apply(item))
                return index;
            index++;
        }
        return -1;
    }

    public static <T, K, E extends Throwable> LinkedHashMap<K, T> associateBy(Iterable<T> list, ThrowingFunction<T, K, E> keyFunc) throws E {
        LinkedHashMap<K, T> res = new LinkedHashMap<>();
        for (T elem : list)
            res.put(keyFunc.apply(elem), elem);
        return res;
    }

    public static <T, K, V, E extends Throwable> LinkedHashMap<K, V> associateByTo(Iterable<T> list, ThrowingFunction<T, K, E> keyFunc, ThrowingFunction<T, V, E> valueFunc) throws E {
        LinkedHashMap<K, V> res = new LinkedHashMap<>();
        for (T elem : list)
            res.put(keyFunc.apply(elem), valueFunc.apply(elem));
        return res;
    }

    public static <T, K, E extends Throwable> LinkedHashMap<K, List<T>> associateByAndMerge(Iterable<T> list, ThrowingFunction<T, K, E> keyFunc) throws E {
        LinkedHashMap<K, List<T>> res = new LinkedHashMap<>();
        for (T elem : list) {
            res.computeIfAbsent(keyFunc.apply(elem), k -> new ArrayList<>()).add(elem);
        }
        return res;
    }

}
