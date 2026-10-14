import java.time.LocalDate;

public class TaskDemo {
    public static void main(String[] args) {
        Task assignment = new AssignmentTask("Microeconomics essay", LocalDate.of(2026, 10, 30));
        Task exam = new ExamTask("Introduction to Programming", LocalDate.of(2026, 11, 12));

        assignment.displayTaskDetails();
        exam.displayTaskDetails();
    }
}
