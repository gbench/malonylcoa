package gbench.util.function;

import java.util.function.*;

public class Functions {

	/**
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T> Function<T, T> identity() {
		return new Function<T, T>() {

			@Override
			public T apply(T o) {
				return o;
			}

			@Override
			public String toString() {
				return "Functions.identity()";
			}
		};
	}
}
