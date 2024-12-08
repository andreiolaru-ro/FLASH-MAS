package easyLog.src.main.java.configuration.configParser.entities.selectors.expect;

import java.util.List;

import  easyLog.src.main.java.configuration.configParser.entities.Entry.Recorder;
import  easyLog.src.main.java.configuration.configParser.entities.selectors.level.Level;


public interface Expect extends Recorder {

    enum ExpectType {
        COUNT,
        MATCH,
        // match cu equals(type.toString().toLowerCase())
    }


    public boolean isSatisfied();


    public String getSatisfactionIndication();
}
