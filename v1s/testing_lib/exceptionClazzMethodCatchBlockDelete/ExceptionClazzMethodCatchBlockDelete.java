package testing_lib.exceptionClazzMethodCatchBlockDelete;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ExceptionClazzMethodCatchBlockDelete {

	public void method1(File file) {
		FileReader fr;
			try {
				fr = new FileReader(file);
				fr.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
}
