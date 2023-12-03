import java.util.List;

public class A {
	public A selfReference;
	public B publicApiReference;
	public C privateApiReference;
	public int primitiveReference;
	public String jdkReference;
	public List<String> jdkParameterizedReference;
	public List<B> jdkParameterizedApiReference;
	public int[] primitiveArrayReference;
	public String[] jdkArrayReference;
	public C[] apiArrayReference;
}