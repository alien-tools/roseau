package com.github.maracas.roseau.combinatorial.client;

public interface ClientTemplates {
	String FILE_TEMPLATE = """
			package %s;
			
			%s""";

	String MAIN_CLASS_TEMPLATE = """
			%s
			
			public class %s {
			    public static void main(String[] args) {
			        %s
			    }
			}
			""";

	String MAIN_THROWING_CLASS_TEMPLATE = """
			%s
			
			public class %s {
			    public static void main(String[] args) throws %s {
			        %s
			    }
			}
			""";

	String CLASS_INHERITANCE_TEMPLATE = """
			%s
			
			class %s extends %s {
			%s
			}
			""";

	String ABSTRACT_CLASS_INHERITANCE_TEMPLATE = """
			%s
			
			abstract class %s extends %s {
			%s
			}
			""";

	String INTERFACE_EXTENSION_TEMPLATE = """
			%s
			
			interface %s extends %s {}
			""";

	String INTERFACE_IMPLEMENTATION_TEMPLATE = """
			%s
			
			class %s implements %s {
			%s
			}
			""";
}
