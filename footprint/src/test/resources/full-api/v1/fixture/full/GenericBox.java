package fixture.full;

import java.util.List;

public final class GenericBox {
	private Object value;

	public GenericBox() {
		// no-op
	}

	public GenericBox(Object value) {
		this.value = value;
	}

	public Object get() {
		return value;
	}

	public void set(Object value) {
		this.value = value;
	}

	public <U> U pick(U left, U right) {
		return left;
	}

	public List<Object> upper(List<Object> input) {
		return input;
	}

	public void lower(List<Object> input, Object element) {
		if (input != null) {
			input.add(element);
		}
	}
}
