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
otherwise randomly; equally near targets, and equally good cells to move to, are chosen between at
random; a sheep that sees a wolf alerts its neighbours.

Order within a step: the grass patches, sheep and wolves act one by one, in one mixed order that is
fixed for the whole run (FLASH-MAS: the order of the executor's entity set; NetLogo: a list shuffled
once at setup), and each of them receives its messages just before acting, so a grass patch is eaten
or grows back when its turn comes. Moves and eaten sheep are applied at the end of the step, and
messages sent in a step are delivered at the next step.

- FLASH-MAS: the `WolfSheepBoot` scenario (commit `0e63886a` plus random tie-breaking in `SheepAgent`
  and `WolfAgent`), started with the number of steps as a parameter.
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
| Execution (ms) | 1063 | **1066** ± 53 (991-1187) | 1303 | **1382** ± 76 (1284-1516) |
| Setup (ms) | 3033 | 3003 ± 58 (2909-3108) | 59 | 60 ± 8 (45-71) |

Execution: FLASH-MAS takes **0.77x** the time of NetLogo (NetLogo is 1.30x slower).

### 1000 steps

| | FLASH-MAS, 1 run | FLASH-MAS, 10 runs | NetLogo, 1 run | NetLogo, 10 runs |
|---|---|---|---|---|
| Execution (ms) | 5156 | **5206** ± 73 (5124-5338) | 9766 | **9753** ± 225 (9371-10175) |
| Setup (ms) | 2939 | 2983 ± 37 (2930-3046) | 62 | 55 ± 9 (46-71) |

Execution: FLASH-MAS takes **0.53x** the time of NetLogo (NetLogo is 1.87x slower).

### Behaviour (identical in every run of each tool)

| | FLASH-MAS, 100 steps | NetLogo, 100 steps | FLASH-MAS, 1000 steps | NetLogo, 1000 steps |
|---|---|---|---|---|
| sheep eaten (of 2890) | 2889 | 2889 | 2890 | 2890 |
| grass eaten | 11410 | 11332 | 11421 | 11335 |

The two tools differ by less than 1%. Almost all the sheep are eaten within 100 steps in both, so the
grass eaten barely changes between 100 and 1000 steps. FLASH-MAS counts a sheep eaten in the last
step only at the next step, so its "sheep eaten" can be lower by the sheep eaten in the last step.

### Behaviour with other seeds (100 steps)

To see whether the differences are systematic or just chance, both tools were run with seeds 1 to 5.

| seed | FLASH-MAS sheep eaten | NetLogo sheep eaten | FLASH-MAS grass eaten | NetLogo grass eaten |
|---|---|---|---|---|
| 1 | 2890 | 2887 | 11519 | 11862 |
| 2 | 2889 | 2889 | 12267 | 11458 |
| 3 | 2887 | 2889 | 11341 | 11987 |
| 4 | 2887 | 2887 | 12266 | 11961 |
| 5 | 2889 | 2889 | 10994 | 11674 |

The ranges overlap, so the remaining differences come from the random numbers, which the two tools
generate differently.

The exact FLASH-MAS numbers depend on the order in which the executor goes through the entities,
which depends on object identity hashes: they are the same in every run of the same program, but can
change when unrelated code changes.

## Notes

- **Setup is not comparable directly**: FLASH-MAS setup includes reading the deployment
  configuration, creating every entity through loaders and connecting it to the contexts; NetLogo
  only sets patch variables and creates turtles in an already loaded world.
- After about 100 steps almost all sheep have been eaten, so most of the 1000-step run measures the
  1445 wolves looking for sheep (48 cells each per step) and the grass regrowing.

## Raw data

| tool | steps | run | setup (ms) | execution (ms) |
|---|---|---|---|---|
| FLASH-MAS | 100 | 1 | 3033 | 1063 |
| FLASH-MAS | 100 | 2 | 2939 | 1035 |
| FLASH-MAS | 100 | 3 | 2909 | 991 |
| FLASH-MAS | 100 | 4 | 2984 | 1008 |
| FLASH-MAS | 100 | 5 | 3042 | 1075 |
| FLASH-MAS | 100 | 6 | 3001 | 1077 |
| FLASH-MAS | 100 | 7 | 2966 | 1075 |
| FLASH-MAS | 100 | 8 | 3001 | 1061 |
| FLASH-MAS | 100 | 9 | 3108 | 1085 |
| FLASH-MAS | 100 | 10 | 3049 | 1187 |
| NetLogo | 100 | 1 | 59 | 1303 |
| NetLogo | 100 | 2 | 56 | 1343 |
| NetLogo | 100 | 3 | 66 | 1516 |
| NetLogo | 100 | 4 | 65 | 1284 |
| NetLogo | 100 | 5 | 61 | 1333 |
| NetLogo | 100 | 6 | 71 | 1390 |
| NetLogo | 100 | 7 | 45 | 1412 |
| NetLogo | 100 | 8 | 67 | 1467 |
| NetLogo | 100 | 9 | 59 | 1334 |
| NetLogo | 100 | 10 | 49 | 1441 |
| FLASH-MAS | 1000 | 1 | 2939 | 5156 |
| FLASH-MAS | 1000 | 2 | 2968 | 5150 |
| FLASH-MAS | 1000 | 3 | 2979 | 5166 |
| FLASH-MAS | 1000 | 4 | 2962 | 5189 |
| FLASH-MAS | 1000 | 5 | 2930 | 5142 |
| FLASH-MAS | 1000 | 6 | 3031 | 5246 |
| FLASH-MAS | 1000 | 7 | 3000 | 5338 |
| FLASH-MAS | 1000 | 8 | 2987 | 5295 |
| FLASH-MAS | 1000 | 9 | 3046 | 5257 |
| FLASH-MAS | 1000 | 10 | 2985 | 5124 |
| NetLogo | 1000 | 1 | 62 | 9766 |
| NetLogo | 1000 | 2 | 50 | 9761 |
| NetLogo | 1000 | 3 | 64 | 9555 |
| NetLogo | 1000 | 4 | 46 | 10175 |
| NetLogo | 1000 | 5 | 49 | 9830 |
| NetLogo | 1000 | 6 | 49 | 9607 |
| NetLogo | 1000 | 7 | 50 | 9910 |
| NetLogo | 1000 | 8 | 49 | 9642 |
| NetLogo | 1000 | 9 | 71 | 9917 |
| NetLogo | 1000 | 10 | 61 | 9371 |
