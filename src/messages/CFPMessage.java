package messages;

import java.io.Serializable;
import java.util.Date;

public class CFPMessage implements Serializable {

    private Date availableTime;
    private double duration;
    private Exam exam;

    public CFPMessage(Date availableTime, double duration, Exam exam) {
        this.availableTime = availableTime;
        this.duration = duration;
        this.exam = exam;
    }

    public Date getAvailableTime() {
        return availableTime;
    }

    public double getDuration() {
        return duration;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam e) {
        this.exam = e;
    }

    @Override
    public String toString() {
        return "CFPMessage{" +
                "availableTime=" + availableTime +
                ", duration=" + duration +
                '}';
    }
}
