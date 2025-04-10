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
			    }

				private static void callInstructionsWithoutException() {
					%s
				}

				private static void callInstructionsWithException()%s {
					%s
				}
			}
			""";

	String CLASS_EXTENSION_TEMPLATE = """
			class %s extends %s {
			%s
			}
			""";

	String ABSTRACT_CLASS_EXTENSION_TEMPLATE = """
			abstract class %s extends %s {
			%s
			}
			""";

	String INTERFACE_EXTENSION_TEMPLATE = "interface %s extends %s {}\n";

	String INTERFACE_IMPLEMENTATION_TEMPLATE = """
			class %s implements %s {
			%s
			}
			""";
}
