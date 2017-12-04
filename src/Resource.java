import jade.core.Agent;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeSet;

public class Resource extends Agent {

    private HashSet<Exam> validExams;
    private TreeSet<ScheduledExam> scheduledExams;

    public static void main(String[] args) throws ParseException {
        DateFormat f = new SimpleDateFormat("d/M/y H:m");
        Date d1 = f.parse("25/11/2017 10:00");
        Date d2 = f.parse("25/11/2017 08:00");
        Date d3 = f.parse("25/11/2017 11:00");
        Date d4 = f.parse("25/11/2017 18:00");
        Date d5 = f.parse("25/11/2017 08:00");
        TreeSet<ScheduledExam> exams = new TreeSet<>();

        exams.add(new ScheduledExam(d1, Exam.EKG, 1));
        exams.add(new ScheduledExam(d2, Exam.CATSCAN, 1));
        exams.add(new ScheduledExam(d3, Exam.IVP, 1));
        exams.add(new ScheduledExam(d4, Exam.MRI, 1));
        exams.add(new ScheduledExam(d5, Exam.PROSTATEEXAM, 1));

        for (ScheduledExam s:
             exams) {
            System.out.println(s);
        }
    }
}