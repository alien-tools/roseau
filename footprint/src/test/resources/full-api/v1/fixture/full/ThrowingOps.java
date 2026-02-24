package fixture.full;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;

public interface ThrowingOps {
	void read() throws IOException;

	default void write() throws SQLException {
		// no-op
	}

	static void touch() throws EOFException {
		// no-op
	}
}
