package org.figuramc.figura.util;

import org.figuramc.figura.util.exception.functional.ThrowingBiFunction;
import org.figuramc.figura.util.exception.functional.ThrowingFunction;

import java.util.*;

@SuppressWarnings("unused")
public class MapUtils {

    public static <K, V1, V2, E extends Throwable> Map<K, V2> mapValues(Map<K, V1> map, ThrowingFunction<V1, V2, E> func) throws E {
        Map<K, V2> result = new HashMap<>();
        for (Map.Entry<K, V1> entry : map.entrySet())
            result.put(entry.getKey(), func.apply(entry.getValue()));
        return result;
    }

    public static <K, V1, V2, E extends Throwable> Map<K, V2> mapValues(Map<K, V1> map, ThrowingBiFunction<K, V1, V2, E> func) throws E {
        Map<K, V2> result = new HashMap<>();
        for (Map.Entry<K, V1> entry : map.entrySet())
            result.put(entry.getKey(), func.apply(entry.getKey(), entry.getValue()));
        return result;
    }

    public static <K1, K2, V, E extends Throwable> Map<K2, V> mapKeys(Map<K1, V> map, ThrowingFunction<K1, K2, E> func) throws E {
        Map<K2, V> result = new HashMap<>();
        for (Map.Entry<K1, V> entry : map.entrySet())
            result.put(func.apply(entry.getKey()), entry.getValue());
        return result;
    }

    public static <K, V> Map<K, List<V>> merge(List<Map<K, V>> maps) {
        Map<K, List<V>> result = new LinkedHashMap<>();
        for (Map<K, V> map : maps)
            for (Map.Entry<K, V> entry : map.entrySet())
                result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        return result;
    }

}
