package com.pradeep.dsaviz;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public final class VisualizerFrame extends JFrame {
    // -------- Custom colour palette --------
    private static final Color BAR_TOP = new Color(100, 180, 255);
    private static final Color BAR_BOTTOM = new Color(0, 50, 150);
    private static final Color ACTIVE = new Color(255, 165, 0);
    private static final Color FOUND = new Color(0, 200, 0);
    private static final Color PIVOT = new Color(255, 255, 0);
    private static final Color HEADER_START = new Color(0, 100, 255);
    private static final Color HEADER_MID = new Color(255, 165, 0);
    private static final Color HEADER_END = new Color(255, 255, 0);
    private static final Color BG_WHITE = new Color(255, 255, 255);
    private static final Color BG_LIGHT_BLUE = new Color(200, 220, 255);
    private static final Color BG_LIGHT_GREEN = new Color(200, 255, 200);
    private static final Color BG_LIGHT_YELLOW = new Color(255, 255, 200);
    private static final Color BG_LIGHT_ORANGE = new Color(255, 220, 180);

    // -------- Algorithm names --------
    private static final String[] SORTING_ALGORITHMS = {
        "Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort", "Heap Sort"
    };
    private static final String[] SEARCHING_ALGORITHMS = {
        "Linear Search", "Binary Search"
    };

    // -------- UI Components --------
    private final Random random = new Random();
    private final BarPanel bars = new BarPanel();
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"Sorting", "Searching"});
    private final JComboBox<String> algorithm = new JComboBox<>(SORTING_ALGORITHMS);
    private final JSlider size = new JSlider(10, 60, 30);
    private final JSlider speed = new JSlider(10, 1000, 300);
    private final JLabel targetLabel = new JLabel("Target");
    private final JTextField target = new JTextField(5);
    private final JTextField customSize = new JTextField("5", 4);
    private final JTextField customElements = new JTextField(30);
    private final JLabel info = new JLabel();
    private final JButton run = new JButton("▶ Visualize");
    private final JButton stop = new JButton("⏹ Stop");
    private final JButton pause = new JButton("⏸ Pause");
    private final JButton reset = new JButton("⟳ New Array");
    private final JButton useCustomArray = new JButton("📥 Use Custom Array");

    // -------- State --------
    private int[] values;
    private int first = -1;
    private int second = -1;
    private int result = -1;
    private int pivotIndex = -1;
    private MergeState mergeState = new MergeState();
    private int heapSize = 0;
    private volatile boolean stopRequested;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    public VisualizerFrame() {
        super("DSA Visualizer");
        setModernLookAndFeel();
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(ACTIVE));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(Color.BLACK));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 720));
        setLocationByPlatform(true);
        setContentPane(createGradientPanel());
        buildUi();
        updateAlgorithmOptions();
        generateArray();
        updateInfo();
        showTargetField(isSearching()); // ensure target is hidden initially
    }

    // -------- Background panel with multi‑colour gradient --------
    private JPanel createGradientPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                float[] fractions = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
                Color[] colors = {BG_WHITE, BG_LIGHT_BLUE, BG_LIGHT_GREEN, BG_LIGHT_YELLOW, BG_LIGHT_ORANGE};
                LinearGradientPaint gp = new LinearGradientPaint(0, 0, getWidth(), getHeight(), fractions, colors);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
    }

    private void setModernLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            UIManager.put("control", new ColorUIResource(Color.WHITE));
            UIManager.put("nimbusBase", new ColorUIResource(ACTIVE));
            UIManager.put("nimbusBlueGrey", new ColorUIResource(ACTIVE));
            UIManager.put("nimbusLightBackground", new ColorUIResource(Color.WHITE));
            UIManager.put("text", new ColorUIResource(Color.BLACK));
            UIManager.put("nimbusSelectionBackground", new ColorUIResource(ACTIVE));
            UIManager.put("nimbusSelectionForeground", new ColorUIResource(Color.BLACK));
            UIManager.put("info", new ColorUIResource(Color.WHITE));
            UIManager.put("Button.background", new ColorUIResource(new Color(200, 230, 255)));
            UIManager.put("Button.foreground", new ColorUIResource(Color.BLACK));
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 14));
            UIManager.put("Slider.background", new ColorUIResource(Color.WHITE));
            UIManager.put("Slider.foreground", new ColorUIResource(ACTIVE));
            UIManager.put("Slider.trackColor", new ColorUIResource(ACTIVE));
            UIManager.put("Slider.thumbColor", new ColorUIResource(ACTIVE));
            UIManager.put("Slider.thumbHeight", 16);
            UIManager.put("Slider.thumbWidth", 16);
            UIManager.put("Slider.trackWidth", 6);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) { /* ignore */ }
        }
    }

    // -------- UI Construction --------
    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(root, BorderLayout.CENTER);

        // Header
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                float[] fractions = {0.0f, 0.5f, 1.0f};
                Color[] colors = {HEADER_START, HEADER_MID, HEADER_END};
                LinearGradientPaint gp = new LinearGradientPaint(0, 0, getWidth(), 0, fractions, colors);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        header.setLayout(new BorderLayout(8, 4));
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        header.setOpaque(false);

        JLabel title = new JLabel("DSA Visualizer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Interactive learning for Sorting & Searching algorithms");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(new Color(230, 240, 255));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Bar Panel
        bars.setOpaque(false);
        bars.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0, 0, 0, 50), 2, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        root.add(bars, BorderLayout.CENTER);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        controls.add(createStyledLabel("Mode"));
        controls.add(styleCombo(mode));
        controls.add(createStyledLabel("Algorithm"));
        controls.add(styleCombo(algorithm));
        controls.add(createStyledLabel("Array size"));
        controls.add(styleSlider(size));
        controls.add(createStyledLabel("Delay (ms)"));
        controls.add(styleSlider(speed));
        controls.add(targetLabel);
        controls.add(styleTextField(target));
        targetLabel.setForeground(Color.BLACK);
        targetLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        controls.add(styleButton(reset));
        controls.add(styleButton(run));
        controls.add(styleButton(pause));
        controls.add(styleButton(stop));
        stop.setEnabled(false);
        pause.setEnabled(false);

        // Custom input
        JPanel customInput = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        customInput.setOpaque(false);
        customInput.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        customInput.add(createStyledLabel("Custom size"));
        customInput.add(styleTextField(customSize));
        customInput.add(createStyledLabel("Elements (comma-separated)"));
        customInput.add(styleTextField(customElements));
        customInput.add(styleButton(useCustomArray));

        // Info Panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(true);
        infoPanel.setBackground(new Color(255, 255, 255, 200));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0, 0, 0, 30), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        info.setForeground(Color.BLACK);
        info.setFont(new Font("Segoe UI", Font.BOLD, 16));
        infoPanel.add(info, BorderLayout.WEST);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        bottom.add(controls, BorderLayout.NORTH);
        bottom.add(customInput, BorderLayout.CENTER);
        bottom.add(infoPanel, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        // Listeners
        mode.addActionListener(e -> {
            updateAlgorithmOptions();
            showTargetField(isSearching());
        });
        algorithm.addActionListener(e -> {
            updateInfo();
            bars.repaint();
        });
        reset.addActionListener(e -> generateArray());
        useCustomArray.addActionListener(e -> loadCustomArray());
        run.addActionListener(e -> start());
        stop.addActionListener(e -> stopAnimation());
        pause.addActionListener(e -> togglePause());

        showTargetField(isSearching());
    }

    // -------- Styling helpers --------
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.BLACK);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
    }

    private JComboBox<String> styleCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        combo.setBackground(Color.WHITE);
        combo.setForeground(Color.BLACK);
        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0, 0, 0, 50), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(ACTIVE);
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(Color.BLACK);
                }
                label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                return label;
            }
        });
        combo.setLightWeightPopupEnabled(true);
        return combo;
    }

    private JButton styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.BLACK);
        button.setBackground(new Color(200, 230, 255));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(100, 150, 200), 2, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    private JSlider styleSlider(JSlider slider) {
        slider.setBackground(Color.WHITE);
        slider.setForeground(ACTIVE);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
        slider.setPreferredSize(new Dimension(120, 30));
        return slider;
    }

    private JTextField styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.BOLD, 13));
        field.setBackground(Color.WHITE);
        field.setForeground(Color.BLACK);
        field.setCaretColor(Color.BLACK);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0, 0, 0, 50), 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return field;
    }

    // -------- Target field: hide completely when not searching --------
    private void showTargetField(boolean show) {
        targetLabel.setVisible(show);
        target.setVisible(show);
        if (show) {
            targetLabel.setPreferredSize(null);
            target.setPreferredSize(null);
        } else {
            // Remove the space completely
            targetLabel.setPreferredSize(new Dimension(0, 0));
            target.setPreferredSize(new Dimension(0, 0));
        }
        revalidate();
        repaint();
    }

    // -------- Pause/Resume --------
    private void togglePause() {
        synchronized (pauseLock) {
            paused = !paused;
            pause.setText(paused ? "▶ Resume" : "⏸ Pause");
            if (!paused) pauseLock.notifyAll();
        }
    }

    // -------- Update Info (black + bold) --------
    private void updateInfo() {
        String selected = (String) algorithm.getSelectedItem();
        String complexity = switch (selected) {
            case "Bubble Sort" -> "O(n²) · O(1)";
            case "Selection Sort" -> "O(n²) · O(1)";
            case "Insertion Sort" -> "O(n²) · O(1)";
            case "Merge Sort" -> "O(n log n) · O(n)";
            case "Quick Sort" -> "O(n log n) · O(log n)";
            case "Heap Sort" -> "O(n log n) · O(1)";
            case "Linear Search" -> "O(n) · O(1)";
            case "Binary Search" -> "O(log n) · O(1)";
            default -> "";
        };
        info.setText("<html><span style='color: #0066cc; font-size: 18px; font-weight: bold;'>⚡ " + selected +
                     "</span> &nbsp;|&nbsp; <span style='color: #cc6600; font-weight: bold;'>Time: <b>" + complexity +
                     "</b></span> &nbsp;|&nbsp; <span style='color: #009900; font-weight: bold;'>✨ Ready</span></html>");
    }

    // -------- The rest of the methods (algorithm logic, merge state, bar painting) --------
    // (Identical to previous version – omitted here for brevity, but included in the final code.)

    private void updateAlgorithmOptions() {
        String[] options = isSearching() ? SEARCHING_ALGORITHMS : SORTING_ALGORITHMS;
        algorithm.setModel(new DefaultComboBoxModel<>(options));
        target.setEnabled(isSearching());
        updateInfo();
    }

    private boolean isSearching() {
        return "Searching".equals(mode.getSelectedItem());
    }

    private void generateArray() {
        values = new int[size.getValue()];
        for (int i = 0; i < values.length; i++) {
            values[i] = 10 + random.nextInt(390);
        }
        customSize.setText(String.valueOf(values.length));
        int suggestedTarget = values[random.nextInt(values.length)];
        target.setText(String.valueOf(suggestedTarget));
        first = second = result = -1;
        pivotIndex = -1;
        mergeState.reset();
        heapSize = values.length;
        bars.repaint();
        info.setText("New array generated. Search target set to " + suggestedTarget + ".");
    }

    private void loadCustomArray() {
        String sizeText = customSize.getText().trim();
        String elementsText = customElements.getText().trim();
        if (!sizeText.matches("\\d+") || elementsText.isEmpty()) {
            info.setText("Enter an array size and comma-separated positive whole numbers.");
            return;
        }
        int requestedSize;
        try {
            requestedSize = Integer.parseInt(sizeText);
        } catch (NumberFormatException e) {
            info.setText("Array size is too large.");
            return;
        }
        if (requestedSize < 1 || requestedSize > 60) {
            info.setText("Custom array size must be between 1 and 60 for clear visualisation.");
            return;
        }
        String[] parts = elementsText.split("\\s*,\\s*");
        if (parts.length != requestedSize) {
            info.setText("Size is " + requestedSize + ", but " + parts.length + " elements were provided.");
            return;
        }
        int[] customValues = new int[requestedSize];
        try {
            for (int i = 0; i < parts.length; i++) {
                customValues[i] = Integer.parseInt(parts[i]);
                if (customValues[i] < 1) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            info.setText("Elements must be positive whole numbers, separated by commas.");
            return;
        }
        values = customValues;
        size.setValue(Math.min(requestedSize, size.getMaximum()));
        target.setText(String.valueOf(values[random.nextInt(values.length)]));
        first = second = result = -1;
        pivotIndex = -1;
        mergeState.reset();
        heapSize = values.length;
        bars.repaint();
        info.setText("Custom array loaded. A valid search target has been selected automatically.");
    }

    private void start() {
        String selected = (String) algorithm.getSelectedItem();
        String targetText = target.getText().trim();
        if (isSearching() && !targetText.matches("\\d+")) {
            info.setText("Enter a whole-number target, or click New Array to use a suggested value.");
            return;
        }
        setControlsEnabled(false);
        stopRequested = false;
        paused = false;
        pause.setText("⏸ Pause");
        pause.setEnabled(true);
        stop.setEnabled(true);
        result = first = second = -1;
        pivotIndex = -1;
        mergeState.reset();
        heapSize = values.length;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    switch (selected) {
                        case "Bubble Sort" -> bubbleSort();
                        case "Selection Sort" -> selectionSort();
                        case "Insertion Sort" -> insertionSort();
                        case "Merge Sort" -> {
                            mergeState.reset();
                            mergeSort(0, values.length - 1, 0);
                            clearHighlight();
                        }
                        case "Quick Sort" -> {
                            quickSort(0, values.length - 1);
                            clearHighlight();
                        }
                        case "Heap Sort" -> {
                            heapSize = values.length;
                            heapSort();
                            clearHighlight();
                        }
                        case "Linear Search" -> linearSearch(Integer.parseInt(targetText));
                        case "Binary Search" -> binarySearch(Integer.parseInt(targetText));
                        default -> throw new IllegalStateException("Unknown algorithm");
                    }
                } catch (AnimationStoppedException ignored) {
                }
                return null;
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                pause.setEnabled(false);
                stop.setEnabled(false);
                if (stopRequested) {
                    info.setText("Animation stopped. You can choose another algorithm or generate a new array.");
                    return;
                }
                info.setText(isSearching()
                        ? (result >= 0 ? "Target found at index " + result + "." : "Target not found.")
                        : selected + " completed successfully.");
            }
        }.execute();
    }

    private void stopAnimation() {
        stopRequested = true;
        synchronized (pauseLock) {
            if (paused) {
                paused = false;
                pauseLock.notifyAll();
            }
        }
        stop.setEnabled(false);
        pause.setEnabled(false);
        info.setText("Stopping animation...");
    }

    // -------- Sorting Algorithms --------
    private void bubbleSort() {
        for (int end = values.length - 1; end > 0; end--) {
            for (int i = 0; i < end; i++) {
                highlight(i, i + 1);
                if (values[i] > values[i + 1]) swap(i, i + 1);
            }
        }
        clearHighlight();
    }

    private void selectionSort() {
        for (int start = 0; start < values.length - 1; start++) {
            int min = start;
            for (int i = start + 1; i < values.length; i++) {
                highlight(min, i);
                if (values[i] < values[min]) min = i;
            }
            swap(start, min);
        }
        clearHighlight();
    }

    private void insertionSort() {
        for (int i = 1; i < values.length; i++) {
            int current = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > current) {
                values[j + 1] = values[j];
                highlight(j, j + 1);
                j--;
            }
            values[j + 1] = current;
            highlight(j + 1, i);
        }
        clearHighlight();
    }

    private void mergeSort(int left, int right, int depth) {
        if (left >= right) return;
        int middle = left + (right - left) / 2;
        mergeState.addSplit(left, middle, right, depth);
        repaintAndPause();
        mergeSort(left, middle, depth + 1);
        mergeSort(middle + 1, right, depth + 1);
        merge(left, middle, right);
    }

    private void merge(int left, int middle, int right) {
        int[] leftArr = Arrays.copyOfRange(values, left, middle + 1);
        int[] rightArr = Arrays.copyOfRange(values, middle + 1, right + 1);
        int i = 0, j = 0, k = left;

        mergeState.setMerging(left, middle, right, leftArr, rightArr);
        while (i < leftArr.length && j < rightArr.length) {
            if (stopRequested) throw new AnimationStoppedException();
            int chosen;
            if (leftArr[i] <= rightArr[j]) {
                chosen = leftArr[i++];
                mergeState.leftCursor = i;
            } else {
                chosen = rightArr[j++];
                mergeState.rightCursor = j;
            }
            values[k] = chosen;
            mergeState.outputArr[k - left] = chosen;
            mergeState.outputCount = k - left + 1;
            highlight(k, -1);
            k++;
        }
        while (i < leftArr.length) {
            values[k] = leftArr[i++];
            mergeState.leftCursor = i;
            mergeState.outputArr[k - left] = values[k];
            mergeState.outputCount = k - left + 1;
            highlight(k, -1);
            k++;
        }
        while (j < rightArr.length) {
            values[k] = rightArr[j++];
            mergeState.rightCursor = j;
            mergeState.outputArr[k - left] = values[k];
            mergeState.outputCount = k - left + 1;
            highlight(k, -1);
            k++;
        }
        mergeState.clearMerging();
        repaintAndPause();
    }

    private void quickSort(int low, int high) {
        if (low >= high) return;
        int pivot = partition(low, high);
        pivotIndex = -1;
        quickSort(low, pivot - 1);
        quickSort(pivot + 1, high);
    }

    private int partition(int low, int high) {
        int pivotValue = values[high];
        pivotIndex = high;
        info.setText("Partitioning: pivot = " + pivotValue + " at index " + high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            highlight(j, high);
            if (values[j] <= pivotValue) swap(++i, j);
        }
        swap(i + 1, high);
        pivotIndex = -1;
        return i + 1;
    }

    private void heapSort() {
        for (int i = values.length / 2 - 1; i >= 0; i--) {
            heapify(values.length, i);
        }
        for (int i = values.length - 1; i > 0; i--) {
            swap(0, i);
            heapSize = i;
            heapify(i, 0);
        }
        heapSize = 0;
        clearHighlight();
    }

    private void heapify(int n, int i) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        if (left < n) {
            highlight(i, left);
            if (values[left] > values[largest]) largest = left;
        }
        if (right < n) {
            highlight(i, right);
            if (values[right] > values[largest]) largest = right;
        }
        if (largest != i) {
            swap(i, largest);
            heapify(n, largest);
        }
    }

    private void linearSearch(int value) {
        for (int i = 0; i < values.length; i++) {
            highlight(i, -1);
            if (values[i] == value) {
                result = i;
                break;
            }
        }
        finishSearch();
    }

    private void binarySearch(int value) {
        Arrays.sort(values);
        repaintAndPause();
        int low = 0, high = values.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            highlight(low, mid);
            if (values[mid] == value) {
                result = mid;
                break;
            }
            if (values[mid] < value) low = mid + 1;
            else high = mid - 1;
        }
        finishSearch();
    }

    private void finishSearch() {
        first = second = -1;
        repaintAndPause();
    }

    // -------- Helpers --------
    private void swap(int i, int j) {
        int tmp = values[i];
        values[i] = values[j];
        values[j] = tmp;
        repaintAndPause();
    }

    private void highlight(int i, int j) {
        first = i;
        second = j;
        repaintAndPause();
    }

    private void clearHighlight() {
        first = second = -1;
        pivotIndex = -1;
        mergeState.clearMerging();
        repaintAndPause();
    }

    private void repaintAndPause() {
        if (stopRequested) throw new AnimationStoppedException();
        synchronized (pauseLock) {
            while (paused && !stopRequested) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AnimationStoppedException();
                }
            }
        }
        if (stopRequested) throw new AnimationStoppedException();
        bars.repaint();
        try {
            Thread.sleep(speed.getValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnimationStoppedException();
        }
        if (stopRequested) throw new AnimationStoppedException();
    }

    private void setControlsEnabled(boolean enabled) {
        mode.setEnabled(enabled);
        algorithm.setEnabled(enabled);
        size.setEnabled(enabled);
        speed.setEnabled(true);
        target.setEnabled(enabled && isSearching());
        customSize.setEnabled(enabled);
        customElements.setEnabled(enabled);
        useCustomArray.setEnabled(enabled);
        run.setEnabled(enabled);
        reset.setEnabled(enabled);
        if (enabled) {
            pause.setEnabled(false);
            stop.setEnabled(false);
        }
    }

    private static final class AnimationStoppedException extends RuntimeException {
    }

    // -------- Merge State --------
    private static final class MergeState {
        List<Split> splits = new ArrayList<>();
        int maxDepth = 0;

        boolean isMerging = false;
        int mergeLeft, mergeMiddle, mergeRight;
        int[] leftArr, rightArr, outputArr;
        int outputCount = 0;
        int leftCursor = 0, rightCursor = 0;

        void addSplit(int left, int middle, int right, int depth) {
            splits.add(new Split(left, middle, right, depth));
            maxDepth = Math.max(maxDepth, depth);
        }

        void setMerging(int left, int middle, int right, int[] leftArr, int[] rightArr) {
            this.isMerging = true;
            this.mergeLeft = left;
            this.mergeMiddle = middle;
            this.mergeRight = right;
            this.leftArr = leftArr;
            this.rightArr = rightArr;
            this.outputArr = new int[right - left + 1];
            this.outputCount = 0;
            this.leftCursor = 0;
            this.rightCursor = 0;
        }

        void clearMerging() {
            this.isMerging = false;
            this.leftArr = this.rightArr = this.outputArr = null;
        }

        void reset() {
            splits.clear();
            maxDepth = 0;
            clearMerging();
        }

        record Split(int left, int middle, int right, int depth) {
        }
    }

    // -------- BarPanel (custom painting) --------
    private final class BarPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (values == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            String selected = (String) algorithm.getSelectedItem();
            if ("Merge Sort".equals(selected)) {
                drawMergeView(g2);
            } else if ("Heap Sort".equals(selected)) {
                drawHeapView(g2);
            } else {
                drawBarView(g2, selected);
            }
            g2.dispose();
        }

        // ----- Bar view: vertical gradient + numbers in BLACK -----
        private void drawBarView(Graphics2D g2, String selectedAlgorithm) {
            int gap = Math.max(1, getWidth() / (values.length * 12));
            int barWidth = Math.max(2, (getWidth() - gap * (values.length + 1)) / values.length);
            int maxVal = Arrays.stream(values).max().orElse(1);

            for (int i = 0; i < values.length; i++) {
                int height = (int) ((values[i] / (double) maxVal) * (getHeight() - 40));
                int x = gap + i * (barWidth + gap);
                int y = getHeight() - height - 12;

                Color topColor, bottomColor;
                if (i == result) {
                    topColor = FOUND;
                    bottomColor = FOUND.darker();
                } else if (i == pivotIndex && "Quick Sort".equals(selectedAlgorithm)) {
                    topColor = PIVOT;
                    bottomColor = PIVOT.darker();
                } else if (i == first || i == second) {
                    topColor = ACTIVE;
                    bottomColor = ACTIVE.darker();
                } else {
                    topColor = BAR_TOP;
                    bottomColor = BAR_BOTTOM;
                }

                GradientPaint gp = new GradientPaint(x, y, topColor, x, y + height, bottomColor);
                g2.setPaint(gp);

                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(x + 2, y + 2, barWidth, height, 8, 8);

                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barWidth, height, 8, 8);

                if (i == first || i == second || i == pivotIndex || i == result) {
                    g2.setColor(new Color(255, 255, 255, 180));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x, y, barWidth, height, 8, 8);
                }

                if (barWidth >= 18) {
                    g2.setColor(Color.BLACK);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
                    String val = String.valueOf(values[i]);
                    int tw = g2.getFontMetrics().stringWidth(val);
                    g2.drawString(val, x + (barWidth - tw) / 2, Math.max(14, y - 4));
                }
            }
        }

        // ----- Merge Sort View -----
        private void drawMergeView(Graphics2D g2) {
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
            g2.drawString("Merge Sort – Divide & Conquer", 24, 40);
            g2.setColor(new Color(80, 80, 80));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            String status = mergeState.isMerging ? "Merging two sorted halves" : "Splitting the array recursively";
            g2.drawString(status, 24, 66);

            int panelWidth = getWidth() - 48;
            int yOffset = 90;
            int cellHeight = 36;
            int gap = 4;

            if (values != null && values.length > 0) {
                drawArrayRow(g2, "Current array", values, 0, values.length - 1, first, second,
                        24, yOffset, panelWidth, cellHeight, gap, BAR_TOP, BAR_BOTTOM, null);
                yOffset += cellHeight + 30;
            }

            if (mergeState.isMerging) {
                int[] left = mergeState.leftArr;
                int[] right = mergeState.rightArr;
                int[] output = mergeState.outputArr;
                int outCount = mergeState.outputCount;

                if (left != null && left.length > 0) {
                    drawArrayRow(g2, "Left half", left, 0, left.length - 1,
                            mergeState.leftCursor - 1, mergeState.leftCursor,
                            24, yOffset, panelWidth / 2 - 10, cellHeight, gap,
                            new Color(100, 180, 255), new Color(0, 50, 150), null);
                }
                if (right != null && right.length > 0) {
                    drawArrayRow(g2, "Right half", right, 0, right.length - 1,
                            mergeState.rightCursor - 1, mergeState.rightCursor,
                            24 + panelWidth / 2 + 10, yOffset, panelWidth / 2 - 10, cellHeight, gap,
                            new Color(100, 180, 255), new Color(0, 50, 150), null);
                }
                yOffset += cellHeight + 30;
                if (output != null && output.length > 0) {
                    drawArrayRow(g2, "Merged output", output, 0, output.length - 1,
                            -1, -1,
                            24, yOffset, panelWidth, cellHeight, gap,
                            FOUND, FOUND.darker(), outCount);
                }
            } else if (!mergeState.splits.isEmpty()) {
                int depth = 0;
                int rowY = yOffset;
                while (true) {
                    final int d = depth;
                    var splitsAtDepth = mergeState.splits.stream()
                            .filter(s -> s.depth() == d)
                            .toList();
                    if (splitsAtDepth.isEmpty()) break;
                    int splitsCount = splitsAtDepth.size();
                    int splitWidth = (panelWidth - gap * (splitsCount - 1)) / splitsCount;
                    int x = 24;
                    for (MergeState.Split sp : splitsAtDepth) {
                        int[] subArr = Arrays.copyOfRange(values, sp.left(), sp.right() + 1);
                        int h1 = -1, h2 = -1;
                        if (first >= sp.left() && first <= sp.right()) h1 = first - sp.left();
                        if (second >= sp.left() && second <= sp.right()) h2 = second - sp.left();
                        drawArrayRow(g2, "", subArr, 0, subArr.length - 1,
                                h1, h2,
                                x, rowY, splitWidth - 4, cellHeight, gap,
                                new Color(100, 180, 255, 180), new Color(0, 50, 150, 180), null);
                        x += splitWidth + gap;
                    }
                    rowY += cellHeight + 20;
                    depth++;
                    if (rowY + cellHeight > getHeight() - 20) break;
                }
            } else {
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18));
                String msg = "Press 'Visualize' to start Merge Sort";
                int tw = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - tw) / 2, getHeight() / 2);
            }
        }

        private void drawArrayRow(Graphics2D g2, String label, int[] arr, int start, int end,
                                  int highlight1, int highlight2,
                                  int x, int y, int width, int cellHeight, int gap,
                                  Color topColor, Color bottomColor, Integer filledCount) {
            if (arr == null || arr.length == 0) {
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                g2.drawString("(empty)", x, y + 14);
                return;
            }
            if (!label.isEmpty()) {
                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
                g2.drawString(label, x, y - 6);
            }
            int count = end - start + 1;
            int cellWidth = Math.max(8, (width - gap * (count - 1)) / count);
            int totalWidth = count * cellWidth + (count - 1) * gap;
            int startX = x + (width - totalWidth) / 2;

            for (int i = 0; i < count; i++) {
                int idx = start + i;
                int cx = startX + i * (cellWidth + gap);
                int cy = y;

                Color useTop = topColor;
                Color useBottom = bottomColor;
                if (i == highlight1 || i == highlight2) {
                    useTop = ACTIVE;
                    useBottom = ACTIVE.darker();
                }
                if (filledCount != null && i >= filledCount) {
                    useTop = new Color(200, 200, 200);
                    useBottom = new Color(150, 150, 150);
                }

                GradientPaint gp = new GradientPaint(cx, cy, useTop, cx, cy + cellHeight, useBottom);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(cx, cy, cellWidth, cellHeight, 6, 6));
                g2.setColor(Color.BLACK);
                g2.draw(new RoundRectangle2D.Double(cx, cy, cellWidth, cellHeight, 6, 6));
                if (cellWidth >= 16) {
                    String val = String.valueOf(arr[i]);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                    int tw = g2.getFontMetrics().stringWidth(val);
                    g2.setColor(Color.BLACK);
                    g2.drawString(val, cx + (cellWidth - tw) / 2, cy + cellHeight - 8);
                }
            }
        }

        // ----- Heap Sort View -----
        private void drawHeapView(Graphics2D g2) {
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
            g2.drawString("Heap Sort – Binary Heap", 24, 40);
            g2.setColor(new Color(80, 80, 80));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            g2.drawString("Building max-heap and extracting elements", 24, 66);

            int levels = 32 - Integer.numberOfLeadingZeros(values.length);
            int radius = Math.max(10, Math.min(28, getWidth() / (1 << Math.min(levels + 1, 8))));
            int levelGap = Math.max(48, (getHeight() - 120) / Math.max(1, levels - 1));

            if (values == null || values.length == 0) {
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18));
                String msg = "Press 'Visualize' to start Heap Sort";
                int tw = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - tw) / 2, getHeight() / 2);
                return;
            }

            g2.setStroke(new BasicStroke(1.5f));
            for (int i = 1; i < values.length; i++) {
                int parent = (i - 1) / 2;
                int px = nodeX(parent, getWidth());
                int py = nodeY(parent, levelGap);
                int cx = nodeX(i, getWidth());
                int cy = nodeY(i, levelGap);
                g2.setColor(new Color(100, 100, 100, 120));
                g2.drawLine(px, py + radius / 2, cx, cy - radius / 2);
            }

            for (int i = 0; i < values.length; i++) {
                int x = nodeX(i, getWidth());
                int y = nodeY(i, levelGap);
                boolean active = (i == first || i == second);

                Color topCol, bottomCol;
                if (i >= heapSize) {
                    topCol = new Color(180, 180, 180);
                    bottomCol = new Color(120, 120, 120);
                } else if (active) {
                    topCol = ACTIVE;
                    bottomCol = ACTIVE.darker();
                } else {
                    topCol = BAR_TOP;
                    bottomCol = BAR_BOTTOM;
                }

                GradientPaint gp = new GradientPaint(x - radius, y - radius, topCol,
                        x + radius, y + radius, bottomCol);
                g2.setPaint(gp);
                g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                g2.setColor(Color.BLACK);
                g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);
                if (radius >= 14) {
                    String val = String.valueOf(values[i]);
                    g2.setColor(Color.BLACK);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
                    int tw = g2.getFontMetrics().stringWidth(val);
                    g2.drawString(val, x - tw / 2, y + 5);
                }
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
                g2.drawString(String.valueOf(i), x - 6, y + radius + 16);
            }
        }

        private int nodeX(int index, int width) {
            int level = 31 - Integer.numberOfLeadingZeros(index + 1);
            int firstAtLevel = (1 << level) - 1;
            int pos = index - firstAtLevel;
            return (pos + 1) * width / ((1 << level) + 1);
        }

        private int nodeY(int index, int levelGap) {
            int level = 31 - Integer.numberOfLeadingZeros(index + 1);
            return 95 + level * levelGap;
        }
    }

    // -------- Main --------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VisualizerFrame().setVisible(true));
    }
}