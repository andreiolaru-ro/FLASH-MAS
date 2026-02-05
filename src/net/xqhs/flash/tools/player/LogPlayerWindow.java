package net.xqhs.flash.tools.player;

import net.xqhs.flash.core.recorder.SimulationEvent;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main GUI window for the Log Player tool.
 * <p>
 * Features include:
 * <ul>
 * <li>Loading and parsing large JSON simulation logs.</li>
 * <li>Visual playback controls (Play, Pause, Timeline).</li>
 * <li>Dynamic filtering based on identified agents.</li>
 * <li>Read-only data table with auto-scrolling.</li>
 * </ul>
 * </p>
 */
public class LogPlayerWindow extends JFrame {

    //  GUI Components
    protected JTable logsTable;
    protected DefaultTableModel tableModel;
    protected JLabel statusLabel;
    protected JSlider timelineSlider;
    protected JLabel lblCurrentTime;
    protected JButton btnPlay, btnPause;

    // Filter Panels
    protected JPanel filtersContainerPanel;
    protected JPanel eventTypesPanel;
    protected JPanel agentFilterPanel;

    // Data
    protected List<SimulationEvent> allEvents = new ArrayList<>();
    protected List<SimulationEvent> visibleEvents = new ArrayList<>();
    protected Set<String> knownAgents = new HashSet<>();
    protected Set<String> knownEventTypes = new HashSet<>();

    // --- Filter State ---
    // Map: Category Name -> List of Checkboxes (one per event type)
    protected Map<String, List<JCheckBox>> categoryCheckboxes = new HashMap<>();
    // Map: Agent Name -> Checkbox
    protected Map<String, JCheckBox> agentCheckboxes = new HashMap<>();
    // Logging Level Selector
    protected JComboBox<String> logLevelCombo;
    protected static final String[] LOG_LEVELS = {"ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"};

    // --- Playback ---
    protected Timer playbackTimer;
    protected boolean isPlaying = false;

    public LogPlayerWindow() {
        super("FLASH-MAS Log Player & Analyzer");
        setSize(1300, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Toolbar (Top)
        JPanel toolbarPanel = createToolbar();
        add(toolbarPanel, BorderLayout.NORTH);

        // 2. Sidebar (Filters)
        JScrollPane sidebarScroll = createSidebar();
        add(sidebarScroll, BorderLayout.WEST);

        // 3. Main Table (Center)
        JScrollPane tableScroll = createMainTable();
        add(tableScroll, BorderLayout.CENTER);

        // 4. Status Bar (Bottom)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready. Please load a file.");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // Timer
        playbackTimer = new Timer(100, e -> playNextStep());

        setLocationRelativeTo(null);
    }

    // --- UI Construction Helpers ---

    private JPanel createToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnLoad = new JButton("Open JSON");
        btnPlay = new JButton("Play");
        btnPause = new JButton("Pause");
        btnPause.setEnabled(false);

        lblCurrentTime = new JLabel("00:00:00.000");
        lblCurrentTime.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblCurrentTime.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        timelineSlider = new JSlider(0, 100, 0);
        timelineSlider.setPreferredSize(new Dimension(500, 20));
        timelineSlider.setEnabled(false);

        // Slider listeners (same as before)
        setupSliderListeners();

        btnLoad.addActionListener(e -> chooseFile());
        btnPlay.addActionListener(e -> startPlayback());
        btnPause.addActionListener(e -> stopPlayback());

        panel.add(btnLoad);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(btnPlay);
        panel.add(btnPause);
        panel.add(timelineSlider);
        panel.add(lblCurrentTime);
        return panel;
    }

    private JScrollPane createSidebar() {
        filtersContainerPanel = new JPanel();
        filtersContainerPanel.setLayout(new BoxLayout(filtersContainerPanel, BoxLayout.Y_AXIS));
        filtersContainerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 1. Event Types Section (Hierarchical)
        eventTypesPanel = new JPanel();
        eventTypesPanel.setLayout(new BoxLayout(eventTypesPanel, BoxLayout.Y_AXIS));
        // We will populate this dynamically in rebuildEventTypeFilters()

        // 2. Agents Section
        agentFilterPanel = new JPanel();
        agentFilterPanel.setLayout(new BoxLayout(agentFilterPanel, BoxLayout.Y_AXIS));

        // Wrappers
        JPanel eventsWrapper = new JPanel(new BorderLayout());
        eventsWrapper.setBorder(BorderFactory.createTitledBorder("Event Categories"));
        eventsWrapper.add(eventTypesPanel, BorderLayout.NORTH);

        JPanel agentsWrapper = new JPanel(new BorderLayout());
        agentsWrapper.setBorder(BorderFactory.createTitledBorder("Filter Agents"));
        agentsWrapper.add(agentFilterPanel, BorderLayout.NORTH);

        filtersContainerPanel.add(eventsWrapper);
        filtersContainerPanel.add(Box.createVerticalStrut(10));
        filtersContainerPanel.add(agentsWrapper);

        JScrollPane scroll = new JScrollPane(filtersContainerPanel);
        scroll.setPreferredSize(new Dimension(300, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JScrollPane createMainTable() {
        String[] columnNames = {"Time", "Timestamp", "Agent", "Category", "Type", "Payload"}; // Added Category column

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        logsTable = new JTable(tableModel);
        // Column sizing
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Time
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // TS
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Agent
        logsTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Cat
        logsTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Type
        logsTable.getColumnModel().getColumn(5).setPreferredWidth(400); // Payload

        return new JScrollPane(logsTable);
    }

    // --- Logic & Data Loading ---

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

                // 1. Extract Metadata
                knownAgents.clear();
                knownEventTypes.clear();

                for (SimulationEvent evt : allEvents) {
                    if (evt.getEntityName() != null) knownAgents.add(evt.getEntityName());
                    if (evt.getType() != null) knownEventTypes.add(evt.getType());
                }

                // 2. Build GUI on EDT
                SwingUtilities.invokeLater(() -> {
                    rebuildAgentFilters();
                    rebuildEventTypeFilters();
                    applyFilters(); // Aici se populeaza visibleEvents

                    if (!visibleEvents.isEmpty()) {
                        timelineSlider.setEnabled(true);

                        // --- FIX: Setam timpul initial cu timpul primului eveniment ---
                        long startTime = visibleEvents.get(0).getTimestamp();
                        lblCurrentTime.setText(formatTime(startTime));

                        // Resetam sliderul la 0 vizual
                        timelineSlider.setValue(0);
                    } else {
                        timelineSlider.setEnabled(false);
                        lblCurrentTime.setText("00:00:00.000");
                    }

                    statusLabel.setText("Loaded " + allEvents.size() + " events.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    // --- DYNAMIC FILTER CONSTRUCTION ---

    private void rebuildAgentFilters() {
        agentFilterPanel.removeAll();
        agentCheckboxes.clear();

        // "Select All" helper
        JButton btnToggle = new JButton("Toggle All Agents");
        btnToggle.addActionListener(e -> {
            boolean anySelected = agentCheckboxes.values().stream().anyMatch(AbstractButton::isSelected);
            agentCheckboxes.values().forEach(chk -> chk.setSelected(!anySelected));
            applyFilters();
        });
        agentFilterPanel.add(btnToggle);

        List<String> sortedAgents = new ArrayList<>(knownAgents);
        Collections.sort(sortedAgents);

        for (String agentName : sortedAgents) {
            JCheckBox chk = new JCheckBox(agentName, true);
            chk.addActionListener(e -> applyFilters());
            agentFilterPanel.add(chk);
            agentCheckboxes.put(agentName, chk);
        }
        agentFilterPanel.revalidate();
    }

    /**
     * Creates the hierarchical category view dynamically based on found event types.
     */
    private void rebuildEventTypeFilters() {
        eventTypesPanel.removeAll();
        categoryCheckboxes.clear();

        // Define Categories
        Set<String> lifecycleTypes = new HashSet<>();
        Set<String> messagingTypes = new HashSet<>();
        Set<String> loggingTypes = new HashSet<>();
        Set<String> otherTypes = new HashSet<>();

        // 1. Classify Event Types
        for (String type : knownEventTypes) {
            if (type.contains("START") || type.contains("STOP") || type.contains("INIT")) {
                lifecycleTypes.add(type);
            } else if (type.contains("MESSAGE") || type.contains("WAVE") || type.contains("SEND") || type.contains("RECEIVE")) {
                messagingTypes.add(type);
            } else if (type.startsWith("LOG") || type.contains("DEBUG") || type.contains("INFO") || type.contains("WARN")) {
                loggingTypes.add(type);
            } else {
                otherTypes.add(type);
            }
        }

        // 2. Build UI Panels
        if (!lifecycleTypes.isEmpty()) {
            addCategoryPanel("Lifecycle", lifecycleTypes);
        }
        if (!messagingTypes.isEmpty()) {
            addCategoryPanel("Messaging", messagingTypes);
        }
        if (!loggingTypes.isEmpty()) {
            addCategoryPanel("Logging", loggingTypes, true); // True for level selector
        }
        if (!otherTypes.isEmpty()) {
            addCategoryPanel("Other / Custom", otherTypes);
        }

        eventTypesPanel.revalidate();
        eventTypesPanel.repaint();
    }

    private void addCategoryPanel(String categoryName, Set<String> types) {
        addCategoryPanel(categoryName, types, false);
    }

    private void addCategoryPanel(String categoryName, Set<String> types, boolean hasLevelSelector) {
        JPanel catPanel = new JPanel();
        catPanel.setLayout(new BoxLayout(catPanel, BoxLayout.Y_AXIS));
        catPanel.setBorder(new TitledBorder(categoryName));
        catPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<JCheckBox> checkboxes = new ArrayList<>();

        // If it's Logging, add the Level Selector
        if (hasLevelSelector) {
            JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            levelPanel.add(new JLabel("Min Level:"));
            logLevelCombo = new JComboBox<>(LOG_LEVELS);
            logLevelCombo.setSelectedIndex(0); // ALL
            logLevelCombo.addActionListener(e -> applyFilters());
            levelPanel.add(logLevelCombo);
            catPanel.add(levelPanel);
        }

        // Add checkboxes for individual types
        List<String> sortedTypes = new ArrayList<>(types);
        Collections.sort(sortedTypes);

        for (String type : sortedTypes) {
            JCheckBox chk = new JCheckBox(type, true);
            // Indent slightly to show hierarchy
            chk.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            chk.addActionListener(e -> applyFilters());
            catPanel.add(chk);
            checkboxes.add(chk);
        }

        categoryCheckboxes.put(categoryName, checkboxes);
        eventTypesPanel.add(catPanel);
        eventTypesPanel.add(Box.createVerticalStrut(5));
    }

    // --- FILTERING ENGINE ---

    private void applyFilters() {
        // 1. Get Selected Agents
        Set<String> selectedAgents = agentCheckboxes.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // 2. Get Allowed Types from Checkboxes
        Set<String> allowedTypes = new HashSet<>();
        for (List<JCheckBox> list : categoryCheckboxes.values()) {
            for (JCheckBox chk : list) {
                if (chk.isSelected()) allowedTypes.add(chk.getText());
            }
        }

        // 3. Get Logging Level Threshold
        int minLevelIdx = (logLevelCombo != null) ? logLevelCombo.getSelectedIndex() : 0;

        // 4. Filter Stream
        visibleEvents = allEvents.stream().filter(evt -> {
            // A. Agent Filter
            if (!selectedAgents.contains(evt.getEntityName())) return false;

            // B. Type Filter (Checkbox)
            if (!allowedTypes.contains(evt.getType())) return false;

            // C. Logging Level Logic
            if (minLevelIdx > 0 && isLogEvent(evt.getType())) {
                int eventLevelIdx = getLogLevelIndex(evt.getType());
                if (eventLevelIdx < minLevelIdx) return false;
            }

            return true;
        }).collect(Collectors.toList());

        refreshTable();
    }

    private boolean isLogEvent(String type) {
        return type.startsWith("LOG") || type.contains("INFO") || type.contains("WARN") || type.contains("ERR");
    }

    private int getLogLevelIndex(String type) {
        // Simple mapping: looks for keywords in the TYPE string.
        // Assumes types like "LOG_INFO", "LOG_WARN", etc.
        String t = type.toUpperCase();
        if (t.contains("FATAL")) return 6;
        if (t.contains("ERROR")) return 5;
        if (t.contains("WARN")) return 4;
        if (t.contains("INFO")) return 3;
        if (t.contains("DEBUG")) return 2;
        if (t.contains("TRACE")) return 1;
        return 3; // Default to INFO if unknown
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS");

        for (SimulationEvent evt : visibleEvents) {
            String cat = getCategoryFor(evt.getType());
            String payloadStr = (evt.getPayload() != null) ? evt.getPayload().toString() : "";

            tableModel.addRow(new Object[]{
                    sdf.format(new java.util.Date(evt.getTimestamp())),
                    evt.getTimestamp(),
                    evt.getEntityName(),
                    cat,                // New Category Column
                    evt.getType(),
                    payloadStr
            });
        }
    }

    // Reverse lookup for table display
    private String getCategoryFor(String type) {
        if (type.contains("START") || type.contains("STOP")) return "Lifecycle";
        if (type.contains("MESSAGE") || type.contains("WAVE")) return "Messaging";
        if (type.contains("LOG")) return "Log";
        return "Other";
    }

    // --- Playback Controls (Same as before) ---
    private void startPlayback() {
        if (visibleEvents.isEmpty()) return;

        int currentRow = logsTable.getSelectedRow();
        int totalRows = tableModel.getRowCount();

        // FIX: Daca suntem la final sau nu e nimic selectat, o luam de la capat
        if (currentRow == -1 || currentRow >= totalRows - 1) {
            selectRow(0);
            timelineSlider.setValue(0);
        }

        isPlaying = true;
        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);

        // Dezactivam filtrele in timpul redarii pentru a nu corupe indexii
        if (filtersContainerPanel != null) {
            filtersContainerPanel.setEnabled(false);
        }

        playbackTimer.start();
    }

    private void stopPlayback() {
        isPlaying = false;
        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
        filtersContainerPanel.setEnabled(true);
        playbackTimer.stop();
    }

    private void playNextStep() {
        int currentRow = logsTable.getSelectedRow();
        int nextRow = currentRow + 1;
        if (nextRow < tableModel.getRowCount()) {
            selectRow(nextRow);
            timelineSlider.setValue((int) (((double) nextRow / (tableModel.getRowCount() - 1)) * 100));
        } else {
            stopPlayback();
        }
    }

    private void selectRow(int row) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            logsTable.setRowSelectionInterval(row, row);
            logsTable.scrollRectToVisible(logsTable.getCellRect(row, 0, true));
        }
    }

    private void setupSliderListeners() {
        // 1. Click pe slider pentru a sari direct (Jump to click)
        timelineSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!timelineSlider.isEnabled()) return;
                JSlider slider = (JSlider) e.getSource();
                int width = slider.getWidth();
                int mouseX = e.getX();
                int value = (int) Math.round(((double) mouseX / width) * slider.getMaximum());
                slider.setValue(value);
            }
        });

        // 2. Hover Tooltip (ramane pentru info rapid fara click)
        timelineSlider.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                if (visibleEvents.isEmpty()) return;

                // Calculam timpul sub mouse (fara sa mutam sliderul)
                double mousePercent = (double) e.getX() / timelineSlider.getWidth();
                mousePercent = Math.max(0, Math.min(1, mousePercent));
                int targetIndex = (int) (mousePercent * (visibleEvents.size() - 1));

                long timestamp = visibleEvents.get(targetIndex).getTimestamp();
                lblCurrentTime.setToolTipText("Seek to: " + formatTime(timestamp));
                timelineSlider.setToolTipText("Seek to: " + formatTime(timestamp));
            }
        });

        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setReshowDelay(0);

        // 3. Sincronizare Slider -> Tabel si Label (DRAG & PLAY)
        timelineSlider.addChangeListener(e -> {
            // Executam mereu cand se schimba valoarea (inclusiv in timpul drag-ului)
            if (!visibleEvents.isEmpty()) {
                int percent = timelineSlider.getValue();
                int targetRow = (int) ((percent / 100.0) * (visibleEvents.size() - 1));

                // A. Actualizam Label-ul de Timp (INSTANT)
                long currentTimestamp = visibleEvents.get(targetRow).getTimestamp();
                lblCurrentTime.setText(formatTime(currentTimestamp));

                // B. Actualizam Tabelul (doar daca nu e in modul Play automat, ca sa nu facem conflict)
                if (!isPlaying) {
                    selectRow(targetRow);
                }
            }
        });
    }

    // Helper mic pentru formatare (sa nu scriem SimpleDateFormat peste tot)
    private String formatTime(long timestamp) {
        return new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(timestamp));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LogPlayerWindow().setVisible(true));
    }
}