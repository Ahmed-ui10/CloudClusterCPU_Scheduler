import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GuiMain extends JFrame {

    private final Color BG_COLOR = new Color(30, 30, 30);
    private final Color PANEL_COLOR = new Color(37, 37, 38);
    private final Color TEXT_COLOR = Color.WHITE;
    private final Color ACCENT_COLOR = new Color(0, 122, 204);
    private final Color ACCENT_HOVER = new Color(28, 151, 234);
    private final Color TABLE_HEADER_BG = new Color(45, 45, 48);
    private final Color TABLE_ROW_ALT = new Color(43, 43, 43);

    private final JComboBox<String> testCaseCombo = new JComboBox<>(new String[]{"Test Case 1", "Test Case 2"});
    private final JComboBox<String> schedulerCombo = new JComboBox<>(new String[]{"ACS", "SJF"});
    private final JTextField qField = new JTextField("3");
    private final JTextField csField = new JTextField("0");

    private final DefaultTableModel traceModel = new DefaultTableModel(
            new String[]{"Time", "Running", "Tier 1", "Tier 2", "Notes"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };

    private final DefaultTableModel metricsModel = new DefaultTableModel(
            new String[]{"Process", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };

    private final JTable traceTable = new JTable(traceModel);
    private final JTable metricsTable = new JTable(metricsModel);

    private final JLabel avgLabel = new JLabel(" Ready to simulate... ");

    public GuiMain() {
        setTitle("CloudCluster CPU Scheduler");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        UIManager.put("Label.foreground", TEXT_COLOR);
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("OptionPane.background", PANEL_COLOR);
        UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
        UIManager.put("TextField.background", PANEL_COLOR);
        UIManager.put("TextField.foreground", Color.WHITE);
        UIManager.put("TextField.caretForeground", Color.WHITE);
        UIManager.put("ComboBox.background", PANEL_COLOR);
        UIManager.put("ComboBox.foreground", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", ACCENT_COLOR);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("TabbedPane.background", BG_COLOR);
        UIManager.put("TabbedPane.foreground", TEXT_COLOR);
        UIManager.put("TabbedPane.selected", PANEL_COLOR);

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(BG_COLOR);
        topWrapper.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        controls.setBackground(PANEL_COLOR);
        controls.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1));

        schedulerCombo.setPreferredSize(new Dimension(100, 30));
        testCaseCombo.setPreferredSize(new Dimension(130, 30));
        qField.setPreferredSize(new Dimension(50, 30));
        csField.setPreferredSize(new Dimension(50, 30));

        schedulerCombo.setRenderer(new DarkComboRenderer());
        testCaseCombo.setRenderer(new DarkComboRenderer());

        testCaseCombo.addActionListener(e -> {
            int selected = testCaseCombo.getSelectedIndex();
            if (selected == 0) { 
                qField.setText("3");
                csField.setText("0");
            } else if (selected == 1) { 
                qField.setText("3");
                csField.setText("1");
            }
        });

        controls.add(createLabel("Scheduler:"));
        controls.add(schedulerCombo);
        controls.add(createLabel("Test Case:"));
        controls.add(testCaseCombo);
        controls.add(createLabel("Quantum (Q):"));
        controls.add(qField);
        controls.add(createLabel("Context Switch (CS):"));
        controls.add(csField);

        JButton runBtn = new JButton("Run Simulation");
        runBtn.setBackground(ACCENT_COLOR);
        runBtn.setForeground(Color.WHITE);
        runBtn.setFocusPainted(false);
        runBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        runBtn.setBorder(new EmptyBorder(8, 25, 8, 25));
        runBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        runBtn.setOpaque(true);
        runBtn.setBorderPainted(false);
        
        runBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { runBtn.setBackground(ACCENT_HOVER); }
            public void mouseExited(MouseEvent e) { runBtn.setBackground(ACCENT_COLOR); }
        });
        runBtn.addActionListener(e -> runSimulation());
        
        controls.add(Box.createHorizontalStrut(10));
        controls.add(runBtn);

        topWrapper.add(controls, BorderLayout.CENTER);
        add(topWrapper, BorderLayout.NORTH);

        styleTable(traceTable);
        styleTable(metricsTable);

        UIManager.put("ScrollPane.background", BG_COLOR);
        UIManager.put("Viewport.background", BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(new EmptyBorder(0, 20, 0, 20));
        tabs.addTab("  Trace (per time unit)  ", new JScrollPane(traceTable));
        tabs.addTab("  Metrics (per process)  ", new JScrollPane(metricsTable));
        add(tabs, BorderLayout.CENTER);

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setBackground(BG_COLOR);
        bottomWrapper.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statsPanel.setBackground(PANEL_COLOR);
        statsPanel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1));
        
        avgLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avgLabel.setForeground(new Color(0, 255, 127));
        statsPanel.add(avgLabel);

        bottomWrapper.add(statsPanel, BorderLayout.CENTER);
        add(bottomWrapper, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_COLOR);
        return lbl;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(35); 
        
        table.setShowVerticalLines(true); 
        table.setShowHorizontalLines(true);
        table.setGridColor(Color.WHITE); 
        
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setBackground(PANEL_COLOR);
        table.setForeground(TEXT_COLOR);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? PANEL_COLOR : TABLE_ROW_ALT);
                    c.setForeground(Color.WHITE); 
                }
                setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(100, 40));
        
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBackground(TABLE_HEADER_BG);
                label.setForeground(Color.WHITE);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.WHITE)); 
                return label;
            }
        });
    }

    private void runSimulation() {
        try {
            int testCase = testCaseCombo.getSelectedIndex() + 1;
            int q = Integer.parseInt(qField.getText().trim());
            int cs = Integer.parseInt(csField.getText().trim());
            String schedName = (String) schedulerCombo.getSelectedItem();

            SimConfig config = new SimConfig(q, cs);
            List<Process> processes = TestCaseLoader.byNumber(testCase);

            Scheduler scheduler = schedName.equals("SJF") ? new SJFScheduler() : new ACSScheduler();
            SimulationResult result = scheduler.run(processes, config);
            result.testCaseName = "Test Case " + testCase;

            traceModel.setRowCount(0);
            for (TraceRow row : result.trace) {
                traceModel.addRow(new Object[]{
                        row.time, row.running, row.tier1, row.tier2, row.notes
                });
            }

            metricsModel.setRowCount(0);
            for (Process p : result.processes) {
                metricsModel.addRow(new Object[]{
                        p.name(), p.arrival, p.burst, p.finish,
                        MetricsCalculator.turnaround(p),
                        MetricsCalculator.waiting(p),
                        MetricsCalculator.response(p)
                });
            }

            avgLabel.setText(String.format(
                    "Avg Waiting = %.2f    |    Avg Turnaround = %.2f    |    Avg Response = %.2f    |    Total Time = %d",
                    result.avgWaiting, result.avgTurnaround, result.avgResponse, result.totalTime));
            
            avgLabel.setForeground(new Color(0, 255, 127));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(), "Simulation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    class DarkComboRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBackground(isSelected ? ACCENT_COLOR : PANEL_COLOR);
            setForeground(Color.WHITE);
            setBorder(new EmptyBorder(5, 10, 5, 10));
            return this;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new GuiMain().setVisible(true));
    }
}