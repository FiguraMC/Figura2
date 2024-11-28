package org.figuramc.figura.util;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReflectionUtils {

    // Get all fields of a class, including private and inherited ones.
    // - getFields() does not return private fields
    // - getDeclaredFields() does not return inherited fields
    public static Set<Field> getAllFields(Class<?> clazz) {
        if (clazz == null)
            return Set.of();
        Set<Field> result = new HashSet<>();
        Collections.addAll(result, clazz.getDeclaredFields());
        result.addAll(getAllFields(clazz.getSuperclass()));
        return result;
    }

    // Get the values of all fields in a class, removing duplicates
    public static Set<Object> getAllFieldValues(Class<?> clazz, Object instance) {
        Set<Field> fields = getAllFields(clazz);
        Set<Object> result = new HashSet<>();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                result.add(field.get(instance));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("ReflectionUtils failed to access field " + field.getName(), e);
            }
        }
        return result;
    }

}
