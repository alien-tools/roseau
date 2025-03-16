package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.changes.BreakingChange;
import htmlflow.HtmlFlow;
import org.xmlet.htmlapifaster.Tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlFormatter implements BreakingChangesFormatter {
	@Override
	public String format(List<BreakingChange> changes) {
		StringBuilder sb = new StringBuilder();
		HtmlFlow.doc(sb)
			.html()
			.head()
			.meta().addAttr("charset", "utf-8").__()
			.meta()
			.addAttr("name", "viewport")
			.addAttr("content", "width=device-width, initial-scale=1.0")
			.__()
			.title().text("Roseau Breaking Changes Report").__()
			.link()
			.addAttr("href", "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css")
			.addAttr("rel", "stylesheet")
			.addAttr("integrity", "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH")
			.addAttr("crossorigin", "anonymous")
			.__()
			.__()
			.body()
			.div().addAttr("class", "container mt-5")
			.h1().text("Breaking Changes Report").__()
			.table().addAttr("class", "table")
			.of(table -> getImpactedApiTree(changes).forEach(node ->
				table.tr().of(tr -> appendNode(tr, node)).__()
					.of(theTable -> node.children.forEach(member -> theTable.tr().of(tr -> appendNode(tr, member)).__()))
			))
			.__()
			.__()
			.__();
		return sb.toString();
	}

	private static void appendNode(Tr<?> tr, Node node) {
		tr
			.td().span().addAttr("class", "badge bg-info")
			.text(node.type.substring(0, node.type.length() - 4)).__().__()
			.of(theTr -> {
				if (node instanceof TypeNode) {
					theTr.td().addAttr("colspan", "2").text(node.name);
				} else {
					theTr.td().__().td().text(node.name);
				}
			})
			.td().of(td -> node.breakingChanges.forEach(change ->
				td.span().addAttr("class", "badge bg-danger").text(change.kind().toString())))
			.__();
	}

	private static List<TypeNode> getImpactedApiTree(List<BreakingChange> changes) {
		Map<Symbol, Node> nodes = new HashMap<>();
		for (BreakingChange change : changes) {
			Symbol sym = change.impactedSymbol();
			switch (sym) {
				case TypeDecl td:
					nodes.computeIfAbsent(sym, k -> fromTypeDecl(td));
					nodes.get(sym).addBreakingChange(change);
					break;
				case TypeMemberDecl tmd:
					TypeDecl container = tmd.getContainingType().getResolvedApiType().get();
					nodes.computeIfAbsent(container, k -> fromTypeDecl(container));
					nodes.computeIfAbsent(sym, k -> fromTypeMemberDecl(tmd));
					if (!((TypeNode) nodes.get(container)).children.contains(nodes.get(sym)))
						((TypeNode) nodes.get(container)).addChild((MemberNode) nodes.get(sym));
					nodes.get(sym).addBreakingChange(change);
					break;
			}
		}
		return nodes.values().stream().filter(n -> n instanceof TypeNode).map(n -> (TypeNode) n).toList();
	}

	private static TypeNode fromTypeDecl(TypeDecl td) {
		return new TypeNode(td.getQualifiedName(), td.getClass().getSimpleName(), new ArrayList<>());
	}

	private static MemberNode fromTypeMemberDecl(TypeMemberDecl tmd) {
		return new MemberNode(tmd.getSimpleName(), tmd.getClass().getSimpleName());
	}

	private static class Node {
		final String name;
		final String type;
		final List<BreakingChange> breakingChanges;

		Node(String name, String type) {
			this.name = name;
			this.type = type;
			this.breakingChanges = new ArrayList<>();
		}

		void addBreakingChange(BreakingChange bc) {
			breakingChanges.add(bc);
		}
	}

	private static class TypeNode extends Node {
		final List<MemberNode> children;

		TypeNode(String name, String type, List<MemberNode> children) {
			super(name, type);
			this.children = new ArrayList<>();
		}

		void addChild(MemberNode child) {
			this.children.add(child);
		}
	}

	private static class MemberNode extends Node {
		MemberNode(String name, String type) {
			super(name, type);
		}
	}
}
