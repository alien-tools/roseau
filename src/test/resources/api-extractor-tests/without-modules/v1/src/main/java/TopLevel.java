

// Top-level classes are either public or package-private
class TopLevelClass {
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

interface TopLevelInterface {
	// Every nested declaration in an interface is implicitly, and obligatorily, public
	// Nested classes in interfaces are implicitly static (no inner classes)
	class NestedClass {}
	interface NestedInterface {}
	record NestedRecord() {}
	enum NestedEnum {}
	@interface NestedAnnotation {}
}

record TopLevelRecord() {
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

enum TopLevelEnum {
	A, B, C;

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

@interface TopLevelAnnotation {
	// Nested declarations in annotations are the same as interfaces
	class InnerClass {}
	interface NestedInterface {}
	record NestedRecord() {}
	enum NestedEnum {}
	@interface NestedAnnotation {}
}
