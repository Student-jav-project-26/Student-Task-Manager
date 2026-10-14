STUDENT TASK MANAGER - START HERE

This version uses Maven to download JavaFX libraries automatically.

1. Install IntelliJ IDEA: https://www.jetbrains.com/idea/download/
2. Install a JDK 21: https://adoptium.net/temurin/releases/?version=21
3. Extract this folder.
4. Open IntelliJ IDEA and choose File > Open.
5. Select the extracted StudentTaskManagerMaven folder (the one containing pom.xml).
6. If asked, choose Open as Project and trust the project. Wait for Maven to finish downloading dependencies.
7. Open the Maven tool window. Expand Plugins > javafx > javafx:run, then double-click javafx:run.
   Alternative: open the terminal in IntelliJ and run: mvn javafx:run
8. To run the separate OOP demonstration, open TaskDemo.java and click the green Run icon beside main().

The first run needs internet access so Maven can download JavaFX libraries.
The GUI saves tasks.csv in the working directory used by the run configuration.
