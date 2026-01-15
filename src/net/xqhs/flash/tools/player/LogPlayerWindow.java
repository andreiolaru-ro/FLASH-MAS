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

    // Componente GUI
    private JTable logsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JSlider timelineSlider; // Am schimbat in JSlider pentru interactiune
    private JButton btnPlay, btnPause;
    private JPanel dynamicFilterPanel; // Panoul unde adaugam agentii dinamic

    // Date
    private List<SimulationEvent> allEvents;     // Toate evenimentele incarcate
    private List<SimulationEvent> visibleEvents; // Evenimentele care trec de filtru

    // State
    private Timer playbackTimer;
    private boolean isPlaying = false;
    private Set<String> knownAgents = new HashSet<>();
    private List<JCheckBox> agentCheckboxes = new ArrayList<>();

    // Filtre Statice
    private JCheckBox chkShowSystem;
    private JCheckBox chkShowLogs;

    public LogPlayerWindow() {
        super("FLASH-MAS Log Player & Analyzer");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initializare Date
        allEvents = new ArrayList<>();
        visibleEvents = new ArrayList<>();

        // --- 1. TOOLBAR (NORD) ---
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnLoad = new JButton("Open JSON");
        btnPlay = new JButton("▶ Play");
        btnPause = new JButton("⏸ Pause");
        btnPause.setEnabled(false); // Dezactivat initial

        // Slider care merge de la 0 la 100% din durata simularii
        timelineSlider = new JSlider(0, 100, 0);
        timelineSlider.setPreferredSize(new Dimension(600, 20));
        timelineSlider.setEnabled(false);

        // ... timelineSlider setup existent ...

        // [FIX 3] Facem slider-ul sa sara exact unde dai click
        timelineSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                JSlider slider = (JSlider) e.getSource();

                // Calculam valoarea bazata pe pozitia X a mouse-ului
                int width = slider.getWidth();
                int mouseX = e.getX();

                // Regula de trei simpla: Pozitia X / Latime Totala * Valoare Maxima
                int value = (int) Math.round(((double) mouseX / width) * slider.getMaximum());

                // Setam valoarea
                slider.setValue(value);
            }
        });

        // Actiuni Butoane
        btnLoad.addActionListener(e -> chooseFile());
        btnPlay.addActionListener(e -> startPlayback());
        btnPause.addActionListener(e -> stopPlayback());

        // Slider Listener (Cand trage utilizatorul de el)
        timelineSlider.addChangeListener(e -> {
            if (!timelineSlider.getValueIsAdjusting() && !isPlaying && !visibleEvents.isEmpty()) {
                int percent = timelineSlider.getValue();
                int targetRow = (int) ((percent / 100.0) * (visibleEvents.size() - 1));
                selectRow(targetRow);
            }
        });

        // [NOU] Hover Listener pentru efectul YouTube
        timelineSlider.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                // 1. Verificam daca avem date
                if (visibleEvents.isEmpty()) return;

                // 2. Calculam procentul bazat pe pozitia mouse-ului (X) in slider
                // e.getX() ne da pixelul curent, getWidth() lungimea totala
                double mousePercent = (double) e.getX() / timelineSlider.getWidth();

                // Ne asiguram ca nu iesim din limite (0.0 - 1.0)
                mousePercent = Math.max(0, Math.min(1, mousePercent));

                // 3. Gasim indexul evenimentului corespunzator acestui procent
                int targetIndex = (int) (mousePercent * (visibleEvents.size() - 1));

                // 4. Luam timestamp-ul si il formatam
                long timestamp = visibleEvents.get(targetIndex).timestamp;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
                String timeText = sdf.format(new java.util.Date(timestamp));

                // 5. Setam tooltip-ul care apare instant
                timelineSlider.setToolTipText("Jump to: " + timeText);
            }
        });

        // [NOU] Hacks pentru ca Tooltip-ul sa apara instantaneu si sa urmareasca mouse-ul
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setReshowDelay(0);

        toolbarPanel.add(btnLoad);
        toolbarPanel.add(new JSeparator(SwingConstants.VERTICAL));
        toolbarPanel.add(btnPlay);
        toolbarPanel.add(btnPause);
        toolbarPanel.add(timelineSlider);

        add(toolbarPanel, BorderLayout.NORTH);

        // --- 2. FILTRE (VEST) ---
        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setPreferredSize(new Dimension(250, 0));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Filtre Statice
        JPanel staticFilters = new JPanel();
        staticFilters.setLayout(new BoxLayout(staticFilters, BoxLayout.Y_AXIS));
        staticFilters.setBorder(BorderFactory.createTitledBorder("Event Types"));

        chkShowSystem = new JCheckBox("Show System Events", true);
        chkShowLogs = new JCheckBox("Show Logs / Messages", true);

        chkShowSystem.addActionListener(e -> applyFilters());
        chkShowLogs.addActionListener(e -> applyFilters());

        staticFilters.add(chkShowSystem);
        staticFilters.add(chkShowLogs);

        // Filtre Dinamice (Agenti) - Scrollabil
        dynamicFilterPanel = new JPanel();
        dynamicFilterPanel.setLayout(new BoxLayout(dynamicFilterPanel, BoxLayout.Y_AXIS));

        JScrollPane agentScrollPane = new JScrollPane(dynamicFilterPanel);
        agentScrollPane.setBorder(BorderFactory.createTitledBorder("Filter Agents"));

        sidebarPanel.add(staticFilters, BorderLayout.NORTH);
        sidebarPanel.add(agentScrollPane, BorderLayout.CENTER);

        add(sidebarPanel, BorderLayout.WEST);

        // --- 3. TABEL (CENTRU) ---
        String[] columnNames = {"Time", "Timestamp", "Agent/Source", "Type", "Payload"};

        // [FIX] Facem tabelul read-only suprascriind isCellEditable
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Nicio celula nu poate fi editata
            }
        };

        logsTable = new JTable(tableModel);
        // Setam latimea coloanelor pentru aspect
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Time
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Timestamp
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Source
        logsTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Type
        logsTable.getColumnModel().getColumn(4).setPreferredWidth(400); // Payload

        JScrollPane tableScroll = new JScrollPane(logsTable);
        add(tableScroll, BorderLayout.CENTER);

        // --- 4. STATUS (SUD) ---
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready. Please load a file.");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // Timer-ul pentru redare (update la fiecare 500ms)
        playbackTimer = new Timer(100, e -> playNextStep());

        setLocationRelativeTo(null);
    }

    // --- LOGICA DE INCARCARE ---

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser(".");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadData(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadData(String filePath) {
        statusLabel.setText("Loading...");
        stopPlayback(); // Oprim daca rula ceva

        new Thread(() -> {
            try {
                allEvents = LogLoader.loadFromFile(filePath);

                // Extragem lista unica de agenti
                knownAgents.clear();
                for (SimulationEvent evt : allEvents) {
                    if (evt.entityName != null && !evt.entityName.isEmpty()) {
                        knownAgents.add(evt.entityName);
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    // Reconstruim UI-ul de filtre
                    rebuildAgentFilters();
                    // Aplicam filtrele (care va popula si tabelul)
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
            JCheckBox chk = new JCheckBox(agentName, true); // Selectat by default
            chk.addActionListener(e -> applyFilters());
            dynamicFilterPanel.add(chk);
            agentCheckboxes.add(chk);
        }

        dynamicFilterPanel.revalidate();
        dynamicFilterPanel.repaint();
    }

    // --- LOGICA DE FILTRARE ---

    private void applyFilters() {
        // 1. Aflam ce agenti sunt selectati
        Set<String> selectedAgents = agentCheckboxes.stream()
                .filter(JCheckBox::isSelected)
                .map(JCheckBox::getText)
                .collect(Collectors.toSet());

        boolean showSys = chkShowSystem.isSelected();
        boolean showLog = chkShowLogs.isSelected();

        // 2. Filtram lista originala
        visibleEvents = allEvents.stream().filter(evt -> {
            // Filtru Agent
            boolean isAgentSelected = selectedAgents.contains(evt.entityName);

            // Filtru Tip (Hardcoded logic simplificat pentru demo)
            boolean isSystemEvent = evt.type.contains("START") || evt.type.contains("STOP");

            // Logica combinata
            if (!isAgentSelected) return false; // Daca agentul nu e bifat, dispare

            if (isSystemEvent && !showSys) return false;
            if (!isSystemEvent && !showLog) return false;

            return true;
        }).collect(Collectors.toList());

        // 3. Actualizam tabelul
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

    // --- LOGICA DE PLAYER ---

    private void startPlayback() {
        // 1. Validari de baza
        if (visibleEvents.isEmpty()) return;

        // 2. [NOU] Logica de Replay inteligent
        // Verificam unde ne aflam acum
        int currentRow = logsTable.getSelectedRow();
        int totalRows = tableModel.getRowCount();

        // Daca nu e nimic selectat (-1) SAU suntem la ultimul rand (final),
        // resetam pozitia la inceput (0) inainte sa pornim.
        if (currentRow == -1 || currentRow >= totalRows - 1) {
            selectRow(0);
            timelineSlider.setValue(0);
        }

        // 3. Pornim efectiv redarea
        isPlaying = true;
        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);
        dynamicFilterPanel.setEnabled(false); // Blocam filtrele in timpul redarii

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
        int totalRows = tableModel.getRowCount(); // Numarul total de randuri

        if (nextRow < totalRows) {
            selectRow(nextRow);

            // [FIX 1] Calculam procentul corect raportat la ultimul index posibil (total - 1)
            // Daca nextRow este ultimul, fractia va fi 1.0 -> 100%
            if (totalRows > 1) {
                int progress = (int) (((double) nextRow / (totalRows - 1)) * 100);
                timelineSlider.setValue(progress);
            }
        } else {
            stopPlayback();
            timelineSlider.setValue(100); // Fortam 100% la final
        }
    }

    private void selectRow(int row) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            // Selectam randul (ca sa fie albastru)
            logsTable.setRowSelectionInterval(row, row);

            // [MODIFICARE] Folosim functia noastra de centrare in loc de scrollRectToVisible
            scrollToCenter(row);
        }
    }

    /**
     * Centreaza tabelul vizual pe rândul specificat.
     * Mult mai placut ochiului decat scrollRectToVisible standard.
     */
    /**
     * Varianta Matematica Pura (fara salturi).
     * Calculeaza pozitia ideala si o constrange intre limitele fizice ale ferestrei.
     */
    private void scrollToCenter(int rowIndex) {
        // 1. Validari de siguranta
        if (!(logsTable.getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport) logsTable.getParent();

        // Obtinem coordonatele rectangulare ale randului tinta
        Rectangle rect = logsTable.getCellRect(rowIndex, 0, true);

        // Dimensiunile ferestrei vizibile si ale tabelului total
        int viewHeight = viewport.getHeight();
        int tableHeight = logsTable.getHeight();

        // 2. Cazul in care tabelul e mai mic decat fereastra (ex: ai doar 5 loguri)
        // Nu are sens sa facem scroll, fortam pozitia 0 (sus).
        if (tableHeight <= viewHeight) {
            viewport.setViewPosition(new Point(0, 0));
            return;
        }

        // 3. Calculam pozitia ideala Y pentru a centra randul
        // Formula: (Centrul Randului) - (Jumatate din Inaltimea Ferestrei)
        int rowCenterY = rect.y + (rect.height / 2);
        int halfViewHeight = viewHeight / 2;
        int targetY = rowCenterY - halfViewHeight;

        // 4. "Clamping" - Fizica scroll-ului
        // Nu avem voie sa fim mai mici ca 0 (mai sus de inceput)
        // Nu avem voie sa fim mai mari ca (InaltimeTabel - InaltimeFereastra) (mai jos de final)

        int maxY = tableHeight - viewHeight; // Maximul cat putem scrola in jos

        if (targetY < 0) {
            targetY = 0;
        }
        if (targetY > maxY) {
            targetY = maxY;
        }

        // 5. Executam mutarea fina
        viewport.setViewPosition(new Point(0, targetY));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LogPlayerWindow().setVisible(true));
    }
}