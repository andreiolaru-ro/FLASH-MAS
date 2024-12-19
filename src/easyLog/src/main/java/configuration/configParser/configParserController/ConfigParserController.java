package easyLog.src.main.java.configuration.configParser.configParserController;

import  easyLog.src.main.java.configuration.configParser.entities.Entry;
import easyLog.src.main.java.parser.engine.ParserEngine;
import easyLog.src.main.java.parser.logsLoader.LogsLoader;
import net.xqhs.flash.sclaim.parser.Parser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigParserController {

    private List<Entry> entriesList = new ArrayList<>(); // list of entries in the configuration file that needs to be processed
    private Set<ParserEngine> engineSet;
    private boolean matched = false;

    public List<Entry> getEntriesList() {
        return entriesList;
    }

    public void setEntitiesList(List<Entry> entitiesList) {
        this.entriesList = entitiesList;
    }

    public void activateParserEngine(InputStream in) throws FileNotFoundException, InterruptedException { //method that activates the parser engine for the configuration objects
//        LogsLoader logsLoader = new LogsLoader(in);
//        LogsLoader logsLoader2 = new LogsLoader(in);
//        LogsLoader logsLoader3 = new LogsLoader(in);
//        LogsLoader logsLoader4 = new LogsLoader(in);
//        LogsLoader logsLoader5 = new LogsLoader(in);
//        ExecutorService  executor = Executors.newCachedThreadPool();
        initializeParserEngineSet();
//        for (Entry entry : entriesList) // entity represents the configuration object
//        {
//            logsLoader.initializeParser(new ParserEngine(entry));
//            if (entry.getOutputItem() != null) {
//                entry.getOutputItem().getOutput();
//            }
//            System.out.println();
//            System.out.println("-----------------------------");
//        }

//        List<ParserEngine> engineList = new ArrayList<>(getEngineSet());
//        logsLoader.initializeParser(engineList.get(0));
//        logsLoader2.initializeParser(engineList.get(1));
//        logsLoader3.initializeParser(engineList.get(2));
//        logsLoader4.initializeParser(engineList.get(3));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            int n = 0;
            while ((line = reader.readLine()) != null) { // aici trebuie sa nu luam in seama primele linii
                if( line.matches("^\\.\\s\\[ boot\\s+\\]\\sConfiguration loaded$")) //( . [  > [  # [ ) match pe primele 3 caractere dintr-un log obisnuit
                {
                    this.matched = true;
                }
                if(this.matched)
                {
//                    this.getEngineSet().forEach(engine -> {
//                        try{
//                            engine.process(line);
//                        } catch (FileNotFoundException e) {
//                            throw new RuntimeException(e);
//                        }
//                    });
                    for(ParserEngine engine : getEngineSet())
                    {
                        engine.process(line);
                    }
                    System.out.println(n++ + " Lines processed");
                    if(n%10 == 0 ){
                        for(Entry entry: entriesList)
                        {
                            if (entry.getOutputItem() != null) {
                                entry.getOutputItem().getOutput();
                            }
                            System.out.println();
                            System.out.println("-----------------------------");
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }





//        for(Entry entry: entriesList)
//        {
//            if (entry.getOutputItem() != null) {
//                entry.getOutputItem().getOutput();
//           }
//            System.out.println();
//            System.out.println("-----------------------------");
//        }
    }

    private void initializeParserEngineSet(){
        this.engineSet = new HashSet<>();
        for(Entry entry : entriesList)
        {
            this.engineSet.add(new ParserEngine(entry));
        }
    }

    public Set<ParserEngine> getEngineSet() {
        return engineSet;
    }
}
