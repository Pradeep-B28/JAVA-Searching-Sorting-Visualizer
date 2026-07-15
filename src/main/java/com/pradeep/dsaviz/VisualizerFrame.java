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
    // -------- Clean, modern color palette --------
    private static final Color BG_DARK = new Color(10, 14, 23);
    private static final Color BG_PANEL = new Color(20, 28, 45);
    private static final Color BAR_COLOR = new Color(0, 180, 255);   // bright cyan
    private static final Color ACTIVE = new Color(255, 215, 0);      // gold
    private static final Color FOUND = new Color(0, 255, 136);       // neon green
    private static final Color PIVOT_COLOR = new Color(255, 107, 157); // pink
    private static final Color HEADER_START = new Color(0, 180, 255);
    private static final Color HEADER_END = new Color(102, 126, 234);
    private static final Color ACCENT = new Color(0, 180, 255);

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
    private final JButton run = new JButton("Visualize");
    private final JButton stop = new JButton("Stop");
    private final JButton pause = new JButton("Pause");
    private final JButton reset = new JButton("New Array");
    private final JButton useCustomArray = new JButton("Use Custom Array");

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
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(ACCENT));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(Color.BLACK));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 720));
        setLocationByPlatform(true);
        buildUi();
        updateAlgorithmOptions();
        generateArray();
        updateInfo();
    }

    private void setModernLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            // Global dark theme
            UIManager.put("control", new ColorUIResource(BG_PANEL));
            UIManager.put("nimbusBase", new ColorUIResource(ACCENT));
            UIManager.put("nimbusBlueGrey", new ColorUIResource(ACCENT));
            UIManager.put("nimbusLightBackground", new ColorUIResource(BG_DARK));
            UIManager.put("text", new ColorUIResource(Color.WHITE));
            UIManager.put("nimbusSelectionBackground", new ColorUIResource(ACCENT));
            UIManager.put("nimbusSelectionForeground", new ColorUIResource(Color.BLACK));
            UIManager.put("info", new ColorUIResource(BG_PANEL));
            UIManager.put("Button.background", new ColorUIResource(ACCENT));
            UIManager.put("Button.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 13));
            // Slider styling – ensure these keys exist to avoid NPE if we were to use custom UI,
            // but we won't – we'll just set these for the default Nimbus slider.
            UIManager.put("Slider.background", new ColorUIResource(BG_PANEL));
            UIManager.put("Slider.foreground", new ColorUIResource(ACCENT));
            UIManager.put("Slider.trackColor", new ColorUIResource(ACCENT));
            UIManager.put("Slider.thumbColor", new ColorUIResource(Color.WHITE));
            // These are used by the default MetalSliderUI if Nimbus falls back – we set them defensively.
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
        root.setBackground(BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        // Header
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, HEADER_START, getWidth(), getHeight(), HEADER_END);
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

        // Bar Panel - the main area
        bars.setBackground(BG_DARK);
        bars.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 2, true),
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
        targetLabel.setForeground(Color.WHITE);
        targetLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        controls.add(styleButton(reset));
        controls.add(styleButton(run));
        controls.add(styleButton(pause));
        controls.add(styleButton(stop));
        stop.setEnabled(false);
        pause.setEnabled(false);

        JPanel customInput = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        customInput.setOpaque(false);
        customInput.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        customInput.add(createStyledLabel("Custom size"));
        customInput.add(styleTextField(customSize));
        customInput.add(createStyledLabel("Elements (comma-separated)"));
        customInput.add(styleTextField(customElements));
        customInput.add(styleButton(useCustomArray));

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(true);
        infoPanel.setBackground(BG_PANEL);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        info.setForeground(new Color(203, 213, 225));
        info.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
    }

    private JComboBox<String> styleCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(BG_PANEL);
        combo.setForeground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(ACCENT);
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(BG_PANEL);
                    c.setForeground(Color.WHITE);
                }
                if (c instanceof JLabel) ((JLabel) c).setOpaque(true);
                return c;
            }
        });
        return combo;
    }

    // Modern slider with clean look – no custom UI, just colours and dimensions.
    private JSlider styleSlider(JSlider slider) {
        slider.setBackground(BG_PANEL);
        slider.setForeground(ACCENT);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
        slider.setPreferredSize(new Dimension(120, 30));
        // The Nimbus L&F will automatically use the UIManager colours we set earlier.
        return slider;
    }

    private JTextField styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(BG_PANEL);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return field;
    }

    private JButton styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void showTargetField(boolean show) {
        targetLabel.setVisible(show);
        target.setVisible(show);
    }

    // -------- Pause/Resume --------
    private void togglePause() {
        synchronized (pauseLock) {
            paused = !paused;
            pause.setText(paused ? "Resume" : "Pause");
            if (!paused) pauseLock.notifyAll();
        }
    }

    // -------- Update Info --------
    private void updateInfo() {
        String selected = (String) algorithm.getSelectedItem();
        String complexity = switch (selected) {
            case "Bubble Sort" -> "Time: O(n²) | Space: O(1)";
            case "Selection Sort" -> "Time: O(n²) | Space: O(1)";
            case "Insertion Sort" -> "Time: O(n²) worst, O(n) best | Space: O(1)";
            case "Merge Sort" -> "Time: O(n log n) | Space: O(n)";
            case "Quick Sort" -> "Time: O(n log n) avg | Space: O(log n)";
            case "Heap Sort" -> "Time: O(n log n) | Space: O(1)";
            case "Linear Search" -> "Time: O(n) | Space: O(1)";
            case "Binary Search" -> "Time: O(log n) | Space: O(1)";
            default -> "";
        };
        info.setText("<html><b>" + selected + "</b> &nbsp;|&nbsp; " + complexity + "</html>");
    }

    private void updateAlgorithmOptions() {
        String[] options = isSearching() ? SEARCHING_ALGORITHMS : SORTING_ALGORITHMS;
        algorithm.setModel(new DefaultComboBoxModel<>(options));
        target.setEnabled(isSearching());
        updateInfo();
    }

    private boolean isSearching() {
        return "Searching".equals(mode.getSelectedItem());
    }

    // -------- Array generation & custom input (unchanged) --------
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

    // -------- Start / Stop --------
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
        pause.setText("Pause");
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

    // -------- Sorting Algorithms (unchanged) --------
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

        // ----- Clean, flat bars with shadow -----
        private void drawBarView(Graphics2D g2, String selectedAlgorithm) {
            int gap = Math.max(1, getWidth() / (values.length * 12));
            int barWidth = Math.max(2, (getWidth() - gap * (values.length + 1)) / values.length);
            int maxVal = Arrays.stream(values).max().orElse(1);

            for (int i = 0; i < values.length; i++) {
                int height = (int) ((values[i] / (double) maxVal) * (getHeight() - 40));
                int x = gap + i * (barWidth + gap);
                int y = getHeight() - height - 12;

                Color color;
                if (i == result) {
                    color = FOUND;
                } else if (i == pivotIndex && "Quick Sort".equals(selectedAlgorithm)) {
                    color = PIVOT_COLOR;
                } else if (i == first || i == second) {
                    color = ACTIVE;
                } else {
                    color = BAR_COLOR;
                }

                // Shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(x + 2, y + 2, barWidth, height, 6, 6);

                // Main bar
                g2.setColor(color);
                g2.fillRoundRect(x, y, barWidth, height, 6, 6);

                // White glow for active/pivot/found
                if (i == first || i == second || i == pivotIndex || i == result) {
                    g2.setColor(new Color(255, 255, 255, 200));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x, y, barWidth, height, 6, 6);
                }

                // Value label
                if (barWidth >= 18) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                    String val = String.valueOf(values[i]);
                    int tw = g2.getFontMetrics().stringWidth(val);
                    g2.drawString(val, x + (barWidth - tw) / 2, Math.max(13, y - 4));
                }
            }
        }

        // ----- Merge Sort View (fills panel) -----
        private void drawMergeView(Graphics2D g2) {
            // Header
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
            g2.drawString("Merge Sort – Divide & Conquer", 24, 40);
            g2.setColor(new Color(203, 213, 225));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));

            String status = mergeState.isMerging ? "Merging two sorted halves" : "Splitting the array recursively";
            g2.drawString(status, 24, 66);

            int panelWidth = getWidth() - 48;
            int yOffset = 90;
            int cellHeight = 36;
            int gap = 4;

            // Always draw the current array (if any)
            if (values != null && values.length > 0) {
                drawArrayRow(g2, "Current array", values, 0, values.length - 1, first, second,
                        24, yOffset, panelWidth, cellHeight, gap, BAR_COLOR, null);
                yOffset += cellHeight + 30;
            }

            // If merging, show the two halves and output
            if (mergeState.isMerging) {
                int[] left = mergeState.leftArr;
                int[] right = mergeState.rightArr;
                int[] output = mergeState.outputArr;
                int outCount = mergeState.outputCount;

                if (left != null && left.length > 0) {
                    drawArrayRow(g2, "Left half", left, 0, left.length - 1,
                            mergeState.leftCursor - 1, mergeState.leftCursor,
                            24, yOffset, panelWidth / 2 - 10, cellHeight, gap, new Color(14, 116, 144), null);
                }
                if (right != null && right.length > 0) {
                    drawArrayRow(g2, "Right half", right, 0, right.length - 1,
                            mergeState.rightCursor - 1, mergeState.rightCursor,
                            24 + panelWidth / 2 + 10, yOffset, panelWidth / 2 - 10, cellHeight, gap, new Color(126, 34, 206), null);
                }
                yOffset += cellHeight + 30;

                if (output != null && output.length > 0) {
                    drawArrayRow(g2, "Merged output", output, 0, output.length - 1,
                            -1, -1,
                            24, yOffset, panelWidth, cellHeight, gap, FOUND, outCount);
                }
            } else if (!mergeState.splits.isEmpty()) {
                // Show split levels (only if we have splits)
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
                                new Color(56, 189, 248, 180), null);
                        x += splitWidth + gap;
                    }
                    rowY += cellHeight + 20;
                    depth++;
                    // Prevent overflow
                    if (rowY + cellHeight > getHeight() - 20) break;
                }
            } else {
                // No splits and not merging – show a message
                g2.setColor(new Color(203, 213, 225));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18));
                String msg = "Press 'Visualize' to start Merge Sort";
                int tw = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - tw) / 2, getHeight() / 2);
            }
        }

        // Helper to draw a row of cells (used in merge view)
        private void drawArrayRow(Graphics2D g2, String label, int[] arr, int start, int end,
                                  int highlight1, int highlight2,
                                  int x, int y, int width, int cellHeight, int gap,
                                  Color defaultColor, Integer filledCount) {
            if (arr == null || arr.length == 0) {
                g2.setColor(new Color(203, 213, 225));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                g2.drawString("(empty)", x, y + 14);
                return;
            }
            if (!label.isEmpty()) {
                g2.setColor(Color.WHITE);
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

                Color color = defaultColor;
                if (i == highlight1 || i == highlight2) color = ACTIVE;
                if (filledCount != null && i >= filledCount) color = new Color(30, 41, 59);

                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Double(cx, cy, cellWidth, cellHeight, 6, 6));
                g2.setColor(Color.WHITE);
                g2.draw(new RoundRectangle2D.Double(cx, cy, cellWidth, cellHeight, 6, 6));
                if (cellWidth >= 16) {
                    String val = String.valueOf(arr[i]);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                    int tw = g2.getFontMetrics().stringWidth(val);
                    g2.drawString(val, cx + (cellWidth - tw) / 2, cy + cellHeight - 8);
                }
            }
        }

        // ----- Heap Sort View (fills panel) -----
        private void drawHeapView(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
            g2.drawString("Heap Sort – Binary Heap", 24, 40);
            g2.setColor(new Color(203, 213, 225));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            g2.drawString("Building max-heap and extracting elements", 24, 66);

            int levels = 32 - Integer.numberOfLeadingZeros(values.length);
            int radius = Math.max(10, Math.min(28, getWidth() / (1 << Math.min(levels + 1, 8))));
            int levelGap = Math.max(48, (getHeight() - 120) / Math.max(1, levels - 1));

            // Check if we have something to draw (i.e., not idle)
            boolean hasNodes = values != null && values.length > 0;

            if (!hasNodes) {
                g2.setColor(new Color(203, 213, 225));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18));
                String msg = "Press 'Visualize' to start Heap Sort";
                int tw = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - tw) / 2, getHeight() / 2);
                return;
            }

            // Draw edges
            g2.setStroke(new BasicStroke(1.5f));
            for (int i = 1; i < values.length; i++) {
                int parent = (i - 1) / 2;
                int px = nodeX(parent, getWidth());
                int py = nodeY(parent, levelGap);
                int cx = nodeX(i, getWidth());
                int cy = nodeY(i, levelGap);
                g2.setColor(new Color(100, 116, 139, 120));
                g2.drawLine(px, py + radius / 2, cx, cy - radius / 2);
            }

            // Draw nodes
            for (int i = 0; i < values.length; i++) {
                int x = nodeX(i, getWidth());
                int y = nodeY(i, levelGap);
                boolean active = (i == first || i == second);

                Color fill;
                if (i >= heapSize) {
                    fill = new Color(50, 50, 60);
                } else if (active) {
                    fill = ACTIVE;
                } else {
                    fill = BAR_COLOR;
                }

                // Simple solid circles
                g2.setColor(fill);
                g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                g2.setColor(Color.WHITE);
                g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);
                if (radius >= 14) {
                    String val = String.valueOf(values[i]);
                    g2.setColor(Color.WHITE);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
                    int tw = g2.getFontMetrics().stringWidth(val);
                    g2.drawString(val, x - tw / 2, y + 5);
                }
                g2.setColor(new Color(148, 163, 184));
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