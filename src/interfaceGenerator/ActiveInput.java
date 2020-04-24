package interfaceGenerator;

public class ActiveInput {
    private static ActiveInput obj = null;
    private String inputValue;

    private ActiveInput() {
    }

    public static ActiveInput getActiveInputInstance() {
        if (obj == null) {
            obj = new ActiveInput();
        }
        return obj;
    }

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    @Override
    public String toString() {
        return "ActiveInput{" +
                "inputValue='" + inputValue + '\'' +
                '}';
    }
}
