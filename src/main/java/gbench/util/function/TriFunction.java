package gbench.util.function;

@FunctionalInterface
public interface TriFunction<X, Y, Z, T> {
    T apply(final X x, final Y y, final Z z);
}