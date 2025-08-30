package easyLog.integration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedOutputStream;

import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.output.StreamLogOutput;

public final class PipedStreamLogOutput implements StreamLogOutput {
	//easyLog Wrapper
	PipedOutputStream out = new PipedOutputStream();
	
	@Override
	public long getUpdatePeriod() {
		return 0;
	}
	
	@Override
	public int formatData() {
		return Logger.INCLUDE_NAME;
	}
	
	@Override
	public boolean useCustomFormat() {
		return false;
	}
	
	@Override
	public String format(Level level, String s, String s1) {
		return null;
	}
	
	@Override
	public void update() {
		// daca e nevoie sa fac flush Stream.flush
		try {
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
	
	@Override
	public OutputStream getOutputStream() {
		return out;
	}
	
	@Override
	public void exit() {
		// inchid streamul
		try {
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}