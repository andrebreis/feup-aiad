import jade.core.AID;
import messages.Exam;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScheduledExam implements Comparable, Serializable {

    private AID patient;
    private Date beginDate;
    private Date endDate;
    private Exam exam;

    public ScheduledExam(Date beginDate, double duration, Exam exam, AID p) {
        this.patient = p;
        this.beginDate = beginDate;
        this.exam = exam;
        this.endDate = new Date((long) (this.beginDate.getTime() + duration * 60 * 60 * 1000));
    }

    public AID getPatient() {
        return patient;
    }

    public void setPatient(AID patient) {
        this.patient = patient;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    @Override
    public String toString() {
        return "ScheduledExam{" +
                "beginDate=" + beginDate +
                ", endDate=" + endDate +
                ", exam=" + exam +
                '}';
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public boolean examsOverlap(ScheduledExam s) {
        if(s == null) return false;
        if(this.beginDate.getTime() <= s.beginDate.getTime()) {
            if (this.endDate.getTime() > s.beginDate.getTime())
                return true;
        }
        else if (s.endDate.getTime() > this.beginDate.getTime())
            return true;
        return false;
    }

    @Override
    public int compareTo(Object o) {
        //TODO: Verify if begin date is between begin date and end date - if so throw error and see if it happens on .add
        if (!(o instanceof ScheduledExam))
            throw new ClassCastException();
        if (this == o) return 0;
        ScheduledExam otherExam = (ScheduledExam) o;
        if(this.beginDate.getTime() <= otherExam.beginDate.getTime()) {
            if (this.endDate.getTime() > otherExam.beginDate.getTime())
                throw new OverlappingDatesException();
        }
        else if (otherExam.endDate.getTime() > this.beginDate.getTime())
            throw new OverlappingDatesException();
        return this.beginDate.compareTo(otherExam.beginDate);
    }
}
