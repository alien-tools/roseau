package testing_lib.exceptionClazzMethodFinallyBlockDelete;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ExceptionClazzMethodFinallyBlockDelete {

	public void method1(File file) {
		FileReader fr = null;
		try {
			fr = new FileReader(file);
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

}
