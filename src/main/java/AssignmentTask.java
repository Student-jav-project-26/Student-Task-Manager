import java.time.LocalDate;

public class AssignmentTask implements Task {
    private final String title;
    private final LocalDate dueDate;

    public AssignmentTask(String title, LocalDate dueDate) {
        this.title = title;
        this.dueDate = dueDate;
    }

    @Override
    public void displayTaskDetails() {
        System.out.println("Assignment Task");
        System.out.println("Title: " + title);
        System.out.println("Due date: " + dueDate);
        System.out.println("--------------------");
    }
}
