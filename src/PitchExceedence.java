

public class PitchExceedence {
    private String startTime;
    private String endTime;

    private int startLine;
    private int endLine;

    public PitchExceedence(String startTime, String endTime, int startLine, int endLine) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public void updateEnd(String newEndTime, int newEndLine) {
        endTime = newEndTime;
        endLine = newEndLine;
    }

    public void print() {
        System.out.println("[PITCH EXCEEDENCE: line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + "]");
    }
}
