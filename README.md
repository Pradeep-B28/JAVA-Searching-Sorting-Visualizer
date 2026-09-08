<div align="center">

# 📊 DSA Visualizer — Real-Time Algorithm Animation Suite

### *Step-by-Step Interactive Visualizer for Searching & Sorting Algorithms in Pure Java*

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Swing](https://img.shields.io/badge/GUI-Java%20Swing-007396?style=for-the-badge&logo=java&logoColor=white)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![Algorithms](https://img.shields.io/badge/Focus-Sorting%20%26%20Searching-0891B2?style=for-the-badge)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

---

</div>

> [!TIP]
> **DSA Visualizer** provides a clear, color-coded, real-time animation of sorting algorithms and search comparisons, allowing students to observe swap operations, pivot partitioning, and binary search space reduction frame-by-frame.

---

## ⚡ Supported Algorithms & Features

<div align="center">

| Category | Supported Algorithms | Visual Indicators |
| :--- | :--- | :--- |
| **Sorting** | Bubble Sort, Selection Sort, Insertion Sort, Quick Sort, Merge Sort, Heap Sort | 🔴 Active Comparison<br>🟢 Sorted Position<br>🔵 Pivot Element |
| **Searching** | Linear Search, Binary Search | 🟡 Target Pointer<br>🟣 Midpoint Divider<br>⚪ Excluded Range |

</div>

- 🎛️ **Playback Control Deck**: Play, Pause, Step Forward, and Reset controls.
- 🎚️ **Speed & Size Sliders**: Dynamic array size adjustment (10 to 200 elements) and speed control (10ms to 1000ms delay).
- 🎲 **Array Generator**: Generate Random, Nearly Sorted, Reversed, or Duplicate-heavy datasets.

---

## 🚀 Installation & Running

```bash
# 1. Clone repository
git clone https://github.com/Pradeep-B28/JAVA-Searching-Sorting-Visualizer.git
cd dsa-visualizer

# 2. Compile Java files
javac -d out src/main/java/com/pradeep/dsaviz/*.java

# 3. Launch Visualizer
java -cp out com.pradeep.dsaviz.DSAVisualizerApp
```

---

## 📁 Code Organization

```
dsa-visualizer/
└── src/main/java/com/pradeep/dsaviz/
    ├── DSAVisualizerApp.java          # Application Entry Point & Control Panel
    └── VisualizerFrame.java           # Custom Graphics2D Rendering Canvas
```

---

<div align="center">

Crafted with care by **[Pradeep](https://github.com/Pradeep-B28)**

</div>
