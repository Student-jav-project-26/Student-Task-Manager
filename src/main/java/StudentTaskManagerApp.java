import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StudentTaskManagerApp extends Application {
    private final ObservableList<TaskItem> tasks = FXCollections.observableArrayList();
    private final ListView<TaskItem> taskList = new ListView<>(tasks);
    private final Label statusLabel = new Label("Ready. Add a task to get started.");
    private final Label backgroundLabel = new Label("Background checker is starting...");
    private final TextField taskNameField = new TextField();
    private final DatePicker deadlinePicker = new DatePicker(LocalDate.now().plusDays(7));
    private final Path csvPath = Path.of("tasks.csv");
    private ScheduledExecutorService backgroundExecutor;

    @Override
    public void start(Stage stage) {
        Label heading = new Label("Student Task Manager");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        taskNameField.setPromptText("Enter task name");
        taskNameField.setAccessibleText("Task name");
        taskNameField.setTooltip(new Tooltip("Enter a short, descriptive name for the assignment or exam."));
        deadlinePicker.setPromptText("Choose deadline");
        deadlinePicker.setAccessibleText("Task deadline");
        deadlinePicker.setTooltip(new Tooltip("Select the task deadline using the calendar."));

        Button addButton = new Button("Add Task");
        addButton.setTooltip(new Tooltip("Add the task to your list and save it to tasks.csv."));
        Button completeButton = new Button("Mark Selected Complete");
        completeButton.setTooltip(new Tooltip("Mark the selected task as completed and save the change."));
        addButton.setOnAction(event -> addTask());
        completeButton.setOnAction(event -> markSelectedComplete());

        HBox inputRow = new HBox(10, taskNameField, deadlinePicker, addButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(taskNameField, Priority.ALWAYS);

        taskList.setPlaceholder(new Label("No tasks yet. Add your first task above."));
        taskList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TaskItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toDisplayString());
                    setTextFill(item.isCompleted() ? Color.web("#176b36") : Color.web("#172554"));
                    setStyle("-fx-font-size: 16px; -fx-padding: 10px;");
                }
            }
        });

        statusLabel.setStyle("-fx-font-size: 14px;");
        backgroundLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0b5394;");

        VBox top = new VBox(12, heading, inputRow);
        top.setPadding(new Insets(18));
        top.setStyle("-fx-background-color: #17365d;");

        VBox center = new VBox(10, new Label("Your tasks"), taskList, completeButton);
        center.setPadding(new Insets(18));
        VBox.setVgrow(taskList, Priority.ALWAYS);

        VBox bottom = new VBox(7, statusLabel, backgroundLabel);
        bottom.setPadding(new Insets(12, 18, 18, 18));
        bottom.setStyle("-fx-background-color: #eef4fb;");

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);
        root.setStyle("-fx-background-color: #ffffff;");

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("Student Task Manager");
        stage.setMinWidth(760);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();

        loadExistingTasks();
        startBackgroundChecker();
    }

    private void addTask() {
        String name = taskNameField.getText().trim();
        LocalDate deadline = deadlinePicker.getValue();
        if (name.isEmpty()) {
            statusLabel.setText("Please enter a task name.");
            taskNameField.requestFocus();
            return;
        }
        if (deadline == null) {
            statusLabel.setText("Please select a deadline.");
            return;
        }

        tasks.add(new TaskItem(name, deadline, false));
        taskNameField.clear();
        taskList.getSelectionModel().selectLast();
        saveTasks();
        statusLabel.setText("Task added and saved.");
    }

    private void markSelectedComplete() {
        TaskItem selected = taskList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a task before marking it complete.");
            return;
        }
        selected.setCompleted(true);
        taskList.refresh();
        saveTasks();
        statusLabel.setText("Task marked complete and saved.");
    }

    private void saveTasks() {
        try (BufferedWriter writer = Files.newBufferedWriter(
                csvPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            writer.write("Task Name,Deadline,Status");
            writer.newLine();
            for (TaskItem task : tasks) {
                writer.write(csvEscape(task.getName()) + ","
                        + task.getDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE) + ","
                        + (task.isCompleted() ? "Completed" : "Pending"));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException exception) {
            statusLabel.setText("Could not save tasks.csv: " + exception.getMessage());
        }
    }

    private String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void loadExistingTasks() {
        if (!Files.exists(csvPath)) {
            statusLabel.setText("No saved task file found. A file will be created when you add a task.");
            return;
        }
        try (var reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Skip the CSV header.
            while ((line = reader.readLine()) != null) {
                // This small parser supports the quoted commas written by csvEscape().
                String[] fields = parseCsvLine(line);
                if (fields.length >= 3) {
                    LocalDate date = LocalDate.parse(fields[1]);
                    tasks.add(new TaskItem(fields[0], date, fields[2].equalsIgnoreCase("Completed")));
                }
            }
            taskList.refresh();
            statusLabel.setText("Saved tasks loaded from tasks.csv.");
        } catch (IOException | RuntimeException exception) {
            statusLabel.setText("Could not load tasks.csv: " + exception.getMessage());
        }
    }

    private String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private void startBackgroundChecker() {
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "task-background-checker");
            thread.setDaemon(true);
            return thread;
        });
        backgroundExecutor.scheduleAtFixedRate(() -> {
            String message = "Checking upcoming deadlines... (" + LocalDate.now() + ")";
            Platform.runLater(() -> backgroundLabel.setText(message));
        }, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class TaskItem {
        private final String name;
        private final LocalDate deadline;
        private boolean completed;

        TaskItem(String name, LocalDate deadline, boolean completed) {
            this.name = name;
            this.deadline = deadline;
            this.completed = completed;
        }

        String getName() { return name; }
        LocalDate getDeadline() { return deadline; }
        boolean isCompleted() { return completed; }
        void setCompleted(boolean completed) { this.completed = completed; }

        String toDisplayString() {
            return (completed ? "✓ COMPLETED" : "○ PENDING")
                    + "   |   " + name + "   |   Due: " + deadline;
        }
    }
}
