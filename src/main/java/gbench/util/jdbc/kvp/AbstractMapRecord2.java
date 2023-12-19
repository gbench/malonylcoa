package gbench.util.jdbc.kvp;

import java.util.function.Function;

/**
 * 
 * @author xuqinghua
 *
 * @param <S> the self class
 */
public abstract class AbstractMapRecord2<S extends IRecord> extends AbstractMapRecord {

	@Override
	public IRecord duplicate() {
		return this.duplicate2();
	}

	/**
	 * 
	 * @return
	 */
	public abstract S duplicate2();

	/**
	 * whether has key
	 * 
	 * @return empty status
	 */
	public boolean empty() {
		return this.data == null || this.data.keySet().size() < 1;
	}

	/**
	 * map state
	 * 
	 * @param <U>
	 * @param mapper     state->u
	 * @param emptyValue
	 * @return u value
	 */
	public <U> U map(final Function<S, U> mapper, final U emptyValue) {
		return this.empty() ? emptyValue : this.map(mapper);
	}

	/**
	 * 
	 * @param <U>    the result class
	 * @param mapper s->u
	 * @return U
	 */
	@SuppressWarnings("unchecked")
	public <U> U map(final Function<S, U> mapper) {
		return mapper.apply((S) this);
	}

	/**
	 * self
	 * 
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public S self() {
		return (S) this;
	}

	/**
	 * update and return self(this) <br>
	 * union append mode
	 * 
	 * @param rec the [kvp] to be appended
	 * @return this
	 */
	public S update(final IRecord rec) {
		this.union(rec, true);
		return self();
	}

	/**
	 * update and return a copy
	 * 
	 * @param rec the [kvp] to be appended
	 * @return a copy of this
	 */
	@SuppressWarnings("unchecked")
	public S update2(final IRecord rec) {
		return ((AbstractMapRecord2<S>) this.update(rec)).duplicate2();
	}

	private static final long serialVersionUID = -6173203337428164905L;
}
