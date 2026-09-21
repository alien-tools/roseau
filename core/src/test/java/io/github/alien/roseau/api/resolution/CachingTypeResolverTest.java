package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachingTypeResolverTest {
	TypeProvider provider1;
	TypeProvider provider2;
	CachingTypeResolver resolver;

	@BeforeEach
	void setUp() {
		provider1 = mock(TypeProvider.class);
		provider2 = mock(TypeProvider.class);
		resolver = new CachingTypeResolver(List.of(provider1, provider2));
	}

	@Test
	void resolve_in_first_provider() {
		var reference = new TypeReference<>("pkg.Type");
		var type1 = mock(ClassDecl.class);
		var type2 = mock(ClassDecl.class);

		when(provider1.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.of(type1));
		when(provider2.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.of(type2));

		var result = resolver.resolve(reference);

		assertThat(result).hasValue(type1);
		verify(provider1, times(1)).findType("pkg.Type", TypeDecl.class);
		verify(provider2, never()).findType(any(), any());
	}

	@Test
	void resolve_in_second_provider() {
		var reference = new TypeReference<>("pkg.Type");
		var type = mock(ClassDecl.class);

		when(provider1.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.empty());
		when(provider2.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.of(type));

		var result = resolver.resolve(reference);

		assertThat(result).hasValue(type);
		verify(provider1, times(1)).findType("pkg.Type", TypeDecl.class);
		verify(provider2, times(1)).findType("pkg.Type", TypeDecl.class);
	}

	@Test
	void resolve_caches_results() {
		var reference = new TypeReference<>("pkg.Type");
		var type = mock(ClassDecl.class);

		when(provider1.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.of(type));
		when(provider2.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.empty());

		var first = resolver.resolve(reference);
		var second = resolver.resolve(reference);

		assertThat(first).hasValue(type);
		assertThat(second).hasValue(type);
		verify(provider1, times(1)).findType("pkg.Type", TypeDecl.class);
		verify(provider2, never()).findType(any(), any());
	}

	@Test
	void resolve_type_not_found() {
		var reference = new TypeReference<>("pkg.UnknownType");

		when(provider1.findType("pkg.UnknownType")).thenReturn(Optional.empty());
		when(provider2.findType("pkg.UnknownType")).thenReturn(Optional.empty());

		var result = resolver.resolve(reference);

		assertThat(result).isEmpty();
		verify(provider1, times(1)).findType("pkg.UnknownType", TypeDecl.class);
		verify(provider2, times(1)).findType("pkg.UnknownType", TypeDecl.class);
	}

	@Test
	void unresolved_types_are_recorded() {
		var unknown = new TypeReference<>("pkg.UnknownType");
		var other = new TypeReference<>("pkg.AnotherUnknownType");
		var known = new TypeReference<>("pkg.Type");
		var type = mock(ClassDecl.class);

		when(provider1.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.of(type));

		resolver.resolve(unknown);
		resolver.resolve(other);
		resolver.resolve(unknown);
		resolver.resolve(known);

		assertThat(resolver.getUnresolvedTypes())
			.containsExactly("pkg.AnotherUnknownType", "pkg.UnknownType");
	}

	@Test
	void resolve_unexpected_type_kind() {
		var reference = new TypeReference<ClassDecl>("pkg.Class");
		var illReference = new TypeReference<InterfaceDecl>("pkg.Class");
		var type = mock(ClassDecl.class);

		when(provider1.findType("pkg.Class", TypeDecl.class)).thenReturn(Optional.of(type));

		var result = resolver.resolve(reference, ClassDecl.class);
		var illResult = resolver.resolve(illReference, InterfaceDecl.class);

		assertThat(result).hasValue(type);
		assertThat(illResult).isEmpty();
		verify(provider1, times(1)).findType("pkg.Class", TypeDecl.class);
		verify(provider2, never()).findType(any(), any());
	}

	@Test
	void wrong_kind_lookup_does_not_poison_later() {
		var classReference = new TypeReference<ClassDecl>("pkg.Type");
		var interfaceReference = new TypeReference<InterfaceDecl>("pkg.Type");
		var type = mock(InterfaceDecl.class);

		when(provider1.findType("pkg.Type", TypeDecl.class)).thenReturn(Optional.of(type));

		var wrongKind = resolver.resolve(classReference, ClassDecl.class);
		var rightKind = resolver.resolve(interfaceReference, InterfaceDecl.class);

		assertSoftly(softly -> {
			softly.assertThat(wrongKind).isEmpty();
			softly.assertThat(rightKind).containsSame(type);
			softly.assertThat(resolver.getUnresolvedTypes()).isEmpty();
		});
	}
}
