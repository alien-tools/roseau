package io.github.alien.roseau;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaReleaseTest {
	@Test
	void host_is_the_running_release() {
		assertThat(JavaRelease.host()).isEqualTo(Runtime.version().feature());
	}

	@ParameterizedTest
	@ValueSource(ints = {8, 11, 17, 21})
	void supports_releases_provided_by_ct_sym(int release) {
		assertThatCode(() -> JavaRelease.checkSupported(release)).doesNotThrowAnyException();
	}

	@Test
	void supports_the_host_release() {
		assertThatCode(() -> JavaRelease.checkSupported(JavaRelease.host())).doesNotThrowAnyException();
	}

	@Test
	void rejects_releases_older_than_8() {
		assertThatThrownBy(() -> JavaRelease.checkSupported(7))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Unsupported Java release 7");
	}

	@Test
	void rejects_releases_newer_than_the_host() {
		int next = JavaRelease.host() + 1;
		assertThatThrownBy(() -> JavaRelease.checkSupported(next))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Unsupported Java release " + next);
	}
}
