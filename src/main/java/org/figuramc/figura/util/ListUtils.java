package org.figuramc.figura.util;

import org.figuramc.figura.util.exception.functional.BiThrowingFunction;
import org.figuramc.figura.util.exception.functional.ThrowingFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;

import java.util.*;

@SuppressWarnings({"DuplicatedCode"})
public class ListUtils {

    public static <T> void swap(List<T> list, int i, int j) {
        T elem = list.get(i);
        list.set(i, list.get(j));
        list.set(j, elem);
    }

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
            if (out != null) result.add(out);
        }
        result.trimToSize();
        return result;
    }

    public static <T, R, E1 extends Throwable, E2 extends Throwable> ArrayList<@NotNull R> mapBiThrowingNonNull(Iterable<T> list, BiThrowingFunction<T, @Nullable R, E1, E2> func) throws E1, E2 {
        ArrayList<R> result = new ArrayList<>();
        for (T t : list) {
            @Nullable R out = func.apply(t);
            if (out != null) result.add(out);
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

    public static <T> ArrayList<T> flatten(Iterable<? extends Iterable<T>> lists) {
        ArrayList<T> result = new ArrayList<>();
        for (Iterable<T> l : lists)
            for (T v : l)
                result.add(v);
        return result;
    }

    public static <A, B> ArrayList<Pair<A, B>> zip(List<A> a, List<B> b) {
        if (a.size() != b.size()) throw new IllegalArgumentException("Zipped lists must have same length");
        ArrayList<Pair<A, B>> result = new ArrayList<>(a.size());
        for (int i = 0; i < a.size(); i++) {
            result.add(new Pair<>(a.get(i), b.get(i)));
        }
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

    public static <T, E extends Throwable> int indexOf(Iterable<T> iterable, T value) {
        int index = 0;
        for (T item : iterable) {
            if (Objects.equals(value, item))
                return index;
            index++;
        }
        return -1;
    }

    public static <T, E extends Throwable> int findIndex(Iterable<T> list, ThrowingFunction<T, Boolean, E> predicate) throws E {
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

    public static <T, K, V, E extends Throwable> LinkedHashMap<K, List<V>> toPairsAndMerge(Iterable<T> list, ThrowingFunction<T, Pair<K, V>, E> pairFunc) throws E {
        LinkedHashMap<K, List<V>> res = new LinkedHashMap<>();
        for (T elem : list) {
            Pair<K, V> pair = pairFunc.apply(elem);
            res.computeIfAbsent(pair.getA(), k -> new ArrayList<>()).add(pair.getB());
        }
        return res;
    }


}
