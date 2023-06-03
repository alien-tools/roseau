public @interface TopLevelAnnotationPublic {
	// Nested declarations in annotations are the same as interfaces
	class InnerClass {}
	interface NestedInterface {}
	record NestedRecord() {}
	enum NestedEnum {}
	@interface NestedAnnotation {}
}
