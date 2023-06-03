package pkg;

public interface TopLevelInterfacePublic {
	// Every nested declaration in an interface is implicitly, and obligatorily, public
	// Nested classes in interfaces are implicitly static (no inner classes)
	class NestedClass {}
	interface NestedInterface {}
	record NestedRecord() {}
	enum NestedEnum {}
	@interface NestedAnnotation {}
}
