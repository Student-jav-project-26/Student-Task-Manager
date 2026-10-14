import java.time.LocalDate;

public class ExamTask implements Task {
    private final String subject;
    private final LocalDate examDate;

    public ExamTask(String subject, LocalDate examDate) {
        this.subject = subject;
        this.examDate = examDate;
    }

    @Override
    public void displayTaskDetails() {
        System.out.println("Exam Task");
        System.out.println("Subject: " + subject);
        System.out.println("Exam date: " + examDate);
        System.out.println("--------------------");
    }
}
