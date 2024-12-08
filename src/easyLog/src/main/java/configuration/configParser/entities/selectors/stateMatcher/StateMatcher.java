package easyLog.src.main.java.configuration.configParser.entities.selectors.stateMatcher;

import java.util.List;

public class StateMatcher {
    private List<String> keywords;

    public StateMatcher(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}
