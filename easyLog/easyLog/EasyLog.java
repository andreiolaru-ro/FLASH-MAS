package easyLog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class EasyLog {
	public static void start(PipedOutputStream source, String configFile) {
		final PipedInputStream in = new PipedInputStream();
		try {
			in.connect(source);
		} catch(IOException e2) {
			e2.printStackTrace();
		}
		
		Thread easyLog = new Thread(() -> {
			try {
				new EngineController(configFile).parseStream(in);
			} catch(FileNotFoundException e) {
				e.printStackTrace();
			} catch(InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
		easyLog.start();
	}
}
