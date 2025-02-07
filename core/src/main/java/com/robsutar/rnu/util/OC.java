package com.robsutar.rnu.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Object conversion, supports YAML and GSON
 */
@SuppressWarnings("unchecked")
public class OC {
    private OC() {

    }

    private static <T> T orGet(Object o, Class<T> type, Supplier<T> supplier) {
        if (type.isInstance(o))
            return type.cast(o);
        return supplier.get();
    }

    private static <T> T or(Object o, Class<T> type, T defaultValue) {
        return orGet(o, type, () -> defaultValue);
    }

    public static Integer intValueOrGet(Object o, Supplier<Number> supplier) {
        return orGet(o, Number.class, supplier).intValue();
    }

    public static Integer intValueOr(Object o, Integer defaultValue) {
        return or(o, Number.class, defaultValue).intValue();
    }

    public static Integer intValue(Object o) {
        return intValueOrGet(o, () -> {
            throw new IllegalArgumentException(o + " is not " + Number.class);
        });
    }

    public static Long longValueOrGet(Object o, Supplier<Number> supplier) {
        return orGet(o, Number.class, supplier).longValue();
    }

    public static Long longValueOr(Object o, Long defaultValue) {
        return or(o, Number.class, defaultValue).longValue();
    }

    public static Long longValue(Object o) {
        return longValueOrGet(o, () -> {
            throw new IllegalArgumentException(o + " is not " + Number.class);
        });
    }

    public static Float floatValueOrGet(Object o, Supplier<Number> supplier) {
        return orGet(o, Number.class, supplier).floatValue();
    }

    public static Float floatValueOr(Object o, Float defaultValue) {
        return or(o, Number.class, defaultValue).floatValue();
    }

    public static Float floatValue(Object o) {
        return floatValueOrGet(o, () -> {
            throw new IllegalArgumentException(o + " is not " + Number.class);
        });
    }

    public static Double doubleValueOrGet(Object o, Supplier<Number> supplier) {
        return orGet(o, Number.class, supplier).doubleValue();
    }

    public static Double doubleValueOr(Object o, Double defaultValue) {
        return or(o, Number.class, defaultValue).doubleValue();
    }

    public static Double doubleValue(Object o) {
        return doubleValueOrGet(o, () -> {
            throw new IllegalArgumentException(o + " is not " + Number.class);
        });
    }

    public static String strOrGet(Object o, Supplier<String> supplier) {
        return orGet(o, String.class, supplier);
    }

    public static String strOr(Object o, String defaultObj) {
        return strOrGet(o, () -> defaultObj);
    }

    public static String str(Object o) {
        return strOrGet(o, () -> {
            throw new IllegalArgumentException(o + " is not " + String.class);
        });
    }

    public static Boolean boolOrGet(Object o, Supplier<Boolean> supplier) {
        return orGet(o, Boolean.class, supplier);
    }

    public static Boolean boolOr(Object o, Boolean defaultObj) {
        return boolOrGet(o, () -> defaultObj);
    }

    public static Boolean bool(Object o) {
        return boolOrGet(o, () -> {
            throw new IllegalArgumentException(o + " is not " + Boolean.class);
        });
    }

    @Deprecated
    public static void map(String wrongUsage) {

    }

    public static <K, V> Map<K, V> map(Object o) {
        if (o == null)
            return new HashMap<>();
        if (o instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) o;
            if (map.isEmpty())
                return new HashMap<>();
            return (Map<K, V>) map;
        }
        throw new IllegalArgumentException(o + " is not " + Map.class);
    }

    public static <E> List<E> list(Object o) {
        if (o == null)
            return new ArrayList<>();
        if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            if (list.isEmpty())
                return new ArrayList<>();
            return (List<E>) list;
        }
        throw new IllegalArgumentException(o + " is not " + List.class);
    }

    public static <T, R> List<R> listOf(Object oList, Function<T, R> func) {
        List<T> list = list(oList);
        List<R> exit = new ArrayList<R>();
        for (T o : list)
            exit.add(func.apply(o));
        return exit;
    }

    public static <RawKey, RawV, K, V> Map<K, V> mapOfWithKey(Object oMap, Function<Map.Entry<RawKey, RawV>, Pair<K, V>> func) {
        Map<RawKey, RawV> map = map(oMap);
        Map<K, V> exit = new HashMap<K, V>();
        for (Map.Entry<RawKey, RawV> entry : map.entrySet()) {
            Pair<K, V> pair = func.apply(entry);
            exit.put(pair.a(), pair.b());
        }
        return exit;
    }

    public static <BothKey, RawV, V> Map<BothKey, V> mapOf(Object oMap, Function<Map.Entry<BothKey, RawV>, V> func) {
        Map<BothKey, RawV> map = map(oMap);
        Map<BothKey, V> exit = new HashMap<BothKey, V>();
        for (Map.Entry<BothKey, RawV> entry : map.entrySet())
            exit.put(entry.getKey(), func.apply(entry));
        return exit;
    }
}
