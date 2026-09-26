# Wolf-Sheep Simulation Performance Benchmark

## Test Environment & Parameters
* **Simulation Steps:** 100 steps
* **Execution Strategy:** Single-threaded stepwise executor (`StepWiseExecutor`)
* **Measurement Method:** Averaged over 4 runs per agent amount
* **Metrics Tracked:**
    * **Deployment Time**
    * **Execution Time**

---

## Benchmark Data

| Number of Agents | Deployment Time (s) | Execution Time (s) | Total Time (s) |
| :--- | :--- | :--- | :--- |
| **1,080** | 1.08 | 0.92 | 2.00 |
| **4,320** | 2.79 | 6.94 | 9.73 |
| **5,880** | 3.65 | 11.16 | 14.80 |
| **8,670** | 5.32 | 22.80 | 28.12 |
| **12,000** | 7.56 | 44.14 | 51.71 |
| **20,280** | 13.28 | 121.45 | 134.74 |
| **27,000** | 18.82 | 237.64 | 256.47 |

