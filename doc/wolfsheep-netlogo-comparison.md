# Wolf-Sheep: execution time, FLASH-MAS vs NetLogo

Execution time of the same Wolf-Sheep scenario in FLASH-MAS (simulation mode) and in NetLogo, over
100 and 1000 steps.

## Scenario (identical in both)

| Parameter | Value |
|---|---|
| scale | 289 |
| grid | 102 x 102 cells, bounded (no wrapping) |
| grass patches | 4335 (on distinct random cells; only those cells have grass), regrowth after 5 steps |
| sheep | 2890, vision 2 |
| wolves | 1445, vision 3 |
| random seed | 42 |

Rules: no energy and no reproduction; a wolf eats every sheep on its cell; a sheep eats the grass on its
cell; both move one cell (8-neighbourhood) towards the nearest visible target (Chebyshev distance),
otherwise randomly; a sheep that sees a wolf alerts its neighbours. The update is synchronous: every
agent decides from the state at the start of the step; moves and eaten sheep are applied at the end
of the step, messages are delivered at the next step.

- FLASH-MAS: `abms.wolfSheepPredation.WolfSheepBoot <steps>`.
- NetLogo: `src-experiments/abms/wolfSheepPredation/netlogo/WolfSheepFMAS.nlogox`, BehaviorSpace
  experiment `benchmark` (headless, one thread), with `timeLimit` set to the number of steps.

## Method

- Each run is a separate JVM process (cold start for both tools), runs are sequential.
- **Setup**: FLASH-MAS `Deployment` (loading the deployment and creating the ~8700 entities) vs
  NetLogo `setup-ms` (`setup` procedure).
- **Execution**: FLASH-MAS `Execution` (the steps) vs NetLogo `timer` after `setup` (the steps).
- "1 run" is the first run; "10 runs" are runs 1-10 (mean, standard deviation, min, max), in ms.
- Machine: Apple M3, 8 cores, 16 GB, macOS 26.4; FLASH-MAS on OpenJDK 24.0.1 (GraalVM CE);
  NetLogo 7.0.2 (`netlogo-7.0.2.jar`, run with the same JDK).

## Results

### 100 steps

| | FLASH-MAS, 1 run | FLASH-MAS, 10 runs | NetLogo, 1 run | NetLogo, 10 runs |
|---|---|---|---|---|
| Execution (ms) | 1076 | **1105** ± 61 (1056-1251) | 1471 | **1434** ± 191 (1260-1805) |
| Setup (ms) | 2581 | 2535 ± 29 (2496-2581) | 63 | 46 ± 7 (39-63) |

Execution: FLASH-MAS takes **0.77x** the time of NetLogo (NetLogo is 1.30x slower).

### 1000 steps

| | FLASH-MAS, 1 run | FLASH-MAS, 10 runs | NetLogo, 1 run | NetLogo, 10 runs |
|---|---|---|---|---|
| Execution (ms) | 5591 | **5618** ± 250 (5275-6225) | 9803 | **9428** ± 258 (8995-9842) |
| Setup (ms) | 2626 | 2657 ± 143 (2540-2992) | 49 | 51 ± 6 (41-58) |

Execution: FLASH-MAS takes **0.60x** the time of NetLogo (NetLogo is 1.68x slower).

### Behaviour (identical in every run of each tool)

| | FLASH-MAS, 100 steps | NetLogo, 100 steps | FLASH-MAS, 1000 steps | NetLogo, 1000 steps |
|---|---|---|---|---|
| sheep eaten (of 2890) | 2862 | 2887 | 2889 | 2890 |
| grass eaten | 12998 | 12937 | 15532 | 12959 |

FLASH-MAS counts a sheep eaten in the last step only at the next step, so its "sheep eaten" can be
lower by the sheep eaten in the last step. Over 1000 steps, one sheep survives longer in FLASH-MAS
and keeps eating grass; ties between equally distant targets and the order of agents within a step
are broken differently by the two tools, so the trajectories are comparable, not identical.

## Notes

- **Setup is not comparable directly**: FLASH-MAS setup includes reading the deployment
  configuration, creating every entity through loaders and connecting it to the contexts; NetLogo
  only sets patch variables and creates turtles in an already loaded world.
- After about 150 steps almost all sheep have been eaten, so most of the 1000-step run measures the
  1445 wolves looking for sheep (48 cells each per step) and the grass regrowing.

## Raw data

| tool | steps | run | setup (ms) | execution (ms) |
|---|---|---|---|---|
| FLASH-MAS | 100 | 1 | 2581 | 1076 |
| FLASH-MAS | 100 | 2 | 2559 | 1136 |
| FLASH-MAS | 100 | 3 | 2519 | 1100 |
| FLASH-MAS | 100 | 4 | 2496 | 1058 |
| FLASH-MAS | 100 | 5 | 2510 | 1076 |
| FLASH-MAS | 100 | 6 | 2549 | 1152 |
| FLASH-MAS | 100 | 7 | 2530 | 1072 |
| FLASH-MAS | 100 | 8 | 2522 | 1056 |
| FLASH-MAS | 100 | 9 | 2513 | 1069 |
| FLASH-MAS | 100 | 10 | 2573 | 1251 |
| NetLogo | 100 | 1 | 63 | 1471 |
| NetLogo | 100 | 2 | 40 | 1294 |
| NetLogo | 100 | 3 | 41 | 1805 |
| NetLogo | 100 | 4 | 47 | 1274 |
| NetLogo | 100 | 5 | 45 | 1551 |
| NetLogo | 100 | 6 | 49 | 1260 |
| NetLogo | 100 | 7 | 39 | 1684 |
| NetLogo | 100 | 8 | 48 | 1267 |
| NetLogo | 100 | 9 | 42 | 1387 |
| NetLogo | 100 | 10 | 41 | 1348 |
| FLASH-MAS | 1000 | 1 | 2626 | 5591 |
| FLASH-MAS | 1000 | 2 | 2597 | 5617 |
| FLASH-MAS | 1000 | 3 | 2540 | 5526 |
| FLASH-MAS | 1000 | 4 | 2820 | 6225 |
| FLASH-MAS | 1000 | 5 | 2552 | 5531 |
| FLASH-MAS | 1000 | 6 | 2676 | 5275 |
| FLASH-MAS | 1000 | 7 | 2551 | 5435 |
| FLASH-MAS | 1000 | 8 | 2609 | 5548 |
| FLASH-MAS | 1000 | 9 | 2992 | 5713 |
| FLASH-MAS | 1000 | 10 | 2609 | 5721 |
| NetLogo | 1000 | 1 | 49 | 9803 |
| NetLogo | 1000 | 2 | 58 | 9187 |
| NetLogo | 1000 | 3 | 51 | 9842 |
| NetLogo | 1000 | 4 | 57 | 9494 |
| NetLogo | 1000 | 5 | 48 | 9511 |
| NetLogo | 1000 | 6 | 49 | 9343 |
| NetLogo | 1000 | 7 | 44 | 9304 |
| NetLogo | 1000 | 8 | 53 | 9345 |
| NetLogo | 1000 | 9 | 41 | 9458 |
| NetLogo | 1000 | 10 | 56 | 8995 |
