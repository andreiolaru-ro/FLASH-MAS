package easyLog.src.main.java.configuration.configParser.entities.selectors.expect;

import com.sun.org.glassfish.external.statistics.impl.CountStatisticImpl;
import easyLog.src.main.java.configuration.configParser.entities.selectors.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static easyLog.src.main.java.configuration.configParser.entities.selectors.expect.Expect.ExpectType.COUNT;
import static easyLog.src.main.java.configuration.configParser.entities.selectors.expect.ExpectCount.CountCriterium.*;
import static easyLog.src.main.java.configuration.configParser.entities.selectors.expect.ExpectCount.CountCriterium.*;

public class ExpectCount implements Expect {
    enum CountCriterium {
        MAX,
        MIN,
        EXACTLY,
    }

    CountCriterium criterium = EXACTLY;
    private int countNumber;

    private Map<String, Integer> numberOfAppearances = new HashMap<>();

    private String key;

    private List<String> whatToExpect;

    public ExpectCount(List<String> whatToExpect) {
        for (String item : whatToExpect) {
            if (item.equals(MAX.toString().toLowerCase())) {
                criterium = MAX;
            } else {
                if (item.equals(MIN.toString().toLowerCase())) {
                    criterium = MIN;
                } else {
                    if (item.equals(EXACTLY.toString().toLowerCase())) {
                        criterium = EXACTLY;
                    } else {
                        this.countNumber = Integer.parseInt(item);
                    }
                }
            }
        }
    }

    @Override
    public void build() {
        // TODO Auto-generated method stub

    }

    @Override
    public void addMatch(String logLine, String entity, String match, Level level) {
        if (numberOfAppearances.containsKey(match)) {
            numberOfAppearances.put(match, numberOfAppearances.get(match) + 1);
        } else {
            numberOfAppearances.put(match, 1);
            key = match;
        }
    }

    @Override
    public boolean isSatisfied() {
        int n = key == null || !numberOfAppearances.containsKey(key) ? 0 : numberOfAppearances.get(key);
        switch (criterium) {
            case MIN:
                return n > countNumber;
            case EXACTLY:
                return n == countNumber;
            case MAX:
                return n < countNumber;

        }
        return n == countNumber;
    }

    @Override
    public String getSatisfactionIndication() {
        return numberOfAppearances.get(key).toString();
    }

    public int getCountNumber() {
        return countNumber;
    }

    public void setCountNumber(int countNumber) {
        this.countNumber = countNumber;
    }
}
