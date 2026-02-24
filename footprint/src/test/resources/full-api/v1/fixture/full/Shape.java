package fixture.full;

public sealed interface Shape permits Circle, Rectangle {
	double area();

	default String kind() {
		return getClass().getSimpleName();
	}

	static Shape unit() {
		return new Circle(1.0d);
	}
}
