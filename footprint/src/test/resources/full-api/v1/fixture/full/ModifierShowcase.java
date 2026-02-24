package fixture.full;

public class ModifierShowcase {
	public volatile long volatileCounter = 0L;
	public transient int transientValue = 1;

	public synchronized void increment() {
		volatileCounter++;
		transientValue++;
	}

	public static strictfp double sum(double left, double right) {
		return left + right;
	}
}
