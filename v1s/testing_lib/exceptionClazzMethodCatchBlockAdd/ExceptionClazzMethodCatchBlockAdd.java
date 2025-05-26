package testing_lib.exceptionClazzMethodCatchBlockAdd;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ExceptionClazzMethodCatchBlockAdd {

	public void method1(File file) {
		FileReader fr;
			try {
				fr = new FileReader(file);
				fr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
}
