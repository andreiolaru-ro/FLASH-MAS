package net.xqhs.flash.tools.player;

import net.xqhs.flash.core.recorder.SimulationEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LogPlayerWindow extends JFrame {

    private JTable logsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JSlider timelineSlider;
    private JButton btnPlay, btnPause;
    private JPanel dynamicFilterPanel;

    private List<SimulationEvent> allEvents;
    private List<SimulationEvent> visibleEvents;

    private Timer playbackTimer;
    private boolean isPlaying = false;
    private Set<String> knownAgents = new HashSet<>();
    private List<JCheckBox> agentCheckboxes = new ArrayList<>();

    private JCheckBox chkShowSystem;
    private JCheckBox chkShowLogs;

    public LogPlayerWindow() {
        super("FLASH-MAS Log Player & Analyzer");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        allEvents = new ArrayList<>();
        visibleEvents = new ArrayList<>();

        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnLoad = new JButton("Open JSON");
        btnPlay = new JButton("▶ Play");
        btnPause = new JButton("⏸ Pause");
        btnPause.setEnabled(false);

        timelineSlider = new JSlider(0, 100, 0);
        timelineSlider.setPreferredSize(new Dimension(600, 20));
        timelineSlider.setEnabled(false);

        timelineSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                JSlider slider = (JSlider) e.getSource();

                int width = slider.getWidth();
                int mouseX = e.getX();

                int value = (int) Math.round(((double) mouseX / width) * slider.getMaximum());

                slider.setValue(value);
            }
        });

        btnLoad.addActionListener(e -> chooseFile());
        btnPlay.addActionListener(e -> startPlayback());
        btnPause.addActionListener(e -> stopPlayback());

        timelineSlider.addChangeListener(e -> {
            if (!timelineSlider.getValueIsAdjusting() && !isPlaying && !visibleEvents.isEmpty()) {
                int percent = timelineSlider.getValue();
                int targetRow = (int) ((percent / 100.0) * (visibleEvents.size() - 1));
                selectRow(targetRow);
            }
        });

        timelineSlider.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                if (visibleEvents.isEmpty()) return;

                double mousePercent = (double) e.getX() / timelineSlider.getWidth();

                mousePercent = Math.max(0, Math.min(1, mousePercent));

                int targetIndex = (int) (mousePercent * (visibleEvents.size() - 1));

                long timestamp = visibleEvents.get(targetIndex).timestamp;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
                String timeText = sdf.format(new java.util.Date(timestamp));

                timelineSlider.setToolTipText("Jump to: " + timeText);
            }
        });

        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setReshowDelay(0);

        toolbarPanel.add(btnLoad);
        toolbarPanel.add(new JSeparator(SwingConstants.VERTICAL));
        toolbarPanel.add(btnPlay);
        toolbarPanel.add(btnPause);
        toolbarPanel.add(timelineSlider);

        add(toolbarPanel, BorderLayout.NORTH);

        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setPreferredSize(new Dimension(250, 0));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel staticFilters = new JPanel();
        staticFilters.setLayout(new BoxLayout(staticFilters, BoxLayout.Y_AXIS));
        staticFilters.setBorder(BorderFactory.createTitledBorder("Event Types"));

        chkShowSystem = new JCheckBox("Show System Events", true);
        chkShowLogs = new JCheckBox("Show Logs / Messages", true);

        chkShowSystem.addActionListener(e -> applyFilters());
        chkShowLogs.addActionListener(e -> applyFilters());

        staticFilters.add(chkShowSystem);
        staticFilters.add(chkShowLogs);

        dynamicFilterPanel = new JPanel();
        dynamicFilterPanel.setLayout(new BoxLayout(dynamicFilterPanel, BoxLayout.Y_AXIS));

        JScrollPane agentScrollPane = new JScrollPane(dynamicFilterPanel);
        agentScrollPane.setBorder(BorderFactory.createTitledBorder("Filter Agents"));

        sidebarPanel.add(staticFilters, BorderLayout.NORTH);
        sidebarPanel.add(agentScrollPane, BorderLayout.CENTER);

        add(sidebarPanel, BorderLayout.WEST);

        String[] columnNames = {"Time", "Timestamp", "Agent/Source", "Type", "Payload"};

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        logsTable = new JTable(tableModel);
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        logsTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        logsTable.getColumnModel().getColumn(4).setPreferredWidth(400);

        JScrollPane tableScroll = new JScrollPane(logsTable);
        add(tableScroll, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready. Please load a file.");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        playbackTimer = new Timer(100, e -> playNextStep());

        setLocationRelativeTo(null);
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser(".");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadData(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadData(String filePath) {
        statusLabel.setText("Loading...");
        stopPlayback();

        new Thread(() -> {
            try {
                allEvents = LogLoader.loadFromFile(filePath);

                knownAgents.clear();
                for (SimulationEvent evt : allEvents) {
                    if (evt.entityName != null && !evt.entityName.isEmpty()) {
                        knownAgents.add(evt.entityName);
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    rebuildAgentFilters();
                    applyFilters();

                    timelineSlider.setEnabled(true);
                    statusLabel.setText("Loaded " + allEvents.size() + " events.");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void rebuildAgentFilters() {
        dynamicFilterPanel.removeAll();
        agentCheckboxes.clear();

        for (String agentName : knownAgents) {
            JCheckBox chk = new JCheckBox(agentName, true);
            chk.addActionListener(e -> applyFilters());
            dynamicFilterPanel.add(chk);
            agentCheckboxes.add(chk);
        }

        dynamicFilterPanel.revalidate();
        dynamicFilterPanel.repaint();
    }

    private void applyFilters() {
        Set<String> selectedAgents = agentCheckboxes.stream()
                .filter(JCheckBox::isSelected)
                .map(JCheckBox::getText)
                .collect(Collectors.toSet());

        boolean showSys = chkShowSystem.isSelected();
        boolean showLog = chkShowLogs.isSelected();

        visibleEvents = allEvents.stream().filter(evt -> {
            boolean isAgentSelected = selectedAgents.contains(evt.entityName);

            boolean isSystemEvent = evt.type.contains("START") || evt.type.contains("STOP");

            if (!isAgentSelected) return false;

            if (isSystemEvent && !showSys) return false;
            return isSystemEvent || showLog;
        }).collect(Collectors.toList());

        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS");

        for (SimulationEvent evt : visibleEvents) {
            String payloadStr = (evt.payload != null) ? evt.payload.toString() : "";

            tableModel.addRow(new Object[]{
                    sdf.format(new java.util.Date(evt.timestamp)),
                    evt.timestamp,
                    evt.entityName,
                    evt.type,
                    payloadStr
            });
        }
    }

    private void startPlayback() {
        if (visibleEvents.isEmpty()) return;

        int currentRow = logsTable.getSelectedRow();
        int totalRows = tableModel.getRowCount();

        if (currentRow == -1 || currentRow >= totalRows - 1) {
            selectRow(0);
            timelineSlider.setValue(0);
        }

        isPlaying = true;
        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);
        dynamicFilterPanel.setEnabled(false);

        playbackTimer.start();
    }

    private void stopPlayback() {
        isPlaying = false;
        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
        dynamicFilterPanel.setEnabled(true);
        playbackTimer.stop();
    }

    private void playNextStep() {
        int currentRow = logsTable.getSelectedRow();
        int nextRow = currentRow + 1;
        int totalRows = tableModel.getRowCount();

        if (nextRow < totalRows) {
            selectRow(nextRow);

            if (totalRows > 1) {
                int progress = (int) (((double) nextRow / (totalRows - 1)) * 100);
                timelineSlider.setValue(progress);
            }
        } else {
            stopPlayback();
            timelineSlider.setValue(100);
        }
    }

    private void selectRow(int row) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            logsTable.setRowSelectionInterval(row, row);

            scrollToCenter(row);
        }
    }

    private void scrollToCenter(int rowIndex) {
        if (!(logsTable.getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport) logsTable.getParent();

        Rectangle rect = logsTable.getCellRect(rowIndex, 0, true);

        int viewHeight = viewport.getHeight();
        int tableHeight = logsTable.getHeight();

        if (tableHeight <= viewHeight) {
            viewport.setViewPosition(new Point(0, 0));
            return;
        }

        int rowCenterY = rect.y + (rect.height / 2);
        int halfViewHeight = viewHeight / 2;
        int targetY = rowCenterY - halfViewHeight;

        int maxY = tableHeight - viewHeight;

        if (targetY < 0) {
            targetY = 0;
        }
        if (targetY > maxY) {
            targetY = maxY;
        }

        viewport.setViewPosition(new Point(0, targetY));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LogPlayerWindow().setVisible(true));
    }
}