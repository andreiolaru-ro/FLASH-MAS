package easyLog.src.main.java.parser.logsLoader;

import easyLog.src.main.java.parser.engine.LineProcessor;
import jdk.internal.util.xml.impl.Input;

import java.io.*;

public class LogsLoader {
    private InputStream  in;
    private boolean matched = false;

    public LogsLoader(InputStream in) {
        this.in = in;
    }

    public void initializeParser(LineProcessor lineProcessor) throws FileNotFoundException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
//            int n = 0;
            while ((line = reader.readLine()) != null) { // aici trebuie sa nu luam in seama primele linii
                if( line.matches("^\\.\\s\\[ boot\\s+\\]\\sConfiguration loaded$"))
                {
                    this.matched = true;
                }
                if(this.matched)
                {
                    lineProcessor.process(line);
//                    System.out.println(n++ + " Lines processed");
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
