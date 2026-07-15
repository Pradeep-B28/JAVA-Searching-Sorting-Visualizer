package com.pradeep.dsaviz;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import javax.swing.*;

public final class VisualizerFrame extends JFrame {
    private static final Color BACKGROUND = new Color(15, 23, 42);
    private static final Color BAR = new Color(56, 189, 248);
    private static final Color ACTIVE = new Color(251, 191, 36);
    private static final Color FOUND = new Color(74, 222, 128);
    private static final Color LEFT_COLOR = new Color(14, 116, 144);
    private static final Color RIGHT_COLOR = new Color(126, 34, 206);
    private static final Color PIVOT_COLOR = new Color(236, 72, 153); // pink/magenta

    private static final String[] SORTING_ALGORITHMS = {
        "Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort", "Heap Sort"
    };
    private static final String[] SEARCHING_ALGORITHMS = {
        "Linear Search", "Binary Search"
    };

    private final Random random = new Random();
    private final BarPanel bars = new BarPanel();
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"Sorting", "Searching"});
    private final JComboBox<String> algorithm = new JComboBox<>(SORTING_ALGORITHMS);
    private final JSlider size = new JSlider(10, 60, 30);
    private final JSlider speed = new JSlider(10, 1000, 300);
    private final JTextField target = new JTextField(5);
    private final JTextField customSize = new JTextField("5", 4);
    private final JTextField customElements = new JTextField(30);
    private final JLabel info = new JLabel();
    private final JButton run = new JButton("Visualize");
    private final JButton stop = new JButton("Stop");
    private final JButton pause = new JButton("Pause");
    private final JButton reset = new JButton("New Array");
    private final JButton useCustomArray = new JButton("Use Custom Array");

    private int[] values;
    private int first = -1;
    private int second = -1;
    private int result = -1;
    private int pivotIndex = -1;   // for Quick Sort

    // Merge sort state
    private MergeState mergeState = new MergeState();

    // Heap sort state (used for visualisation)
    private int heapSize = 0;

    private volatile boolean stopRequested;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    public VisualizerFrame() {
        super("DSA Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        setLocationByPlatform(true);
        buildUi();
        updateAlgorithmOptions();
        generateArray();
        updateInfo();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("DSA Visualizer");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Explore algorithms step by step in Java");
        subtitle.setForeground(new Color(203, 213, 225));
        subtitle.setFont(subtitle.getFont().deriveFont(14f));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Bar panel
        bars.setBackground(BACKGROUND);
        bars.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 2, true));
        root.add(bars, BorderLayout.CENTER);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        controls.setOpaque(false);
        controls.add(label("Mode"));
        controls.add(mode);
        controls.add(label("Algorithm"));
        controls.add(algorithm);
        controls.add(label("Array size"));
        controls.add(size);
        controls.add(label("Delay (ms)"));
        controls.add(speed);
        controls.add(label("Target"));
        controls.add(target);
        controls.add(reset);
        controls.add(run);
        controls.add(pause);
        controls.add(stop);
        stop.setEnabled(false);
        pause.setEnabled(false);

        JPanel customInput = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        customInput.setOpaque(false);
        customInput.add(label("Custom size"));
        customInput.add(customSize);
        customInput.add(label("Elements (comma-separated)"));
        customInput.add(customElements);
        customInput.add(useCustomArray);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        info.setForeground(new Color(203, 213, 225));
        info.setFont(info.getFont().deriveFont(14f));
        bottom.add(controls, BorderLayout.NORTH);
        bottom.add(customInput, BorderLayout.CENTER);
        bottom.add(info, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        mode.addActionListener(e -> updateAlgorithmOptions());
        algorithm.addActionListener(e -> updateInfo());
        reset.addActionListener(e -> generateArray());
        useCustomArray.addActionListener(e -> loadCustomArray());
        run.addActionListener(e -> start());
        stop.addActionListener(e -> stopAnimation());
        pause.addActionListener(e -> togglePause());
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(12f));
        return label;
    }

    private void togglePause() {
        synchronized (pauseLock) {
            paused = !paused;
            pause.setText(paused ? "Resume" : "Pause");
            if (!paused) {
                pauseLock.notifyAll();
            }
        }
    }

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

    // ---------- Sorting Algorithms ----------

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

    // Merge Sort with detailed state
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

    // Quick Sort
    private void quickSort(int low, int high) {
        if (low >= high) return;
        int pivot = partition(low, high);
        // after partition, pivot is at correct position; we can clear pivot index
        pivotIndex = -1;
        quickSort(low, pivot - 1);
        quickSort(pivot + 1, high);
    }

    private int partition(int low, int high) {
        int pivotValue = values[high];
        pivotIndex = high;   // highlight pivot
        info.setText("Partitioning: pivot = " + pivotValue + " at index " + high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            highlight(j, high);  // high is pivot
            if (values[j] <= pivotValue) {
                swap(++i, j);
            }
        }
        // swap pivot into place
        swap(i + 1, high);
        // pivotIndex is now at i+1, but we clear it after returning
        pivotIndex = -1;
        return i + 1;
    }

    // Heap Sort
    private void heapSort() {
        // Build heap
        for (int i = values.length / 2 - 1; i >= 0; i--) {
            heapify(values.length, i);
        }
        // Extract elements
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

    // Searching
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

    // ---------- Helpers ----------

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

    // ---------- Merge State ----------

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

    // ---------- BarPanel ----------

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

        // ------- Standard bar view (for other algorithms) -------

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
                    color = PIVOT_COLOR;   // pivot highlight
                } else if (i == first || i == second) {
                    color = ACTIVE;
                } else {
                    color = BAR;
                }
                g2.setColor(color);
                g2.fillRoundRect(x, y, barWidth, height, 6, 6);
                // Draw a border around pivot
                if (i == pivotIndex && "Quick Sort".equals(selectedAlgorithm)) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(x, y, barWidth, height, 6, 6);
                }
                if (barWidth >= 18) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                    String val = String.valueOf(values[i]);
                    int tw = g2.getFontMetrics().stringWidth(val);
                    g2.drawString(val, x + (barWidth - tw) / 2, Math.max(13, y - 4));
                    // If pivot, add a 'P' label at bottom
                    if (i == pivotIndex && "Quick Sort".equals(selectedAlgorithm)) {
                        g2.setColor(Color.WHITE);
                        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
                        g2.drawString("P", x + (barWidth - 6) / 2, y + height + 14);
                    }
                }
            }
        }

        // ------- Merge Sort View -------

        private void drawMergeView(Graphics2D g2) {
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

            // Draw the current array at the top
            drawArrayRow(g2, "Current array", values, 0, values.length - 1, first, second,
                    24, yOffset, panelWidth, cellHeight, gap, BAR, null);
            yOffset += cellHeight + 30;

            if (mergeState.isMerging) {
                int[] left = mergeState.leftArr;
                int[] right = mergeState.rightArr;
                int[] output = mergeState.outputArr;
                int outCount = mergeState.outputCount;

                drawArrayRow(g2, "Left half", left, 0, left.length - 1,
                        mergeState.leftCursor - 1, mergeState.leftCursor,
                        24, yOffset, panelWidth / 2 - 10, cellHeight, gap, LEFT_COLOR, null);
                drawArrayRow(g2, "Right half", right, 0, right.length - 1,
                        mergeState.rightCursor - 1, mergeState.rightCursor,
                        24 + panelWidth / 2 + 10, yOffset, panelWidth / 2 - 10, cellHeight, gap, RIGHT_COLOR, null);
                yOffset += cellHeight + 30;

                drawArrayRow(g2, "Merged output", output, 0, output.length - 1,
                        -1, -1,
                        24, yOffset, panelWidth, cellHeight, gap, FOUND,
                        outCount);
                yOffset += cellHeight + 30;
            } else {
                // Draw split tree level by level
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
                }
            }
        }

        // Helper: draw a row of bars for an array segment
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

        // ------- Heap Sort View -------

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
                } else {
                    fill = active ? ACTIVE : BAR;
                }

                GradientPaint grad = new GradientPaint(x - radius, y - radius, fill,
                        x + radius, y + radius, fill.darker());
                g2.setPaint(grad);
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
                // Small index label
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VisualizerFrame().setVisible(true));
    }
}