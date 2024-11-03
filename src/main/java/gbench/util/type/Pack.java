package gbench.util.type;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 打包一个对象t的打包对象，使之可以进行链式调用
 * 
 * @param <T> 被封装的值
 */
public class Pack<T> {

	/**
	 * 构造函数
	 * 
	 * @param value
	 */
	public Pack(final T value) {
		this.value = value;
	};

	/**
	 * 值查看
	 * 
	 * @param cs t-&gt;{}
	 * @return
	 */
	public Pack<T> wrap(final Consumer<T> cs) {
		cs.accept(this.value);
		return this;
	};

	/**
	 * 值查看（触发计算）
	 * 
	 * @param cs t-&gt;{}
	 * @return
	 */
	public T evaluate(final Consumer<T> cs) {
		cs.accept(this.value);
		return value;
	};

	/**
	 * 值变换
	 * 
	 * @param <U>    结果对象
	 * @param mapper t-&gt;u
	 * @return
	 */
	public <U> Pack<U> map(final Function<T, U> mapper) {
		final U result = mapper.apply(value);
		if (result == null) {
			throw new IllegalArgumentException("Mapper function cannot return null.");
		}
		return Pack.of(result);
	};

	/**
	 * 值变换
	 * 
	 * @param <U>    结果对象
	 * @param mapper t-&gt;u
	 * @return
	 */
	public <U> Pack<U> flatMap(final Function<T, Pack<U>> mapper) {
		return mapper.apply(value);
	}

	/**
	 * 返回原来的数据
	 * 
	 * @return
	 */
	public T value() {
		return this.value;
	}

	/**
	 * 返回原来的数据
	 * 
	 * @return
	 */
	public Optional<T> valueOpt() {
		return Optional.ofNullable(this.value);
	}

	/**
	 * toString
	 */
	@Override
	public String toString() {
		return String.valueOf(value);
	}

	/**
	 * hashCode
	 * 
	 * @return
	 */
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	/**
	 * 判断
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pack<?> pack) {
			return Objects.equals(this.value, pack.value);
		} else {
			return false;
		}
	}

	/**
	 * 创建一个封装对象
	 * 
	 * @param <T>
	 * @param value
	 * @return
	 */
	public static <T> Pack<T> of(final T value) {
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		return new Pack<>(value);
	}

	private final T value;

}
