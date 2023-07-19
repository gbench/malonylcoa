package gbench.util.function;

@FunctionalInterface
public interface TriConsumer<X, Y, Z> {
	void accept(final X x, final Y y, final Z z);
}