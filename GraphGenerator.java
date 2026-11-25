import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class GraphGenerator {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int MARGIN = 80;

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get("graphs"));
        Scheduler sjf = new SJFScheduler();
        Scheduler acs = new ACSScheduler();

        for (int tc = 1; tc <= 2; tc++) {
            Map<Integer, Double> sjfWaitQ = new LinkedHashMap<>();
            Map<Integer, Double> acsWaitQ = new LinkedHashMap<>();
            Map<Integer, Double> sjfTatQ = new LinkedHashMap<>();
            Map<Integer, Double> acsTatQ = new LinkedHashMap<>();

            for (int q = 1; q <= 5; q++) {
                sjfWaitQ.put(q, sjf.run(TestCaseLoader.byNumber(tc), new SimConfig(q, 0)).avgWaiting);
                acsWaitQ.put(q, acs.run(TestCaseLoader.byNumber(tc), new SimConfig(q, 0)).avgWaiting);
                sjfTatQ.put(q, sjf.run(TestCaseLoader.byNumber(tc), new SimConfig(q, 0)).avgTurnaround);
                acsTatQ.put(q, acs.run(TestCaseLoader.byNumber(tc), new SimConfig(q, 0)).avgTurnaround);
            }

            drawLineChart(sjfWaitQ, acsWaitQ, "TC" + tc + ": Avg Waiting vs Q", "Q", "Average Waiting Time", "graphs/waiting_vs_q_tc" + tc + ".png");
            drawLineChart(sjfTatQ, acsTatQ, "TC" + tc + ": Avg Turnaround vs Q", "Q", "Average Turnaround Time", "graphs/turnaround_vs_q_tc" + tc + ".png");
        }

        for (int tc = 1; tc <= 2; tc++) {
            Map<Integer, Double> sjfWaitCS = new LinkedHashMap<>();
            Map<Integer, Double> acsWaitCS = new LinkedHashMap<>();
            Map<Integer, Double> sjfTatCS = new LinkedHashMap<>();
            Map<Integer, Double> acsTatCS = new LinkedHashMap<>();

            for (int cs = 0; cs <= 2; cs++) {
                sjfWaitCS.put(cs, sjf.run(TestCaseLoader.byNumber(tc), new SimConfig(3, cs)).avgWaiting);
                acsWaitCS.put(cs, acs.run(TestCaseLoader.byNumber(tc), new SimConfig(3, cs)).avgWaiting);
                sjfTatCS.put(cs, sjf.run(TestCaseLoader.byNumber(tc), new SimConfig(3, cs)).avgTurnaround);
                acsTatCS.put(cs, acs.run(TestCaseLoader.byNumber(tc), new SimConfig(3, cs)).avgTurnaround);
            }

            drawLineChart(sjfWaitCS, acsWaitCS, "TC" + tc + ": Avg Waiting vs CS", "CS", "Average Waiting Time", "graphs/waiting_vs_cs_tc" + tc + ".png");
            drawLineChart(sjfTatCS, acsTatCS, "TC" + tc + ": Avg Turnaround vs CS", "CS", "Average Turnaround Time", "graphs/turnaround_vs_cs_tc" + tc + ".png");
        }

        System.out.println("All graphs generated in graphs/ folder.");
    }

    private static void drawLineChart(Map<Integer, Double> data1, Map<Integer, Double> data2, String title, String xLabel, String yLabel, String path) throws IOException {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fmTitle = g.getFontMetrics();
        g.drawString(title, (WIDTH - fmTitle.stringWidth(title)) / 2, 35);

        int plotX = MARGIN;
        int plotY = 60;
        int plotW = WIDTH - 2 * MARGIN;
        int plotH = HEIGHT - 2 * MARGIN;

        double minY = 0;
        double maxY = -Double.MAX_VALUE;
        for (double v : data1.values()) if (v > maxY) maxY = v;
        for (double v : data2.values()) if (v > maxY) maxY = v;
        if (maxY == 0) maxY = 1;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int k : data1.keySet()) { if (k < minX) minX = k; if (k > maxX) maxX = k; }
        if (minX == maxX) { minX = 0; maxX = minX + 1; }

        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(plotX, plotY, plotX, plotY + plotH);
        g.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fmAxis = g.getFontMetrics();
        int yTicks = 10;
        for (int i = 0; i <= yTicks; i++) {
            double val = minY + (maxY - minY) * i / yTicks;
            int y = plotY + plotH - (int) ((val - minY) / (maxY - minY) * plotH);
            g.setColor(new Color(230, 230, 230));
            g.drawLine(plotX, y, plotX + plotW, y);
            g.setColor(Color.BLACK);
            String label = String.format("%.1f", val);
            g.drawString(label, plotX - 10 - fmAxis.stringWidth(label), y + 4);
        }

        for (Map.Entry<Integer, Double> e : data1.entrySet()) {
            int x = plotX + (int) ((e.getKey() - minX) / (double) (maxX - minX) * plotW);
            String label = String.valueOf(e.getKey());
            g.drawString(label, x - fmAxis.stringWidth(label) / 2, plotY + plotH + 18);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fmLabel = g.getFontMetrics();
        g.drawString(xLabel, plotX + plotW / 2 - fmLabel.stringWidth(xLabel) / 2, plotY + plotH + 40);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(-Math.PI / 2, 25, plotY + plotH / 2);
        g2.drawString(yLabel, 25 - fmLabel.stringWidth(yLabel) / 2, plotY + plotH / 2);
        g2.dispose();

        drawSeries(g, data1, Color.RED, minX, maxX, minY, maxY, plotX, plotY, plotW, plotH);
        drawSeries(g, data2, new Color(0, 102, 204), minX, maxX, minY, maxY, plotX, plotY, plotW, plotH);

        g.setColor(Color.RED); g.fillRect(plotX + plotW - 100, plotY + 10, 15, 15);
        g.setColor(Color.BLACK); g.drawString("SJF", plotX + plotW - 80, plotY + 23);
        g.setColor(new Color(0, 102, 204)); g.fillRect(plotX + plotW - 100, plotY + 30, 15, 15);
        g.setColor(Color.BLACK); g.drawString("ACS", plotX + plotW - 80, plotY + 43);

        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }

    private static void drawSeries(Graphics2D g, Map<Integer, Double> data, Color color, int minX, int maxX, double minY, double maxY, int plotX, int plotY, int plotW, int plotH) {
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f));
        Integer prevX = null, prevY = null;
        for (Map.Entry<Integer, Double> e : data.entrySet()) {
            int x = plotX + (int) ((e.getKey() - minX) / (double) (maxX - minX) * plotW);
            int y = plotY + plotH - (int) ((e.getValue() - minY) / (maxY - minY) * plotH);
            if (prevX != null) g.drawLine(prevX, prevY, x, y);
            prevX = x; prevY = y;
        }
        for (Map.Entry<Integer, Double> e : data.entrySet()) {
            int x = plotX + (int) ((e.getKey() - minX) / (double) (maxX - minX) * plotW);
            int y = plotY + plotH - (int) ((e.getValue() - minY) / (maxY - minY) * plotH);
            g.fillOval(x - 5, y - 5, 10, 10);
        }
    }
}