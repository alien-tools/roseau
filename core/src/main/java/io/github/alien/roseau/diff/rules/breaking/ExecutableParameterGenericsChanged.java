package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.ApiAnalyzer;
import io.github.alien.roseau.api.analysis.TypeParameterMapping;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails.MethodParameterGenericsChanged;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Compares the parameter types of matched executables from the perspective of each of their clients:
 * <ul>
 *   <li>Callers: every argument accepted by an old parameter must be accepted by the new one, i.e., the old type
 *   must be a subtype of the new one.</li>
 *   <li>Overriders and hiders, when the executable can be overridden or hidden: the signature they were written
 *   against must still be a subsignature of the new one (JLS §8.4.2), i.e., the same signature or its erasure.</li>
 *   <li>Lambdas and method references implementing an interface method: their parameters and bodies were written
 *   against the old parameter types (JLS §15.27.3), which must not change.</li>
 * </ul>
 */
public class ExecutableParameterGenericsChanged implements MemberRule<ExecutableDecl> {
	@Override
	public void onMatched(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		// Clients that parameterize the type see the executable as it is declared, and those that use the type raw see
		// its erasure (§4.8). The existing clients of a type that becomes generic can only use it raw.
		findRawChange(oldExecutable, newExecutable, ctx)
			.or(() -> findChange(oldExecutable, newExecutable, ctx))
			.ifPresent(details -> ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
				ctx.oldType(), oldExecutable, newExecutable, details));
	}

	/**
	 * Finds the first parameter whose change breaks the clients that parameterize the type, if any.
	 */
	private static Optional<MethodParameterGenericsChanged> findChange(ExecutableDecl oldExecutable,
	                                                                   ExecutableDecl newExecutable,
	                                                                   MemberRuleContext ctx) {
		if (ctx.isErasedForExistingClients(newExecutable)) {
			return Optional.empty();
		}

		ApiAnalyzer v1 = ctx.v1().analyzer();
		ApiAnalyzer v2 = ctx.v2().analyzer();
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
			ctx.oldType(), ctx.newType(), oldExecutable, newExecutable);
		List<ITypeReference> oldTypes = parameterTypes(oldExecutable, newExecutable);
		List<ITypeReference> newTypes = parameterTypes(newExecutable, oldExecutable);
		// §8.4.2: overriders and hiders keep a subsignature if the old signature is the erasure of the new one
		boolean breaksOverriders = !v1.isEffectivelyFinal(ctx.oldType(), oldExecutable) &&
			!IntStream.range(0, oldTypes.size()).allMatch(i ->
				oldTypes.get(i).equals(v2.getErasedType(newExecutable, newTypes.get(i))));
		// §15.27.3: the parameters of a lambda get the parameter types of the method it implements
		boolean breaksLambdas = ctx.isLambdaImplementable(oldExecutable);

		for (int i = 0; i < oldTypes.size(); i++) {
			ITypeReference oldType = oldTypes.get(i);
			ITypeReference newType = newTypes.get(i);
			// Rename the type parameters of the old version; those added by the new one are not erased, since
			// generifying a type argument (List<Object> to List<T>) is a name clash for overriders
			ITypeReference normalizedOldType = normalizer.normalizeOld(oldType);
			if (normalizedOldType.equals(newType)) {
				continue;
			}

			boolean acceptsOldArguments = acceptsRawArguments(oldType, newType) ||
				v2.isSubtypeOf(newExecutable, normalizedOldType, normalizer.normalizeNew(newType));
			if (!acceptsOldArguments || breaksOverriders || breaksLambdas) {
				return Optional.of(new MethodParameterGenericsChanged(oldType, newType));
			}
		}
		return Optional.empty();
	}

	/**
	 * Finds the first parameter whose change breaks the clients that use the type raw, if any. They see the erasure
	 * of the executable's declaration, which accepts the arguments of its supertypes and is only overridden by
	 * itself.
	 */
	private static Optional<MethodParameterGenericsChanged> findRawChange(ExecutableDecl oldExecutable,
	                                                                      ExecutableDecl newExecutable,
	                                                                      MemberRuleContext ctx) {
		ApiAnalyzer v1 = ctx.v1().analyzer();
		ApiAnalyzer v2 = ctx.v2().analyzer();
		if (!v2.isErasedInRawType(ctx.newType(), newExecutable)) {
			return Optional.empty();
		}

		ExecutableDecl oldDeclaration = getDeclaration(ctx.oldType(), oldExecutable, v1);
		ExecutableDecl newDeclaration = getDeclaration(ctx.newType(), newExecutable, v2);
		boolean wasErased = v1.isErasedInRawType(ctx.oldType(), oldExecutable);
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
			ctx.oldType(), ctx.newType(), oldExecutable, newExecutable);
		List<ITypeReference> oldTypes = parameterTypes(wasErased ? oldDeclaration : oldExecutable, newExecutable).stream()
			.map(type -> wasErased ? v1.getErasedType(oldDeclaration, type) : type)
			.toList();
		List<ITypeReference> newTypes = parameterTypes(newDeclaration, oldExecutable).stream()
			.map(type -> v2.getErasedType(newDeclaration, type))
			.toList();
		boolean isOverridable = !v1.isEffectivelyFinal(ctx.oldType(), oldExecutable);
		// A generic method never is the erasure that the clients of a type that becomes generic see: its type
		// parameters are reported as removed
		boolean isGenericMethod = !wasErased && !oldExecutable.getFormalTypeParameters().isEmpty();
		return IntStream.range(0, oldTypes.size())
			.filter(i -> isOverridable
				? !oldTypes.get(i).equals(newTypes.get(i)) && !isGenericMethod
				: !v2.isSubtypeOf(newExecutable, normalizer.normalizeOld(oldTypes.get(i)), newTypes.get(i)))
			.mapToObj(i -> new MethodParameterGenericsChanged(oldTypes.get(i), newTypes.get(i)))
			.findFirst()
			// The erased signature that raw clients invoke and override may remain declared by a supertype
			.filter(_ -> !wasErased || !(oldDeclaration instanceof MethodDecl) ||
				v2.getLinkableMethodDeclarations(ctx.newType(), v1.getErasure(oldDeclaration)).isEmpty());
	}

	private static ExecutableDecl getDeclaration(TypeDecl type, ExecutableDecl executable, ApiAnalyzer analyzer) {
		return executable instanceof MethodDecl method ? analyzer.getDeclaration(type, method) : executable;
	}

	/**
	 * Returns the parameter types of an executable to compare with those of its counterpart. A variadic parameter's
	 * type is its component type: it is turned into an array type when its counterpart is not variadic too.
	 */
	private static List<ITypeReference> parameterTypes(ExecutableDecl executable, ExecutableDecl counterpart) {
		return IntStream.range(0, executable.getParameters().size())
			.mapToObj(i -> {
				ParameterDecl p = executable.getParameters().get(i);
				return p.isVarargs() && !counterpart.getParameters().get(i).isVarargs() ? arrayOf(p.type()) : p.type();
			})
			.toList();
	}

	private static ITypeReference arrayOf(ITypeReference type) {
		return type instanceof ArrayTypeReference(var component, var dimension)
			? new ArrayTypeReference(component, dimension + 1)
			: new ArrayTypeReference(type, 1);
	}

	/**
	 * §5.1.9: every argument accepted by a raw type is accepted by its unbounded wildcard parameterization, e.g.,
	 * {@code List} becoming {@code List<?>}, or {@code List[]} becoming {@code List<?>[]}. Only a raw type becoming
	 * parameterized qualifies: {@code Object} becoming {@code String}, both without type arguments, does not.
	 */
	private static boolean acceptsRawArguments(ITypeReference oldType, ITypeReference newType) {
		return switch (oldType) {
			case TypeReference<?> tr1 when newType instanceof TypeReference<?> tr2 ->
				tr1.typeArguments().isEmpty() && !tr2.typeArguments().isEmpty() &&
					tr1.qualifiedName().equals(tr2.qualifiedName()) && tr2.typeArguments().stream()
					.allMatch(arg -> arg instanceof WildcardTypeReference wildcard && wildcard.isUnbounded());
			case ArrayTypeReference a1 when newType instanceof ArrayTypeReference a2 && a1.dimension() == a2.dimension() ->
				acceptsRawArguments(a1.componentType(), a2.componentType());
			default -> false;
		};
	}
}
