package testing_lib.inheritanceIfazeDefaultMethodOverrideDelete;

public interface InheritanceIfazeDefaultMethodOverrideDelete extends Interface1 {

	@Override
	public default void method1() {
		System.out.println("InheritancIfazeDefaultMethodOverrideDelete default method1");
	}
	
}
