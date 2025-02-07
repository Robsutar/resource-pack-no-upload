package com.robsutar.rnu.util;

import java.util.Objects;

public final class Pair<A, B> {
    private final A a;
    private final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A a() {
        return a;
    }

    public B b() {
        return b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        Pair that = (Pair) obj;
        return Objects.equals(this.a, that.a) &&
                Objects.equals(this.b, that.b);
    }

    @Override
    public String toString() {
        return "Pair[" +
                "a=" + a + ", " +
                "b=" + b + ']';
    }

}
