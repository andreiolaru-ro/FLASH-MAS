package easyLog.configuration.entry.selector.expect;

import java.util.List;

import easyLog.configuration.entry.Entry.Recorder;
import easyLog.configuration.entry.selector.level.Level;


public interface Expect extends Recorder {

    enum ExpectType {
        COUNT,
        MATCH,
        // match cu equals(type.toString().toLowerCase())
    }


    public boolean isSatisfied();


    public String getSatisfactionIndication();
}
