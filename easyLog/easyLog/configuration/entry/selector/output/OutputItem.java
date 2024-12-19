package easyLog.configuration.entry.selector.output;

import java.util.List;

public class OutputItem {


    List<OutputElement> elements;

    public OutputItem(List<OutputElement> elements) {
        this.elements = elements;
    }

    {
        // dacă elementul din YAML se potrivește cu vreun ExpectType, atunci e un element de tip ExpectOutput (ideal, instanța de ExpectType se ia chiar cea construită pentru această entitate)
        // dacă are două puncte (:) și dacă ce e înainte de : este "list" sau "last" atunci e un element de tip List sau Last
        // altfel, este un StringOutput
    }

    public List<OutputElement> getElements() {
        return elements;
    }

    public void setElements(List<OutputElement> elements) {
        this.elements = elements;
    }

    public void getOutput() {

        for (OutputElement item : elements) {
            item.build();
        }
    }
}
