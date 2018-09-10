public class Exceedence {
    private String startTime;
    private String endTime;

    private int startLine;
    private int endLine;

    private String exceedenceName;

    public Exceedence(String startTime, String endTime, int startLine, int endLine, String exceedenceName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
        this.exceedenceName = exceedenceName;
    }

    public void updateEnd(String newEndTime, int newEndLine) {
        endTime = newEndTime;
        endLine = newEndLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void print() {
        System.out.println("[" + exceedenceName + " EXCEEDENCE: line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + "]");
    }

    public String toString() {
        return  "[" + exceedenceName + " EXCEEDENCE: line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + "]";
    }


}
