package easyLog.src.main.java.configuration.configParser.configParserController;

import  easyLog.src.main.java.configuration.configParser.entities.Entry;
import easyLog.src.main.java.parser.engine.ParserEngine;
import easyLog.src.main.java.parser.logsLoader.LogsLoader;
import net.xqhs.flash.sclaim.parser.Parser;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigParserController {

    private List<Entry> entriesList = new ArrayList<>(); // list of entries in the configuration file that needs to be processed
    private Set<ParserEngine> engineSet;

    public List<Entry> getEntriesList() {
        return entriesList;
    }

    public void setEntitiesList(List<Entry> entitiesList) {
        this.entriesList = entitiesList;
    }

    public void activateParserEngine(InputStream in) throws FileNotFoundException, InterruptedException { //method that activates the parser engine for the configuration objects
        LogsLoader logsLoader = new LogsLoader(in);
        ExecutorService  executor = Executors.newCachedThreadPool();
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
        for(ParserEngine engine: getEngineSet())
        {
            executor.submit(() -> {
               try{
                   logsLoader.initializeParser(engine);
               } catch (FileNotFoundException e) {
                   throw new RuntimeException(e);
               }
            });
        }
        executor.shutdown();
        for(Entry entry: entriesList)
        {
            if (entry.getOutputItem() != null) {
                entry.getOutputItem().getOutput();
           }
            System.out.println();
            System.out.println("-----------------------------");
        }


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
