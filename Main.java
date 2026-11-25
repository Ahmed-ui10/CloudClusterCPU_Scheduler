import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Path output = Paths.get("output");
        Files.createDirectories(output);

        ACSScheduler scheduler = new ACSScheduler();
        for (int testCaseNumber = 1; testCaseNumber <= 2; testCaseNumber++) {
            List<Process> testCase = TestCaseLoader.byNumber(testCaseNumber);
            SimConfig config = TestCaseLoader.assignmentConfig(testCaseNumber);
            SimulationResult result = scheduler.run(testCase, config);
            result.testCaseName = "Test Case " + testCaseNumber;

            System.out.println("\n" + MetricsCalculator.formatMetrics(result));
            System.out.println(MetricsCalculator.formatTrace(result));

            String prefix = "ACS_TestCase" + testCaseNumber;
            TestCaseLoader.exportMetrics(result, output.resolve(prefix + "_metrics.csv").toString());
            TestCaseLoader.exportTrace(result, output.resolve(prefix + "_trace.csv").toString());
            TestCaseLoader.appendSummary(output.resolve("ACS_summary.csv").toString(),
                    result.testCaseName, result);
        }

        System.out.println("ACS Tier 1 tests completed. CSV files are in the output folder.");
        System.out.println("NOTE: Tier 2 currently uses a temporary FIFO placeholder for Student 3.");
    }
}
