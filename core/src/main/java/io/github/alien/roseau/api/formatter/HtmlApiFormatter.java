package io.github.alien.roseau.api.formatter;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Formats a {@link LibraryTypes} as a self-contained HTML page showing the library's public API. Types are grouped by
 * package, and each type section lists its fields, constructors, methods, and type-specific members (enum values,
 * record components, annotation methods).
 */
public final class HtmlApiFormatter {
	private final LibraryTypes libraryTypes;
	private final Set<String> exportedTypeNames;

	/**
	 * Creates a new formatter.
	 *
	 * @param libraryTypes      the library types to render
	 * @param exportedTypeNames the qualified names of the types considered exported; only these types are shown
	 */
	public HtmlApiFormatter(LibraryTypes libraryTypes, Set<String> exportedTypeNames) {
		this.libraryTypes = libraryTypes;
		this.exportedTypeNames = exportedTypeNames;
	}

	/**
	 * Generates the full HTML report.
	 *
	 * @return the HTML string
	 */
	public String format() {
		List<TypeDecl> types = libraryTypes.getAllTypes().stream()
			.filter(t -> exportedTypeNames.contains(t.getQualifiedName()))
			.sorted(Comparator.comparing(TypeDecl::getQualifiedName))
			.toList();

		StringBuilder sb = new StringBuilder(16_000 + 2_000 * types.size());
		DateTimeFormatter human = DateTimeFormatter.ofPattern("MMMM d, uuuu 'at' h:mm a", Locale.ENGLISH);
		String generatedAt = ZonedDateTime.now(ZoneId.systemDefault()).format(human);

		sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n");
		sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		sb.append("<title>Roseau API Report</title>\n");
		sb.append("<style>\n").append(BASE_CSS).append("</style>\n");
		sb.append(EARLY_THEME_SCRIPT);
		sb.append("</head>\n<body>\n");

		// Header
		sb.append("<header class=\"header\">\n<div class=\"page\">");
		sb.append("<div class=\"titles\"><h1>Roseau API Report</h1>\n");
		sb.append("<div class=\"subtitle\">").append(escape(libraryTypes.getLibrary().getLocation().toString()));
		sb.append(" &mdash; Generated ").append(escape(generatedAt)).append("</div></div>\n");
		sb.append("<button id=\"theme-toggle\" class=\"theme-toggle\" title=\"Toggle theme\">🌙</button>");
		sb.append("</div></header>\n");

		// Main content
		sb.append("<main class=\"page\">\n");

		// Summary cards
		appendSummary(sb, types);

		// Table of contents
		appendToc(sb, types);

		// Type details
		sb.append("<section class=\"details\">\n");
		for (TypeDecl type : types) {
			appendTypeDetail(sb, type);
		}
		sb.append("</section>\n");

		sb.append("</main>\n");
		sb.append("<footer class=\"footer\">Made with <span class=\"heart\">&#10084;</span> by Roseau</footer>\n");
		sb.append(THEME_TOGGLE_SCRIPT);
		sb.append("</body>\n</html>\n");
		return sb.toString();
	}

	// ---- Summary ----

	private void appendSummary(StringBuilder sb, List<TypeDecl> types) {
		long classes = types.stream().filter(t -> t.isClass() && !t.isEnum() && !t.isRecord()).count();
		long interfaces = types.stream().filter(t -> t.isInterface() && !t.isAnnotation()).count();
		long enums = types.stream().filter(TypeDecl::isEnum).count();
		long records = types.stream().filter(TypeDecl::isRecord).count();
		long annotations = types.stream().filter(TypeDecl::isAnnotation).count();

		sb.append("<section class=\"summary\">\n");

		// Module / Package info
		ModuleDecl module = libraryTypes.getModule();
		sb.append("<div class=\"card\">\n<h2>Library</h2>\n<div class=\"lib-meta\">");
		sb.append("<span><span class=\"muted\">Module:</span> ")
			.append(escape(module.getQualifiedName())).append("</span>");
		long pkgCount = types.stream().map(TypeDecl::getPackageName).distinct().count();
		sb.append("<span><span class=\"muted\">Packages:</span> ").append(pkgCount).append("</span>");
		sb.append("<span><span class=\"muted\">Exported types:</span> ").append(types.size())
			.append(" / ").append(libraryTypes.getAllTypes().size()).append("</span>");
		sb.append("</div>\n</div>\n");

		// Metrics
		sb.append("<div class=\"card metrics\">\n<h2>Types</h2>\n<div class=\"metrics-grid\">\n");
		sb.append(metric("Classes", Long.toString(classes), "class"));
		sb.append(metric("Interfaces", Long.toString(interfaces), "interface"));
		sb.append(metric("Enums", Long.toString(enums), "enum"));
		sb.append(metric("Records", Long.toString(records), "record"));
		sb.append(metric("Annotations", Long.toString(annotations), "annotation"));
		sb.append("</div>\n</div>\n");
		sb.append("</section>\n");
	}

	// ---- Table of Contents ----

	private void appendToc(StringBuilder sb, List<TypeDecl> types) {
		sb.append("<section class=\"toc card\">\n<h2>Contents</h2>\n");
		sb.append("<input id=\"toc-search\" class=\"toc-search\" type=\"text\" placeholder=\"Filter types...\">\n");

		Map<String, List<TypeDecl>> byPkg = new TreeMap<>();
		for (TypeDecl type : types) {
			byPkg.computeIfAbsent(type.getPackageName(), _ -> new ArrayList<>()).add(type);
		}

		byPkg.forEach((pkg, pkgTypes) -> {
			sb.append("<div class=\"toc-pkg\">\n");
			sb.append("<h3 class=\"pkg-name\">").append(escape(pkg.isEmpty() ? "(default)" : pkg)).append("</h3>\n");
			sb.append("<ul class=\"toc-list\">\n");
			pkgTypes.forEach(type -> {
				sb.append("<li data-name=\"")
					.append(escape(type.getQualifiedName().toLowerCase(Locale.ROOT))).append("\">");
				sb.append("<span class=\"kind-badge kind-").append(kindCss(type)).append("\">")
					.append(kindBadge(type)).append("</span> ");
				sb.append("<a href=\"#").append(anchor(type)).append("\">")
					.append(escape(type.getSimpleName())).append("</a>");
				sb.append("</li>\n");
			});
			sb.append("</ul>\n</div>\n");
		});

		sb.append("</section>\n");
	}

	// ---- Type Detail ----

	private void appendTypeDetail(StringBuilder sb, TypeDecl type) {
		sb.append("<article class=\"card type\" id=\"").append(anchor(type)).append("\">\n");

		// Declaration line
		sb.append("<h2>");
		sb.append("<span class=\"kind-badge kind-").append(kindCss(type)).append("\">")
			.append(kindBadge(type)).append("</span> ");
		appendDeclarationLine(sb, type);
		sb.append("</h2>\n");

		// Annotations
		if (!type.getAnnotations().isEmpty()) {
			sb.append("<div class=\"ann-list\">");
			appendAnnotations(sb, type.getAnnotations());
			sb.append("</div>\n");
		}

		// Superclass / interfaces / permitted types
		appendTypeRelations(sb, type);

		// Type-specific sections
		switch (type) {
			case RecordDecl record -> appendRecordComponents(sb, record);
			case EnumDecl enumDecl -> appendEnumValues(sb, enumDecl);
			case AnnotationDecl annDecl -> appendAnnotationMethods(sb, annDecl);
			default -> {}
		}

		// Fields
		if (!type.getDeclaredFields().isEmpty()) {
			appendFieldsTable(sb, type.getDeclaredFields());
		}

		// Constructors
		if (type instanceof ClassDecl classDecl && !classDecl.getDeclaredConstructors().isEmpty()) {
			appendConstructorsTable(sb, classDecl.getDeclaredConstructors());
		}

		// Methods
		if (!type.getDeclaredMethods().isEmpty()) {
			appendMethodsTable(sb, type.getDeclaredMethods());
		}

		sb.append("</article>\n");
	}

	private void appendDeclarationLine(StringBuilder sb, TypeDecl type) {
		sb.append("<code>");
		sb.append(escape(type.getVisibility().toString())).append(" ");
		appendModifiers(sb, type.getModifiers());
		sb.append(escape(kindKeyword(type))).append(" ");
		sb.append("<strong>").append(escape(type.getSimpleName())).append("</strong>");
		appendFormalTypeParams(sb, type.getFormalTypeParameters());
		sb.append("</code>");
	}

	private void appendTypeRelations(StringBuilder sb, TypeDecl type) {
		boolean hasRelations = false;

		if (type instanceof ClassDecl classDecl) {
			TypeReference<ClassDecl> superClass = classDecl.getSuperClass();
			if (!superClass.equals(TypeReference.OBJECT) && !superClass.equals(TypeReference.RECORD)
				&& !superClass.equals(TypeReference.ENUM)) {
				if (!hasRelations) {
					sb.append("<div class=\"relations\">");
					hasRelations = true;
				}
				sb.append("<div><span class=\"muted\">extends</span> <code>")
					.append(renderTypeRef(superClass)).append("</code></div>");
			}
		}

		if (!type.getImplementedInterfaces().isEmpty()) {
			if (!hasRelations) {
				sb.append("<div class=\"relations\">");
				hasRelations = true;
			}
			String keyword = type.isInterface() ? "extends" : "implements";
			sb.append("<div><span class=\"muted\">").append(keyword).append("</span> <code>");
			sb.append(type.getImplementedInterfaces().stream()
				.map(this::renderTypeRef)
				.collect(Collectors.joining(", ")));
			sb.append("</code></div>");
		}

		if (!type.getPermittedTypes().isEmpty()) {
			if (!hasRelations) {
				sb.append("<div class=\"relations\">");
				hasRelations = true;
			}
			sb.append("<div><span class=\"muted\">permits</span> <code>");
			sb.append(type.getPermittedTypes().stream()
				.map(this::renderTypeRef)
				.collect(Collectors.joining(", ")));
			sb.append("</code></div>");
		}

		if (hasRelations) {
			sb.append("</div>\n");
		}
	}

	// ---- Type-specific sections ----

	private void appendRecordComponents(StringBuilder sb, RecordDecl record) {
		List<RecordComponentDecl> components = record.getRecordComponents();
		if (components.isEmpty()) {
			return;
		}
		sb.append("<div class=\"member-section\">\n<h3>Record Components</h3>\n");
		sb.append("<table class=\"member-table\"><thead><tr><th>Type</th><th>Name</th></tr></thead><tbody>\n");
		for (RecordComponentDecl comp : components) {
			sb.append("<tr><td><code>").append(renderTypeRef(comp.getType())).append("</code></td>");
			sb.append("<td>").append(escape(comp.getSimpleName()));
			if (comp.isVarargs()) {
				sb.append("...");
			}
			appendAnnotationPills(sb, comp.getAnnotations());
			sb.append("</td></tr>\n");
		}
		sb.append("</tbody></table>\n</div>\n");
	}

	private void appendEnumValues(StringBuilder sb, EnumDecl enumDecl) {
		Set<EnumValueDecl> values = enumDecl.getValues();
		if (values.isEmpty()) {
			return;
		}
		sb.append("<div class=\"member-section\">\n<h3>Enum Values</h3>\n");
		sb.append("<div class=\"enum-values\">");
		values.stream()
			.sorted(Comparator.comparing(Symbol::getSimpleName))
			.forEach(v -> {
				sb.append("<span class=\"enum-value\">").append(escape(v.getSimpleName()));
				appendAnnotationPills(sb, v.getAnnotations());
				sb.append("</span>");
			});
		sb.append("</div>\n</div>\n");
	}

	private void appendAnnotationMethods(StringBuilder sb, AnnotationDecl annDecl) {
		Set<AnnotationMethodDecl> methods = annDecl.getAnnotationMethods();
		if (methods.isEmpty()) {
			return;
		}
		sb.append("<div class=\"member-section\">\n<h3>Annotation Elements</h3>\n");
		sb.append("<table class=\"member-table\"><thead><tr>")
			.append("<th>Type</th><th>Name</th><th>Default</th></tr></thead><tbody>\n");
		methods.stream()
			.sorted(Comparator.comparing(Symbol::getSimpleName))
			.forEach(m -> {
				sb.append("<tr><td><code>").append(renderTypeRef(m.getType())).append("</code></td>");
				sb.append("<td>").append(escape(m.getSimpleName())).append("()");
				appendAnnotationPills(sb, m.getAnnotations());
				sb.append("</td>");
				sb.append("<td>").append(m.hasDefault() ? "Yes" : "").append("</td></tr>\n");
			});
		sb.append("</tbody></table>\n</div>\n");
	}

	// ---- Fields / Constructors / Methods tables ----

	private void appendFieldsTable(StringBuilder sb, Collection<FieldDecl> fields) {
		sb.append("<div class=\"member-section\">\n<h3>Fields</h3>\n");
		sb.append("<table class=\"member-table\"><thead><tr>")
			.append("<th>Visibility</th><th>Modifiers</th><th>Type</th><th>Name</th></tr></thead><tbody>\n");
		fields.stream()
			.sorted(Comparator.comparing(Symbol::getSimpleName))
			.forEach(f -> {
				sb.append("<tr>");
				sb.append("<td>").append(escape(f.getVisibility().toString())).append("</td>");
				sb.append("<td>").append(renderModifiers(f.getModifiers())).append("</td>");
				sb.append("<td><code>").append(renderTypeRef(f.getType())).append("</code></td>");
				sb.append("<td>").append(escape(f.getSimpleName()));
				appendAnnotationPills(sb, f.getAnnotations());
				sb.append("</td>");
				sb.append("</tr>\n");
			});
		sb.append("</tbody></table>\n</div>\n");
	}

	private void appendConstructorsTable(StringBuilder sb, Collection<ConstructorDecl> constructors) {
		sb.append("<div class=\"member-section\">\n<h3>Constructors</h3>\n");
		sb.append("<table class=\"member-table\"><thead><tr>")
			.append("<th>Visibility</th><th>Signature</th><th>Throws</th></tr></thead><tbody>\n");
		constructors.stream()
			.sorted(Comparator.comparing(ConstructorDecl::getSignature))
			.forEach(c -> {
				sb.append("<tr>");
				sb.append("<td>").append(escape(c.getVisibility().toString())).append("</td>");
				sb.append("<td><code>");
				appendFormalTypeParams(sb, c.getFormalTypeParameters());
				sb.append(escape(c.getSimpleName())).append("(");
				sb.append(renderParameters(c.getParameters()));
				sb.append(")");
				sb.append("</code>");
				appendAnnotationPills(sb, c.getAnnotations());
				sb.append("</td>");
				sb.append("<td>").append(renderThrown(c.getThrownExceptions())).append("</td>");
				sb.append("</tr>\n");
			});
		sb.append("</tbody></table>\n</div>\n");
	}

	private void appendMethodsTable(StringBuilder sb, Collection<MethodDecl> methods) {
		sb.append("<div class=\"member-section\">\n<h3>Methods</h3>\n");
		sb.append("<table class=\"member-table\"><thead><tr>")
			.append("<th>Visibility</th><th>Modifiers</th><th>Return Type</th><th>Signature</th><th>Throws</th>")
			.append("</tr></thead><tbody>\n");
		methods.stream()
			.sorted(Comparator.comparing(MethodDecl::getSignature))
			.forEach(m -> {
				sb.append("<tr>");
				sb.append("<td>").append(escape(m.getVisibility().toString())).append("</td>");
				sb.append("<td>").append(renderModifiers(m.getModifiers())).append("</td>");
				sb.append("<td><code>").append(renderTypeRef(m.getType())).append("</code></td>");
				sb.append("<td><code>");
				appendFormalTypeParams(sb, m.getFormalTypeParameters());
				sb.append(escape(m.getSimpleName())).append("(");
				sb.append(renderParameters(m.getParameters()));
				sb.append(")");
				sb.append("</code>");
				appendAnnotationPills(sb, m.getAnnotations());
				sb.append("</td>");
				sb.append("<td>").append(renderThrown(m.getThrownExceptions())).append("</td>");
				sb.append("</tr>\n");
			});
		sb.append("</tbody></table>\n</div>\n");
	}

	// ---- Rendering helpers ----

	private void appendModifiers(StringBuilder sb, Set<Modifier> modifiers) {
		if (!modifiers.isEmpty()) {
			sb.append(modifiers.stream()
				.map(m -> escape(m.toString()))
				.collect(Collectors.joining(" ")));
			sb.append(" ");
		}
	}

	private String renderModifiers(Set<Modifier> modifiers) {
		return modifiers.stream()
			.map(m -> escape(m.toString()))
			.collect(Collectors.joining(" "));
	}

	private void appendFormalTypeParams(StringBuilder sb, List<FormalTypeParameter> ftps) {
		if (!ftps.isEmpty()) {
			sb.append("&lt;");
			for (int i = 0; i < ftps.size(); i++) {
				if (i > 0) {
					sb.append(", ");
				}
				FormalTypeParameter ftp = ftps.get(i);
				sb.append(escape(ftp.name()));
				List<ITypeReference> bounds = ftp.bounds();
				if (!bounds.isEmpty() && !(bounds.size() == 1 && bounds.getFirst().equals(TypeReference.OBJECT))) {
					sb.append(" extends ");
					sb.append(bounds.stream().map(this::renderTypeRef).collect(Collectors.joining(" &amp; ")));
				}
			}
			sb.append("&gt; ");
		}
	}

	private String renderTypeRef(ITypeReference ref) {
		if (ref == null) {
			return "";
		}
		if (ref instanceof TypeReference<?> tr) {
			StringBuilder s = new StringBuilder();
			s.append(escape(simpleNameOf(tr.getQualifiedName())));
			if (!tr.typeArguments().isEmpty()) {
				s.append("&lt;");
				s.append(tr.typeArguments().stream().map(this::renderTypeRef).collect(Collectors.joining(", ")));
				s.append("&gt;");
			}
			return s.toString();
		}
		return escape(ref.toString());
	}

	private String renderParameters(List<ParameterDecl> params) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			ParameterDecl p = params.get(i);
			s.append(renderTypeRef(p.type()));
			if (p.isVarargs()) {
				s.append("...");
			}
			s.append(" ").append(escape(p.name()));
		}
		return s.toString();
	}

	private String renderThrown(Set<ITypeReference> thrown) {
		if (thrown.isEmpty()) {
			return "";
		}
		return "<code>" + thrown.stream().map(this::renderTypeRef).collect(Collectors.joining(", ")) + "</code>";
	}

	private void appendAnnotations(StringBuilder sb, Set<Annotation> annotations) {
		for (Annotation ann : annotations) {
			sb.append("<span class=\"ann-pill\">@").append(escape(simpleNameOf(ann.actualAnnotation().getQualifiedName())));
			if (!ann.values().isEmpty()) {
				sb.append("(");
				sb.append(ann.values().entrySet().stream()
					.map(e -> escape(e.getKey()) + "=" + escape(e.getValue()))
					.collect(Collectors.joining(", ")));
				sb.append(")");
			}
			sb.append("</span> ");
		}
	}

	private void appendAnnotationPills(StringBuilder sb, Set<Annotation> annotations) {
		if (!annotations.isEmpty()) {
			sb.append(" ");
			appendAnnotations(sb, annotations);
		}
	}

	// ---- Static helpers ----

	private static String escape(String s) {
		return s == null ? "" : s
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

	private static String anchor(TypeDecl type) {
		return type.getQualifiedName()
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-|-$)", "");
	}

	private static String simpleNameOf(String qualifiedName) {
		int dot = qualifiedName.lastIndexOf('.');
		return dot >= 0 ? qualifiedName.substring(dot + 1) : qualifiedName;
	}

	private static String kindKeyword(TypeDecl type) {
		return switch (type) {
			case RecordDecl _ -> "record";
			case EnumDecl _ -> "enum";
			case AnnotationDecl _ -> "@interface";
			case InterfaceDecl _ -> "interface";
			case ClassDecl _ -> "class";
		};
	}

	private static String kindBadge(TypeDecl type) {
		return switch (type) {
			case RecordDecl _ -> "R";
			case EnumDecl _ -> "E";
			case AnnotationDecl _ -> "@";
			case InterfaceDecl _ -> "I";
			case ClassDecl _ -> "C";
		};
	}

	private static String kindCss(TypeDecl type) {
		return switch (type) {
			case RecordDecl _ -> "record";
			case EnumDecl _ -> "enum";
			case AnnotationDecl _ -> "annotation";
			case InterfaceDecl _ -> "interface";
			case ClassDecl _ -> "class";
		};
	}

	private static String metric(String label, String value, String tone) {
		return ("<div class=\"metric\" data-tone=\"%s\"><div class=\"metric-value\">%s</div>" +
			"<div class=\"metric-label\">%s</div></div>").formatted(tone, escape(value), escape(label));
	}

	// ---- CSS ----

	private static final String BASE_CSS = """
		:root{--bg:#0b1020;--text:#e6e9f5;--muted:#9aa4d6;--header-start:#111736;--header-end:#0b1020;
		--header-title:#ffffff;--subtitle:#9aa4d6;--card-bg:#0f1531;--card-border:#1b2452;--card-shadow:#1a2147;
		--chip-bg:#0c1127;--chip-border:#1a2147;--link:#d7dbf2;--pill-bg:#202a61;--pill-text:#b8c1ff;--metric-bg:#0c1127;
		--metric-border:#1a2147;--member-border:#1b2452;--footer:#9aa4d6;
		--code-bg:#0c1127;--code-border:#1a2147;
		--kind-class-bg:#1a3a6b;--kind-class-text:#8cc4ff;--kind-class-border:#2a5090;
		--kind-interface-bg:#1a4a2e;--kind-interface-text:#8cf5b0;--kind-interface-border:#2a6e44;
		--kind-enum-bg:#4a3a0a;--kind-enum-text:#ffd88a;--kind-enum-border:#6b540e;
		--kind-record-bg:#3a1a5a;--kind-record-text:#d4a5f5;--kind-record-border:#5a2a7a;
		--kind-annotation-bg:#5a1a1a;--kind-annotation-text:#ffb3b3;--kind-annotation-border:#7a2a2a;
		--ann-bg:#202a61;--ann-text:#b8c1ff;--ann-border:#2a3d77;
		--table-header-bg:#111736;--table-row-hover:#151d40;--table-border:#1b2452;
		--search-bg:#0c1127;--search-border:#1b2452;--search-text:#e6e9f5;
		--enum-value-bg:#0c1127;--enum-value-border:#1a2147;
		--relations-bg:#0c1127;--relations-border:#1a2147}
		[data-theme=light]{--bg:#ffffff;--text:#0f1230;--muted:#556083;--header-start:#f5f7ff;--header-end:#ffffff;
		--header-title:#111111;--subtitle:#556083;--card-bg:#ffffff;--card-border:#dfe5fb;--card-shadow:#e6e9f5;
		--chip-bg:#f6f8ff;--chip-border:#dfe5fb;--link:#0f1230;--pill-bg:#e9edff;--pill-text:#1f3bb3;
		--metric-bg:#f6f8ff;--metric-border:#dfe5fb;--member-border:#e3e8ff;--footer:#556083;
		--code-bg:#f6f8ff;--code-border:#e6e9f5;
		--kind-class-bg:#dbeeff;--kind-class-text:#0a4a8a;--kind-class-border:#b3d9ff;
		--kind-interface-bg:#e0ffe8;--kind-interface-text:#0a5a2a;--kind-interface-border:#b3eec3;
		--kind-enum-bg:#fff5db;--kind-enum-text:#7a5a0a;--kind-enum-border:#ffe2a6;
		--kind-record-bg:#f3e8ff;--kind-record-text:#5a1a8a;--kind-record-border:#dbb3ff;
		--kind-annotation-bg:#ffe8e8;--kind-annotation-text:#8a1a1a;--kind-annotation-border:#ffb3b3;
		--ann-bg:#e9edff;--ann-text:#1f3bb3;--ann-border:#d2dbff;
		--table-header-bg:#f0f3ff;--table-row-hover:#f6f8ff;--table-border:#dfe5fb;
		--search-bg:#ffffff;--search-border:#dfe5fb;--search-text:#0f1230;
		--enum-value-bg:#f6f8ff;--enum-value-border:#dfe5fb;
		--relations-bg:#f6f8ff;--relations-border:#dfe5fb}
		body{font-family:Inter,system-ui,Segoe UI,Roboto,Helvetica,Arial,sans-serif;line-height:1.5;margin:0;
		background:var(--bg);color:var(--text)}.page{max-width:1100px;margin:0 auto;padding:0 16px}
		.header{padding:16px 0;background:linear-gradient(180deg,var(--header-start),var(--header-end))}
		.header .page{display:flex;gap:12px;align-items:center;justify-content:space-between}
		.header h1{margin:0;font-size:28px;color:var(--header-title)}
		.header .subtitle{color:var(--subtitle);margin-top:6px}
		.theme-toggle{border:1px solid var(--card-border);background:var(--chip-bg);color:var(--text);
		border-radius:999px;padding:6px 10px;cursor:pointer;font-size:14px}
		.summary{display:grid;gap:16px;padding:16px}
		.card{background:var(--card-bg);border:1px solid var(--card-border);border-radius:12px;padding:16px;
		box-shadow:0 1px 0 var(--card-shadow)}
		.lib-meta{color:var(--text);opacity:.9;margin-top:6px}
		.lib-meta span{display:inline-block;margin-right:10px}
		.metrics-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:12px}
		.metric{background:var(--metric-bg);border:1px solid var(--metric-border);border-radius:10px;padding:12px;
		text-align:center}.metric-value{font-size:24px;font-weight:700}
		.metric-label{font-size:12px;color:var(--muted)}
		.toc{margin:0 16px 8px}
		.toc-search{width:100%;box-sizing:border-box;padding:8px 12px;margin-bottom:12px;border-radius:8px;
		border:1px solid var(--search-border);background:var(--search-bg);color:var(--search-text);font-size:14px}
		.toc-search::placeholder{color:var(--muted)}
		.toc-pkg{margin-bottom:8px}
		.toc-list{list-style:none;padding-left:0;margin:0;display:flex;flex-wrap:wrap;gap:8px}
		.toc-list li{background:var(--chip-bg);border:1px solid var(--chip-border);border-radius:999px;
		padding:4px 10px;display:flex;align-items:center;gap:4px}
		.toc-list a{color:var(--link);text-decoration:none}
		.pkg-name{margin:8px 0 6px;font-size:16px;color:var(--muted)}
		.kind-badge{display:inline-block;width:20px;height:20px;line-height:20px;text-align:center;font-weight:700;
		font-size:11px;border-radius:4px;flex-shrink:0}
		.kind-class{background:var(--kind-class-bg);color:var(--kind-class-text);border:1px solid var(--kind-class-border)}
		.kind-interface{background:var(--kind-interface-bg);color:var(--kind-interface-text);
		border:1px solid var(--kind-interface-border)}
		.kind-enum{background:var(--kind-enum-bg);color:var(--kind-enum-text);border:1px solid var(--kind-enum-border)}
		.kind-record{background:var(--kind-record-bg);color:var(--kind-record-text);
		border:1px solid var(--kind-record-border)}
		.kind-annotation{background:var(--kind-annotation-bg);color:var(--kind-annotation-text);
		border:1px solid var(--kind-annotation-border)}
		.details{display:grid;gap:16px;padding:16px}.type h2{margin:0 0 6px;display:flex;align-items:center;gap:8px}
		.type h2 code{font-size:16px;font-weight:400}
		.relations{background:var(--relations-bg);border:1px solid var(--relations-border);border-radius:8px;
		padding:8px 12px;margin:8px 0;font-size:14px}
		.relations div{margin:2px 0}
		.member-section{margin-top:12px;padding-top:8px;border-top:1px solid var(--member-border)}
		.member-section h3{margin:0 0 8px;font-size:15px;color:var(--muted)}
		.member-table{width:100%;border-collapse:collapse;font-size:14px}
		.member-table th{text-align:left;padding:6px 8px;background:var(--table-header-bg);
		border-bottom:2px solid var(--table-border);font-weight:600;font-size:12px;color:var(--muted)}
		.member-table td{padding:6px 8px;border-bottom:1px solid var(--table-border);vertical-align:top}
		.member-table tr:hover td{background:var(--table-row-hover)}
		code{background:var(--code-bg);border:1px solid var(--code-border);border-radius:6px;padding:2px 4px;
		font-size:13px}
		.ann-list{margin:4px 0}
		.ann-pill{display:inline-block;background:var(--ann-bg);color:var(--ann-text);
		border:1px solid var(--ann-border);border-radius:999px;padding:2px 8px;font-size:11px;margin:1px 2px}
		.enum-values{display:flex;flex-wrap:wrap;gap:8px}
		.enum-value{background:var(--enum-value-bg);border:1px solid var(--enum-value-border);border-radius:8px;
		padding:4px 10px;font-family:monospace;font-size:13px}
		.footer{color:var(--footer);text-align:center;padding:24px}.heart{color:#ff6b9a}
		.muted{color:var(--muted)}""";

	// ---- JS ----

	private static final String EARLY_THEME_SCRIPT = """
		<script>(function(){try{var s=localStorage.getItem('roseau_theme');if(s){document.documentElement.setAttribute\
		('data-theme',s);}else{var prefers=matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light';document.\
		documentElement.setAttribute('data-theme',prefers);}}catch(e){}})();</script>""";

	private static final String THEME_TOGGLE_SCRIPT = """
		<script>(function(){
		function setIcon(){try{var t=document.documentElement.getAttribute('data-theme')||'light';
		var b=document.getElementById('theme-toggle');if(b){b.textContent=t==='dark'?'\\u{1F319}':'\\u{2600}\\u{FE0F}';}\
		}catch(e){}};setIcon();
		var btn=document.getElementById('theme-toggle');if(btn){btn.addEventListener('click',function(){try{var cur=\
		document.documentElement.getAttribute('data-theme')||'dark';var next=cur==='dark'?'light':'dark';document.\
		documentElement.setAttribute('data-theme',next);localStorage.setItem('roseau_theme',next);setIcon();\
		}catch(e){}});}
		var search=document.getElementById('toc-search');if(search){search.addEventListener('input',function(){
		var q=this.value.toLowerCase();document.querySelectorAll('.toc-list li').forEach(function(li){
		li.style.display=(li.dataset.name||'').indexOf(q)>=0?'':'none';});
		document.querySelectorAll('.toc-pkg').forEach(function(pkg){
		var visible=pkg.querySelectorAll('.toc-list li[style=\"\"], .toc-list li:not([style])');
		pkg.style.display=visible.length?'':'none';});});}
		document.querySelectorAll('.type h2').forEach(function(h2){h2.style.cursor='pointer';
		h2.addEventListener('click',function(){var art=this.closest('article');if(art){
		var sections=art.querySelectorAll('.relations,.member-section,.ann-list,.enum-values');
		var hidden=art.classList.toggle('collapsed');
		sections.forEach(function(s){s.style.display=hidden?'none':'';});}});});
		})();</script>""";
}
