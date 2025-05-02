package io.github.alien.roseau.combinatorial.client;

public interface ClientTemplates {
	String FULL_CLIENT_FILE_TEMPLATE = """
			package %s;

			%s

			public class %s {
				%s

				public static void main(String[] args)%s {
					%s
			    }

			%s
			}
			""";

	String CALL_INSTRUCTIONS_WITHOUT_EXCEPTION_TEMPLATE = """
				private static void %s() {
					%s
				}
			""";

	String CALL_INSTRUCTIONS_WITH_EXCEPTION_TEMPLATE = """
				private static void %s()%s {
					%s
				}
			""";

	String CLASS_EXTENSION_TEMPLATE = """
			static class %s extends %s {
			%s
			}
			""";

	String INTERFACE_EXTENSION_TEMPLATE = "static interface %s extends %s {}";

	String INTERFACE_IMPLEMENTATION_TEMPLATE = """
			static class %s implements %s {
			%s
			}
			""";
}
