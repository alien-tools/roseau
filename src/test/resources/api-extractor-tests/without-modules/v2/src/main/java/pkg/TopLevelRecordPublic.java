package pkg;

public record TopLevelRecordPublic() {
	// Inner classes
	class InnerClass {}
	public class InnerClassPublic {}
	protected class InnerClassProtected {}
	private class InnerClassPrivate {}

	// Nested classes
	static class NestedClass {}
	public static class NestedClassPublic {}
	protected static class NestedClassProtected {}
	private static class NestedClassPrivate {}

	// Nested interfaces are implicitly static (no inner interfaces)
	interface NestedInterface {}
	public interface NestedInterfacePublic {}
	protected interface NestedInterfaceProtected {}
	private interface NestedInterfacePrivate {}

	// Nested records are implicitly static (no inner records)
	record NestedRecord() {}
	public record NestedRecordPublic() {}
	protected record NestedRecordProtected() {}
	private record NestedRecordPrivate() {}

	// Nested enums are implicitly static (no inner enums)
	enum NestedEnum {}
	public enum NestedEnumPublic {}
	protected enum NestedEnumProtected {}
	private enum NestedEnumPrivate {}

	// Annotations are interfaces so nested annotations are implicitly static (no inner annotations)
	@interface NestedAnnotation {}
	public @interface NestedAnnotationPublic {}
	protected @interface NestedAnnotationProtected {}
	private @interface NestedAnnotationPrivate {}
}
