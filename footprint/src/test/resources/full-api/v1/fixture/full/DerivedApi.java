package fixture.full;

import java.io.IOException;
import java.util.List;

public class DerivedApi extends BaseApi implements Service, ThrowingOps, AutoCloseable {
	public int mutableCounter = 0;
	protected String protectedName = "derived";

	public DerivedApi() {
		super();
	}

	public DerivedApi(int seed) throws CheckedProblem {
		super(seed);
	}

	protected DerivedApi(String name) throws DerivedCheckedProblem {
		super(name);
		this.protectedName = name;
	}

	@Override
	public Number compute(Number input) throws CheckedProblem {
		if (input == null) {
			throw new CheckedProblem("input");
		}
		return input.intValue() + 1;
	}

	@Override
	public void read() throws IOException {
		// no-op
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	protected void protectedMethod() throws CheckedProblem {
		super.protectedMethod();
	}

	public void consumeList(List<? super Integer> sink) {
		if (sink != null) {
			sink.add(1);
		}
	}

	public List<? extends Number> produceList() {
		return List.of(1, 2, 3);
	}

	public static DerivedApi create() {
		return new DerivedApi();
	}
}
