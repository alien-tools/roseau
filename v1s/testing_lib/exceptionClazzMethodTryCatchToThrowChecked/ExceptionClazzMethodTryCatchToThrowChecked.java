package testing_lib.exceptionClazzMethodTryCatchToThrowChecked;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ExceptionClazzMethodTryCatchToThrowChecked {

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
