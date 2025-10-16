package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * An HTML formatter for Roseau reports.
 */
public final class HtmlFormatter implements BreakingChangesFormatter {
	@Override
	public String format(RoseauReport report) {
		StringBuilder sb = new StringBuilder(16_384);

		API apiV1 = report.v1();
		API apiV2 = report.v2();
		List<BreakingChange> changes = report.getBreakingChanges();

		List<TypeDecl> impactedTypes = report.impactedTypes();

		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		DateTimeFormatter human = DateTimeFormatter.ofPattern("MMMM d, uuuu 'at' h:mm a", Locale.ENGLISH);
		String generatedAt = now.format(human);

		sb.append("<!DOCTYPE html>\n");
		sb.append("<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n");
		sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		sb.append("<title>Roseau Breaking Changes Report</title>\n");
		sb.append("<style>\n");
		sb.append(baseCss());
		sb.append("</style>\n");
		// Theme initialization script
		sb.append("<script>!function(){const e=matchMedia(\"(prefers-color-scheme: light)\"),t=s=>document.documentElement.setAttribute(\"data-theme\",s.matches?\"light\":\"dark\");t(e);e.addEventListener(\"change\",t)}();</script>\n\n");
		sb.append("</head>\n<body>\n");

		// Header
		sb.append("<header class=\"header\">\n");
		sb.append("<div class=\"page\">");
		sb.append("<div class=\"titles\"><h1>Roseau Report</h1>\n");
		sb.append("<div class=\"subtitle\">Generated ").append(escape(generatedAt)).append("</div></div>\n");
		sb.append("<button id=\"theme-toggle\" class=\"theme-toggle\" title=\"Toggle theme\">🌙</button>");
		sb.append("</div>");
		sb.append("</header>\n");

		// Main content wrapper
		sb.append("<main class=\"page\">\n");
		// Summary cards
		sb.append("<section class=\"summary\">\n");
		// API info
		sb.append("<div class=\"card\">\n<h2>Compared APIs</h2>\n<div class=\"libgrid\">\n");
		sb.append(apiCard("Baseline: " + apiV1.getLibraryTypes().getLibrary().getLocation(), apiV1));
		sb.append(apiCard("New: " + apiV2.getLibraryTypes().getLibrary().getLocation(), apiV2));
		sb.append("</div>\n</div>\n");
		// Metrics
		sb.append("<div class=\"card metrics\">\n<h2>Summary</h2>\n<div class=\"metrics-grid\">\n");
		sb.append(metric("Breaking changes", Integer.toString(changes.size()), changes.isEmpty() ? "ok" : "danger"));
		sb.append(metric("Impacted types", Integer.toString(impactedTypes.size()), impactedTypes.isEmpty() ? "ok" : "warn"));
		sb.append(metric("Source-compatible", report.isSourceBreaking() ? "No" : "Yes", report.isSourceBreaking() ? "danger" : "ok"));
		sb.append(metric("Binary-compatible", report.isBinaryBreaking() ? "No" : "Yes", report.isBinaryBreaking() ? "danger" : "ok"));
		sb.append("</div>\n</div>\n");
		sb.append("</section>\n");

		// Contents
		sb.append("<section class=\"toc card\">\n<h2>Contents</h2>\n");
		if (changes.isEmpty()) {
			sb.append("<ul class=\"toc-list\"><li><em>No breaking changes detected.</em></li></ul>\n");
		} else {
			// Build package grouping
			Map<String, List<TypeDecl>> byPkg = new TreeMap<>();
			for (TypeDecl type : impactedTypes) {
				byPkg.computeIfAbsent(type.getPackageName(), k -> new ArrayList<>()).add(type);
			}
			byPkg.forEach((pkg, types) -> {
				sb.append("<h3 class=\"pkg-name\">").append(escape(pkg)).append("</h3>\n<ul class=\"toc-list\">\n");
				types.forEach(type -> {
					int totalBCs = report.breakingChangesOnTypeAndMembers(type).size();
					sb.append("<li>")
						.append("<a href=\"").append("#").append(anchor(type)).append("\">")
						.append(escape(type.getSimpleName())).append("</a> ")
						.append("<span class=\"pill\">").append(totalBCs).append("</span>");
					if (report.isSourceBreakingType(type))
						sb.append(" <span class=\"compat compat-source\" title=\"Source-incompatible\">Src</span>");
					if (report.isBinaryBreakingType(type))
						sb.append(" <span class=\"compat compat-binary\" title=\"Binary-incompatible\">Bin</span>");
					sb.append("</li>\n");
				});
				sb.append("</ul>\n");
			});
		}
		sb.append("</section>\n");

		// Detailed changes
		sb.append("<section class=\"details\">\n");
		for (TypeDecl type : impactedTypes) {
			String typeName = type.getQualifiedName();
			sb.append("<article class=\"card type\" id=\"").append(anchor(type)).append("\">\n");
			int totalBCs = report.breakingChangesOnTypeAndMembers(type).size();
			sb.append("<h2>").append(escape(typeName)).append(" <span class=\"badge\">").append(totalBCs).append("</span>");
			String typeLoc = locationBadge(report.typeLocation(typeName));
			if (!typeLoc.isEmpty()) sb.append(" ").append(typeLoc);
			sb.append("</h2>\n");
			List<BreakingChange> typeLevel = report.breakingChangesOnType(type);
			boolean typeRemoved = typeLevel.stream().anyMatch(bc -> bc.kind() == BreakingChangeKind.TYPE_REMOVED);
			if (typeRemoved) {
				sb.append("<div class=\"danger-banner\">This type was removed in the new version.</div>\n");
			} else {
				if (!typeLevel.isEmpty()) {
					sb.append("<div class=\"type-level\">\n<h3>Type-level changes</h3>\n<ul class=\"changes\">\n");
					for (BreakingChange bc : typeLevel) {
						sb.append("<li>")
							.append("<span class=\"kind kind-").append(cssKind(bc)).append("\">")
							.append(escape(bc.kind().toString())).append("</span>");
						String newSym = printSymbol(bc.newSymbol());
						if (!newSym.isEmpty()) {
							sb.append(" <span class=\"arrow\">→</span> <code>").append(escape(newSym)).append("</code>");
						}
						String det = detailHtml(bc);
						if (!det.isEmpty()) {
							sb.append(" <span class=\"detail\">— ").append(det).append("</span>");
						}
						sb.append("</li>\n");
					}
					sb.append("</ul>\n</div>\n");
				}
				Map<String, List<BreakingChange>> members = report.memberChanges(typeName);
				for (Map.Entry<String, List<BreakingChange>> me : members.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
					String memberName = me.getKey();
					List<BreakingChange> mchanges = me.getValue();
					Symbol ms = mchanges.get(0).impactedSymbol();
					String memberKind = (ms instanceof TypeMemberDecl tmd) ? tmd.getClass().getSimpleName() : "Member";
					SourceLocation mloc = (ms instanceof TypeMemberDecl tmd) ? tmd.getLocation() : null;
					sb.append("<div class=\"member\">\n");
					sb.append("<h3>").append(escape(memberName)).append(" <small class=\"muted\">")
						.append(escape(memberKind)).append("</small>");
					String memLoc = locationBadge(mloc);
					if (!memLoc.isEmpty()) sb.append(" ").append(memLoc);
					sb.append("</h3>\n");
					sb.append("<ul class=\"changes\">\n");
					for (BreakingChange bc : mchanges) {
						sb.append("<li>")
							.append("<span class=\"kind kind-")
							.append(cssKind(bc))
							.append("\">")
							.append(escape(bc.kind().toString()))
							.append("</span>")
							.append(" ")
							.append(compatBadges(bc));
						String newSym = printSymbol(bc.newSymbol());
						if (!newSym.isEmpty()) {
							sb.append(" <span class=\"arrow\">→</span> <code>")
								.append(escape(newSym))
								.append("</code>");
							String newLoc = symbolLocationBadge(bc.newSymbol());
							if (!newLoc.isEmpty()) sb.append(" ").append(newLoc);
						}
						String det = detailHtml(bc);
						if (!det.isEmpty()) {
							sb.append(" <span class=\"detail\">— ").append(det).append("</span>");
						}
						sb.append("</li>\n");
					}
					sb.append("</ul>\n</div>\n");
				}
			}
			sb.append("</article>\n");
		}
		sb.append("</section>\n");

		// Close main and footer
		sb.append("</main>\n");
		sb.append("<footer class=\"footer\">Made with <span class=\"heart\">❤</span> by Roseau</footer>\n");
		// Theme toggle script (after DOM is ready)
		sb.append("<script>(function(){function setIcon(){try{var t=document.documentElement.getAttribute('data-theme')||'light';var b=document.getElementById('theme-toggle');if(b){b.textContent=t==='dark'?'🌙':'☀️';}}catch(e){}};setIcon();var btn=document.getElementById('theme-toggle');if(btn){btn.addEventListener('click',function(){try{var cur=document.documentElement.getAttribute('data-theme')||'dark';var next=cur==='dark'?'light':'dark';document.documentElement.setAttribute('data-theme',next);localStorage.setItem('roseau_theme',next);setIcon();}catch(e){}});}})();</script>\n");
		sb.append("</body>\n</html>\n");
		return sb.toString();
	}


	private static String printSymbol(Symbol s) {
		if (s == null) return "";
		if (s instanceof ExecutableDecl e) {
			return e.getContainingType().getQualifiedName() + "." + e.getSignature();
		}
		return s.getQualifiedName();
	}

	private static String metric(String label, String value, String tone) {
		return "<div class=\"metric \" data-tone=\"" + tone + "\">" +
			"<div class=\"metric-value\">" + escape(value) + "</div>" +
			"<div class=\"metric-label\">" + escape(label) + "</div></div>";
	}

	private static String apiCard(String label, API api) {
		int exported = api.getExportedTypes().size();
		int all = api.getLibraryTypes().getAllTypes().size();
		int methodCount = api.getExportedTypes().stream().mapToInt(t -> t.getDeclaredMethods().size()).sum();
		int fieldCount = api.getExportedTypes().stream().mapToInt(t -> t.getDeclaredFields().size()).sum();
		return new StringBuilder()
			.append("<div class=\"lib\">")
			.append("<div class=\"lib-label\">").append(escape(label)).append("</div>")
			.append("<div class=\"lib-meta\"><span><span class=\"muted\">Exported types:</span> ")
			.append(exported).append(" / ").append(all).append("</span>")
			.append("<span><span class=\"muted\">Methods:</span> ").append(methodCount).append("</span>")
			.append("<span><span class=\"muted\">Fields:</span> ").append(fieldCount).append("</span></div>")
			.append("</div>")
			.toString();
	}

	private static String anchor(TypeDecl type) {
		return type.getQualifiedName()
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-|-$)", "");
	}

	private static String cssKind(BreakingChange bc) {
		return bc.kind().getNature().name().toLowerCase(Locale.ROOT);
	}

	private static String escape(String s) {
		return s == null ? "" : s
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

	private static String baseCss() {
		return ":root{--bg:#0b1020;--text:#e6e9f5;--muted:#9aa4d6;--header-start:#111736;--header-end:#0b1020;--header-title:#ffffff;--subtitle:#9aa4d6;--card-bg:#0f1531;--card-border:#1b2452;--card-shadow:#1a2147;--chip-bg:#0c1127;--chip-border:#1a2147;--link:#d7dbf2;--pill-bg:#202a61;--pill-text:#b8c1ff;--metric-bg:#0c1127;--metric-border:#1a2147;--danger-bg:#3a0f19;--danger-border:#5a1a28;--danger-text:#ffb3c0;--member-border:#1b2452;--change-bg:#0c1127;--change-border:#1b2452;--detail:#d0d4f7;--del-bg:#3a0f19;--del-text:#ffb3c0;--del-border:#5a1a28;--mut-bg:#2a1e05;--mut-text:#ffd88a;--mut-border:#4b370c;--add-bg:#0f2a1a;--add-text:#a6f2c3;--add-border:#1b5a39;--compat-src-bg:#20315b;--compat-src-text:#bcd0ff;--compat-src-border:#2a3d77;--compat-bin-bg:#19422e;--compat-bin-text:#b6f5d5;--compat-bin-border:#1f5b3c;--loc-bg:#202a61;--loc-text:#b8c1ff;--loc-border:#2a3d77;--arrow:#9aa4d6;--code-bg:#0c1127;--code-border:#1a2147;--footer:#9aa4d6}"
			+ "[data-theme=light]{--bg:#ffffff;--text:#0f1230;--muted:#556083;--header-start:#f5f7ff;--header-end:#ffffff;--header-title:#111111;--subtitle:#556083;--card-bg:#ffffff;--card-border:#dfe5fb;--card-shadow:#e6e9f5;--chip-bg:#f6f8ff;--chip-border:#dfe5fb;--link:#0f1230;--pill-bg:#e9edff;--pill-text:#1f3bb3;--metric-bg:#f6f8ff;--metric-border:#dfe5fb;--danger-bg:#ffe8ec;--danger-border:#ffc2cd;--danger-text:#8a1f2b;--member-border:#e3e8ff;--change-bg:#f6f8ff;--change-border:#dfe5fb;--detail:#333a5f;--del-bg:#ffe8ec;--del-text:#8a1f2b;--del-border:#ffc2cd;--mut-bg:#fff5db;--mut-text:#7a5a0a;--mut-border:#ffe2a6;--add-bg:#e8fff3;--add-text:#116e3b;--add-border:#bdeed3;--compat-src-bg:#e9edff;--compat-src-text:#1f3bb3;--compat-src-border:#d2dbff;--compat-bin-bg:#e8fff3;--compat-bin-text:#116e3b;--compat-bin-border:#bdeed3;--loc-bg:#e9edff;--loc-text:#1f3bb3;--loc-border:#d2dbff;--arrow:#556083;--code-bg:#f6f8ff;--code-border:#e6e9f5;--footer:#556083}"
			+ "body{font-family:Inter,system-ui,Segoe UI,Roboto,Helvetica,Arial,sans-serif;line-height:1.5;margin:0;background:var(--bg);color:var(--text)}"
			+ ".page{max-width:1100px;margin:0 auto;padding:0 16px}"
			+ ".header{padding:16px 0;background:linear-gradient(180deg,var(--header-start),var(--header-end));}"
			+ ".header .page{display:flex;gap:12px;align-items:center;justify-content:space-between}"
			+ ".header h1{margin:0;font-size:28px;color:var(--header-title)} .header .subtitle{color:var(--subtitle);margin-top:6px}"
			+ ".theme-toggle{border:1px solid var(--card-border);background:var(--chip-bg);color:var(--text);border-radius:999px;padding:6px 10px;cursor:pointer;font-size:14px}"
			+ ".summary{display:grid;gap:16px;padding:16px} .card{background:var(--card-bg);border:1px solid var(--card-border);border-radius:12px;padding:16px;box-shadow:0 1px 0 var(--card-shadow)}"
			+ ".libgrid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:12px}"
			+ ".lib-label{font-weight:600;color:var(--muted);margin-bottom:6px} .lib-meta{color:var(--text);opacity:.9;margin-top:6px} .lib-meta span{display:inline-block;margin-right:10px}"
			+ ".metrics-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px}"
			+ ".metric{background:var(--metric-bg);border:1px solid var(--metric-border);border-radius:10px;padding:12px;text-align:center}"
			+ ".metric-value{font-size:24px;font-weight:700} .metric-label{font-size:12px;color:var(--muted)}"
			+ ".toc{margin:0 16px 8px} .toc-list{list-style:none;padding-left:0;margin:0;display:flex;flex-wrap:wrap;gap:8px}"
			+ ".toc-list li{background:var(--chip-bg);border:1px solid var(--chip-border);border-radius:999px;padding:6px 10px} .toc-list a{color:var(--link);text-decoration:none}"
			+ ".pill{background:var(--pill-bg);color:var(--pill-text);border-radius:999px;padding:2px 8px;margin-left:6px;font-size:12px}"
			+ ".pkg-name{margin:8px 0 6px;font-size:16px;color:var(--muted)}"
			+ ".details{display:grid;gap:16px;padding:16px} .type h2{margin:0 0 6px} .badge{background:var(--pill-bg);color:var(--pill-text);border-radius:999px;padding:2px 8px;font-size:12px}"
			+ ".type-level{margin-top:6px} .danger-banner{background:var(--danger-bg);border:1px solid var(--danger-border);color:var(--danger-text);padding:8px 10px;border-radius:8px;margin:8px 0}"
			+ ".member{margin-top:8px;padding-top:8px;border-top:1px solid var(--member-border)} .member h3{margin:0 0 6px;font-size:15px} .muted{color:var(--muted)}"
			+ ".changes{list-style:none;margin:0;padding-left:0} .changes li{margin:8px 0;padding:8px 10px;border-left:3px solid var(--change-border);background:var(--change-bg);border-radius:8px} .kind{font-weight:600;border-radius:6px;padding:2px 6px} .detail{color:var(--detail)}"
			+ ".kind-deletion{background:var(--del-bg);color:var(--del-text);border:1px solid var(--del-border)} .kind-mutation{background:var(--mut-bg);color:var(--mut-text);border:1px solid var(--mut-border)} .kind-addition{background:var(--add-bg);color:var(--add-text);border:1px solid var(--add-border)}"
			+ ".compat{font-size:11px;border-radius:999px;padding:2px 6px;border:1px solid transparent} .compat-source{background:var(--compat-src-bg);color:var(--compat-src-text);border-color:var(--compat-src-border)} .compat-binary{background:var(--compat-bin-bg);color:var(--compat-bin-text);border-color:var(--compat-bin-border)}"
			+ ".loc{font-size:11px;color:var(--loc-text);background:var(--loc-bg);border:1px solid var(--loc-border);border-radius:999px;padding:2px 6px;vertical-align:middle}"
			+ ".arrow{color:var(--arrow);margin:0 6px} code{background:var(--code-bg);border:1px solid var(--code-border);border-radius:6px;padding:2px 4px}"
			+ ".footer{color:var(--footer);text-align:center;padding:24px} .heart{color:#ff6b9a}";
	}

	private static String detailHtml(BreakingChange bc) {
		BreakingChangeDetails d = bc.details();
		if (d == null || d instanceof BreakingChangeDetails.None) return "";
		return switch (d) {
			case BreakingChangeDetails.MethodReturnTypeChanged(var prev, var now) ->
				"return type: <code>" + escape(String.valueOf(prev)) + "</code> → <code>" + escape(String.valueOf(now)) + "</code>";
			case BreakingChangeDetails.FieldTypeChanged(var prev, var now) ->
				"field type: <code>" + escape(String.valueOf(prev)) + "</code> → <code>" + escape(String.valueOf(now)) + "</code>";
			case BreakingChangeDetails.MethodAddedToInterface(var newMethod) ->
				"added abstract method: <code>" + escape(newMethod.getSignature()) + "</code>";
			case BreakingChangeDetails.MethodAbstractAddedToClass(var newMethod) ->
				"added abstract method: <code>" + escape(newMethod.getSignature()) + "</code>";
			case BreakingChangeDetails.ClassTypeChanged(var oldType, var newType) ->
				"class kind: <code>" + escape(oldType.getSimpleName()) + "</code> → <code>" + escape(newType.getSimpleName()) + "</code>";
			case BreakingChangeDetails.SuperTypeRemoved(var superType) ->
				"removed supertype: <code>" + escape(String.valueOf(superType)) + "</code>";
			case BreakingChangeDetails.AnnotationTargetRemoved(var target) ->
				"annotation target removed: <code>" + escape(String.valueOf(target)) + "</code>";
			case BreakingChangeDetails.AnnotationMethodAddedWithoutDefault(var newMethod) ->
				"new annotation method without default value: <code>" + escape(newMethod.getSignature()) + "</code>";
			case BreakingChangeDetails.MethodNoLongerThrowsCheckedException(var exception) ->
				"no longer throws: <code>" + escape(String.valueOf(exception)) + "</code>";
			case BreakingChangeDetails.MethodNowThrowsCheckedException(var exception) ->
				"now throws: <code>" + escape(String.valueOf(exception)) + "</code>";
			case BreakingChangeDetails.MethodParameterGenericsChanged(var oldType, var newType) ->
				"parameter generic type: <code>" + escape(String.valueOf(oldType)) + "</code> → <code>" + escape(String.valueOf(newType)) + "</code>";
			case BreakingChangeDetails.TypeFormalTypeParametersRemoved(var ftp) ->
				"type parameter removed: <code>" + escape(String.valueOf(ftp)) + "</code>";
			case BreakingChangeDetails.TypeFormalTypeParametersAdded(var ftp) ->
				"type parameter added: <code>" + escape(String.valueOf(ftp)) + "</code>";
			case BreakingChangeDetails.TypeFormalTypeParametersChanged(var oldFtp, var newFtp) ->
				"type parameter changed: <code>" + escape(String.valueOf(oldFtp)) + "</code> → <code>" + escape(String.valueOf(newFtp)) + "</code>";
			case BreakingChangeDetails.MethodFormalTypeParametersRemoved(var ftp) ->
				"method type parameter removed: <code>" + escape(String.valueOf(ftp)) + "</code>";
			case BreakingChangeDetails.MethodFormalTypeParametersAdded(var ftp) ->
				"method type parameter added: <code>" + escape(String.valueOf(ftp)) + "</code>";
			case BreakingChangeDetails.MethodFormalTypeParametersChanged(var oldFtp, var newFtp) ->
				"method type parameter changed: <code>" + escape(String.valueOf(oldFtp)) + "</code> → <code>" + escape(String.valueOf(newFtp)) + "</code>";
			case BreakingChangeDetails.None none -> "";
		};
	}

	private static String compatBadges(BreakingChange bc) {
		boolean src = bc.kind().isSourceBreaking();
		boolean bin = bc.kind().isBinaryBreaking();
		StringBuilder b = new StringBuilder();
		if (src) b.append("<span class=\"compat compat-source\">Source</span>");
		if (bin) b.append(src ? " " : "").append("<span class=\"compat compat-binary\">Binary</span>");
		return b.toString();
	}

	private static String locationBadge(SourceLocation loc) {
		if (loc == null) return "";
		try {
			if (loc.line() < 0 || loc.file() == null) return "";
			String fileName = String.valueOf(loc.file().getFileName());
			String title = escape(loc.toString());
			return "<span class=\"loc\" title=\"" + title + "\">📍 " + escape(fileName) + ":" + loc.line() + "</span>";
		} catch (Exception e) {
			return "";
		}
	}

	private static String symbolLocationBadge(Symbol s) {
		if (s == null) return "";
		return locationBadge(s.getLocation());
	}
}
