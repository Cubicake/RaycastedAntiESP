Run 1:
[16:27:33 INFO]: [EventDispatchBenchmark] Event dispatch results (average ns/op, error is 99.9% confidence interval):
[16:27:33 INFO]: [EventDispatchBenchmark] listeners | cubi ns/op | bukkit ns/op | bukkit/cubi
[16:27:33 INFO]: [EventDispatchBenchmark]         0 |    2.529 ± 0.006    |      4.589 ± 0.037    | 1.81x
[16:27:33 INFO]: [EventDispatchBenchmark]         1 |    3.489 ± 0.013    |      7.568 ± 0.102    | 2.17x
[16:27:33 INFO]: [EventDispatchBenchmark]         4 |    5.855 ± 0.025    |     17.063 ± 0.616    | 2.91x
[16:27:33 INFO]: [EventDispatchBenchmark]        16 |   10.737 ± 0.356    |     52.979 ± 0.236    | 4.93x
[16:27:33 INFO]: [EventDispatchBenchmark]        32 |   18.078 ± 3.040    |    101.260 ± 1.882    | 5.60x
[16:27:33 INFO]: [EventDispatchBenchmark]        64 |   29.596 ± 0.088    |    203.622 ± 0.996    | 6.88x

Run 2:
[16:33:24 INFO]: [EventDispatchBenchmark] Event dispatch results (average ns/op, error is 99.9% confidence interval):
[16:33:24 INFO]: [EventDispatchBenchmark] listeners | cubi ns/op | bukkit ns/op | bukkit/cubi
[16:33:24 INFO]: [EventDispatchBenchmark]         0 |    2.550 ± 0.010    |      4.608 ± 0.016    | 1.81x
[16:33:24 INFO]: [EventDispatchBenchmark]         1 |    4.381 ± 0.015    |      7.511 ± 0.011    | 1.71x
[16:33:24 INFO]: [EventDispatchBenchmark]         4 |    6.053 ± 0.009    |     17.762 ± 3.984    | 2.93x
[16:33:24 INFO]: [EventDispatchBenchmark]        16 |   14.320 ± 0.598    |     52.959 ± 0.099    | 3.70x
[16:33:24 INFO]: [EventDispatchBenchmark]        32 |   23.392 ± 0.095    |    101.222 ± 0.228    | 4.33x
[16:33:24 INFO]: [EventDispatchBenchmark]        64 |   41.709 ± 0.692    |    204.705 ± 2.331    | 4.91x

Using CopyOnWriteArray `for (BaseEventHandler<E> eventHandler : handlers.eventHandlers)`, without special-pathing monitor.
Ignore bukkit results, they were made to not fire the event.
[21:01:41 INFO]: [EventDispatchBenchmark] listeners | cubi ns/op | bukkit ns/op | bukkit/cubi
[21:01:41 INFO]: [EventDispatchBenchmark]         0 |    4.287 ± 0.036    |      1.526 ± 0.176    | 0.36x
[21:01:41 INFO]: [EventDispatchBenchmark]         1 |    7.566 ± 0.046    |      1.609 ± 0.123    | 0.21x
[21:01:41 INFO]: [EventDispatchBenchmark]         4 |    8.813 ± 0.019    |      1.576 ± 0.061    | 0.18x
[21:01:41 INFO]: [EventDispatchBenchmark]        16 |   17.443 ± 0.018    |      1.564 ± 0.002    | 0.09x
[21:01:41 INFO]: [EventDispatchBenchmark]        32 |   34.947 ± 1.051    |      1.575 ± 0.042    | 0.05x
[21:01:41 INFO]: [EventDispatchBenchmark]        64 |   74.946 ± 0.066    |      1.572 ± 0.039    | 0.02x

This benchmark is not a strictly apples-to-apples comparison because Bukkit’s event-dispatch path performs additional work, 
including thread validation, plugin-state checks, listener/executor indirection, and exception handling. 
The results therefore compare `games.cubi.utils.events`’s specialised handler registry with Bukkit’s broader public event infrastructure, 
rather than two dispatch mechanisms providing identical functionality.

However, since that extra work is unnecessary for our use-cases, `games.cubi.utils.events` is still better.