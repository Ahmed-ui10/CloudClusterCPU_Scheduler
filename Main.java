import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Path output = Paths.get("output");
        Files.createDirectories(output);

        Scheduler[] schedulers = { new SJFScheduler(), new ACSScheduler() };

        for (Scheduler scheduler : schedulers) {
            for (int testCaseNumber = 1; testCaseNumber <= 2; testCaseNumber++) {
                List<Process> testCase = TestCaseLoader.byNumber(testCaseNumber);
                SimConfig config = TestCaseLoader.assignmentConfig(testCaseNumber);
                SimulationResult result = scheduler.run(testCase, config);
                result.testCaseName = "Test Case " + testCaseNumber;

                System.out.println("\n" + MetricsCalculator.formatMetrics(result));
                System.out.println(MetricsCalculator.formatTrace(result));

                String prefix = scheduler.name().replace(" ", "_") + "_TestCase" + testCaseNumber;
                TestCaseLoader.exportMetrics(result, output.resolve(prefix + "_metrics.csv").toString());
                TestCaseLoader.exportTrace(result, output.resolve(prefix + "_trace.csv").toString());
                TestCaseLoader.appendSummary(output.resolve("Summary.csv").toString(),
                        result.testCaseName, result);
            }
        }

        System.out.println("All simulations completed. CSV files are in the output folder.");
    }
}