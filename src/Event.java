public class Event {
    private String startTime;
    private String endTime;

    private int startLine;
    private int endLine;

    private String eventName;

    public Event(String startTime, String endTime, int startLine, int endLine, String eventName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
        this.eventName = eventName;
    }

    public void updateEnd(String newEndTime, int newEndLine) {
        endTime = newEndTime;
        endLine = newEndLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void print() {
        System.out.println("[" + eventName + ": line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + "]");
    }

    public String toString() {
        return  "[" + eventName + ": line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + "]";
    }
}
