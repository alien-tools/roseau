package fixture.full;

public class GenericHolder<T> {
	public T item;
	protected T protectedItem;

	public GenericHolder() {
		// no-op
	}

	public GenericHolder(T item) {
		this.item = item;
		this.protectedItem = item;
	}

	public T get() {
		return item;
	}

	public void set(T item) {
		this.item = item;
	}

	public <U> U convert(U value) {
		return value;
	}

	protected T protectedGet() {
		return protectedItem;
	}
}
