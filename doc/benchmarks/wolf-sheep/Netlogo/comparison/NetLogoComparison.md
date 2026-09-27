# Performance Comparison: FLASH-MAS vs NetLogo

## 1. Test Environment & Methodology
* **Model Benchmark:** Wolf Sheep Predation
* **Simulation Duration:** 100 steps
* **Execution Strategy:** Single-threaded execution (NetLogo default sequential run vs FLASH-MAS `StepWiseExecutor`)
* **Measurement:** time, averaged over 4 independent runs per batch

## 2. Benchmark Results (Seconds)

The table below illustrates the performance of the execution. For FLASH-MAS, the total time was also included (which encompasses the agent deployment overhead).

| Total Agents | NetLogo Exec Time (s) | FLASH-MAS Exec Time (s) | FLASH-MAS Total Time (s) | Multiplier (NetLogo vs FLASH-MAS Exec) |
| :--- | :--- | :--- | :--- | :--- |
| **1,080** | 0.044 | 0.92 | 2.00 | ~20x slower |
| **4,320** | 0.1095 | 6.94 | 9.73 | ~63x slower |
| **5,880** | 0.1655 | 11.16 | 14.80 | ~67x slower |
| **8,670** | 0.298 | 22.80 | 28.12 | ~76x slower |
| **12,000** | 0.416 | 44.14 | 51.71 | ~106x slower |
| **20,280** | 0.753 | 121.45 | 134.74 | ~161x slower |
| **27,000** | 1.0145 | 237.64 | 256.47 | ~234x slower |