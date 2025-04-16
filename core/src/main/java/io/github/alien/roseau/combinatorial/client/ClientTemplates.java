package io.github.alien.roseau.combinatorial.client;

public interface ClientTemplates {
	String FULL_CLIENT_FILE_TEMPLATE = """
			package %s;

			%s

			public class %s {
			%s
				public static void main(String[] args)%s {
					callInstructionsWithoutException();
					callInstructionsWithException();
					callInstructionsWithTryCatch();
			    }

				private static void callInstructionsWithoutException() {
					%s
				}

				private static void callInstructionsWithException()%s {
					%s
				}

				private static void callInstructionsWithTryCatch() {
					%s
				}
			}
			""";

	String CLASS_EXTENSION_TEMPLATE = """
			static class %s extends %s {
			%s
			}
			""";

	String INTERFACE_EXTENSION_TEMPLATE = "static interface %s extends %s {}\n";

	String INTERFACE_IMPLEMENTATION_TEMPLATE = """
			static class %s implements %s {
			%s
			}
			""";
}
