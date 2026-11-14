package abms.smartMeeting;

public class TimeSlot {
    private final int startMinute;
    private final int endMinute;

    public TimeSlot(int startMinute, int endMinute) {
        if (endMinute <= startMinute)
            throw new IllegalArgumentException("endMinute must be greater than startMinute");
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public boolean overlaps(TimeSlot other) {
        if (other == null)
            return false;
        return startMinute < other.endMinute && other.startMinute < endMinute;
    }

    public int durationMinutes() {
        return endMinute - startMinute;
    }

    public static TimeSlot parse(String value) {
        if (value == null || !value.contains("-"))
            return new TimeSlot(9 * 60, 10 * 60);
        String[] parts = value.split("-", 2);
        return new TimeSlot(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    @Override
    public String toString() {
        return startMinute + "-" + endMinute;
    }
}
