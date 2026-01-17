# Thread Executors in Java - Deep Dive Guide

## Table of Contents
1. [Why Thread Executors?](#why-thread-executors)
2. [Restaurant Analogy (Extended)](#restaurant-analogy-extended)
3. [Executor Framework Architecture](#executor-framework-architecture)
4. [Types of Executors Explained](#types-of-executors-explained)
5. [ExecutorService Deep Dive](#executorservice-deep-dive)
6. [Callable and Future Explained](#callable-and-future-explained)
7. [ScheduledExecutorService Deep Dive](#scheduledexecutorservice-deep-dive)
8. [ThreadPoolExecutor Parameters](#threadpoolexecutor-parameters)
9. [Execution Flow (Step-by-Step)](#execution-flow-step-by-step)
10. [Real-World Scenarios](#real-world-scenarios)
11. [Complete Examples](#complete-examples)

---

## Why Thread Executors?

### The Problem with Manual Thread Management

```java
// Bad approach - creating new thread for each task
for (int i = 0; i < 1000; i++) {
    new Thread(() -> {
        processOrder(i);
    }).start();
}
```

**Problems:**
1. **Memory Overhead:** Each thread consumes ~1MB of stack memory
    - 1000 threads = ~1GB RAM just for threads!
2. **CPU Overhead:** Creating/destroying threads is expensive
    - Thread creation takes ~1-2ms
    - Thread destruction requires garbage collection
3. **Context Switching:** Too many threads = CPU wastes time switching
    - Optimal threads ≈ number of CPU cores
    - More threads = more context switching overhead
4. **No Control:** Cannot limit concurrent execution
    - Database might allow only 10 connections
    - But 1000 threads trying to connect!
5. **Resource Exhaustion:** Can crash JVM with OutOfMemoryError
    ```
    java.lang.OutOfMemoryError: unable to create new native thread
    ```

### The Solution: Thread Executors

```java
// Good approach - reuse threads with executor
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < 1000; i++) {
    final int orderId = i;
    executor.submit(() -> processOrder(orderId));
}

executor.shutdown();
```

**Benefits:**
1. **Memory Efficient:** Only 10 threads in memory (not 1000!)
2. **Fast Execution:** Threads are reused, no creation/destruction overhead
3. **Controlled Concurrency:** Exactly 10 threads running at once
4. **Predictable Performance:** Consistent resource usage
5. **Built-in Queue:** Tasks wait in queue when all threads busy
6. **Lifecycle Management:** Clean shutdown methods provided

**Performance Comparison:**

| Approach | Threads Created | Memory Used | Throughput |
|----------|----------------|-------------|------------|
| Manual (1000 tasks) | 1000 | ~1GB | Low (context switching) |
| Executor (10 threads) | 10 | ~10MB | High (optimal threads) |

---

## Restaurant Analogy (Extended)

Let's use a **Pizza Restaurant** to understand Thread Executors:

### Basic Setup

```
Restaurant Configuration:
- Kitchen: ExecutorService (manages chefs)
- Chefs: Threads (do the actual work)
- Order Board: Queue (holds pending orders)
- Orders: Tasks/Runnables (work to be done)
```

### Scenario 1: Fixed Thread Pool (FixedThreadPool)

**Configuration:**
```java
ExecutorService kitchen = Executors.newFixedThreadPool(3);
// Restaurant hires exactly 3 chefs (no more, no less)
```

**How it works:**

**10 customers arrive with orders:**

```
Time: 0s
Orders: [Pizza-1, Pizza-2, Pizza-3, Pizza-4, Pizza-5, ...]

Kitchen Actions:
Chef-1: Takes Pizza-1, starts cooking
Chef-2: Takes Pizza-2, starts cooking  
Chef-3: Takes Pizza-3, starts cooking
Order Board: [Pizza-4, Pizza-5, Pizza-6, ..., Pizza-10] (waiting)
```

```
Time: 5s (Chef-1 finishes Pizza-1)
Chef-1: Looks at order board
Chef-1: Takes Pizza-4 from board, starts cooking
Order Board: [Pizza-5, Pizza-6, ..., Pizza-10]
```

**Key Points:**
- Always 3 chefs working (never more, never less)
- New orders wait on the board (queue)
- Chefs never take breaks (threads stay alive)
- FIFO: First order on board gets picked up first

---

### Scenario 2: Cached Thread Pool (CachedThreadPool)

**Configuration:**
```java
ExecutorService kitchen = Executors.newCachedThreadPool();
// Restaurant hires chefs on-demand, fires idle ones
```

**How it works:**

**Rush Hour - 100 customers arrive:**

```
Time: 0s
Orders: 100 pizzas all at once

Kitchen Actions:
Hire Chef-1 → Makes Pizza-1
Hire Chef-2 → Makes Pizza-2
Hire Chef-3 → Makes Pizza-3
...
Hire Chef-100 → Makes Pizza-100

All 100 orders cooking simultaneously!
```

**Slow Hour - Chefs become idle:**

```
Time: 5min (All pizzas done, no new orders)

Kitchen Actions:
Wait 60 seconds per chef
After 60s idle:
  Fire Chef-1 (not needed)
  Fire Chef-2 (not needed)
  ...
  Keep Chef-100 for 60s, then fire if still idle

After 61s: Kitchen has 0 chefs (all fired)
```

**New Order Arrives:**

```
Time: 6min
Order: Pizza-101

Kitchen Actions:
No chefs available
Hire new Chef-101 → Makes Pizza-101
```

**Key Points:**
- No fixed number of chefs
- Hire chefs instantly when needed
- Fire chefs after 60 seconds of being idle
- Perfect for unpredictable workloads
- Dangerous: Can create thousands of threads if not careful!

---

### Scenario 3: Single Thread Executor (SingleThreadExecutor)

**Configuration:**
```java
ExecutorService kitchen = Executors.newSingleThreadExecutor();
// Restaurant has exactly 1 chef (the owner!)
```

**How it works:**

**10 orders arrive:**

```
Time: 0s
Orders: [Pizza-1, Pizza-2, ..., Pizza-10]

Kitchen Actions:
Chef-1 (the owner): Takes Pizza-1
Order Board: [Pizza-2, Pizza-3, ..., Pizza-10] (waiting in line)

Time: 5min (Pizza-1 done)
Chef-1: Takes Pizza-2 from board
Order Board: [Pizza-3, Pizza-4, ..., Pizza-10]

... and so on (one at a time, in order)
```

**Key Points:**
- Only 1 chef, ever
- Orders processed sequentially (one after another)
- Guarantees FIFO order execution
- Perfect for tasks that must run in sequence
- Example: Processing bank transactions (order matters!)

---

### Scenario 4: Scheduled Thread Pool (ScheduledThreadPool)

**Configuration:**
```java
ScheduledExecutorService kitchen = Executors.newScheduledThreadPool(2);
// Restaurant with 2 chefs for timed orders
```

**How it works:**

**Customer orders:**

```java
// "Make a pizza 5 minutes from now"
kitchen.schedule(() -> makePizza(), 5, TimeUnit.MINUTES);

// "Make a pizza every 10 minutes starting now"
kitchen.scheduleAtFixedRate(() -> makePizza(), 0, 10, TimeUnit.MINUTES);

// "Make a pizza, then wait 10 minutes after finishing, then repeat"
kitchen.scheduleWithFixedDelay(() -> makePizza(), 0, 10, TimeUnit.MINUTES);
```

**Timeline:**

```
Time: 0:00 - Start
  Chef-1: Makes Pizza-1 (takes 3 minutes)
  
Time: 0:03 - Pizza-1 done
  
Time: 0:05 - Scheduled task triggers
  Chef-1: Makes Pizza-2

Time: 0:10 - Fixed rate task triggers
  Chef-2: Makes Pizza-3 (even if Chef-1 still busy)

Time: 0:13 - Fixed delay waits 10 min AFTER Pizza-3 done
  
Time: 0:23 - Next fixed delay task triggers
  Chef: Makes Pizza-4
```

**Key Points:**
- Schedule tasks for future execution
- Run tasks periodically (recurring)
- Fixed rate vs Fixed delay (important difference!)
- Perfect for maintenance tasks, polling, monitoring

---

## Executor Framework Architecture

### Hierarchy

```
                    Executor (interface)
                        |
                        |
                ExecutorService (interface)
                   /          \
                  /            \
     AbstractExecutorService    ScheduledExecutorService (interface)
            |                            |
            |                            |
    ThreadPoolExecutor          ScheduledThreadPoolExecutor
```

### Interface Responsibilities

**Executor (base interface):**
```java
public interface Executor {
    void execute(Runnable command);
}
```
- Simplest interface
- Only one method: `execute()`
- Fire-and-forget execution
- No return value, no status tracking

**ExecutorService (extends Executor):**
```java
public interface ExecutorService extends Executor {
    // Lifecycle management
    void shutdown();
    List<Runnable> shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    boolean awaitTermination(long timeout, TimeUnit unit);
    
    // Task submission with results
    <T> Future<T> submit(Callable<T> task);
    Future<?> submit(Runnable task);
    <T> Future<T> submit(Runnable task, T result);
    
    // Batch operations
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks);
    <T> T invokeAny(Collection<? extends Callable<T>> tasks);
}
```
- Adds lifecycle methods
- Returns Future for tracking
- Supports Callable tasks (with return values)
- Batch execution methods

**ScheduledExecutorService (extends ExecutorService):**
```java
public interface ScheduledExecutorService extends ExecutorService {
    // Delayed execution
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);
    
    // Periodic execution
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, 
                                           long initialDelay,
                                           long period, 
                                           TimeUnit unit);
    
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                              long initialDelay,
                                              long delay,
                                              TimeUnit unit);
}
```
- Adds scheduling capabilities
- Delayed execution
- Periodic/recurring execution
- Returns ScheduledFuture for cancellation

---

## Types of Executors Explained

### 1. Fixed Thread Pool

```java
ExecutorService executor = Executors.newFixedThreadPool(5);
```

**Under the hood:**
```java
// What Executors.newFixedThreadPool(5) actually creates:
new ThreadPoolExecutor(
    5,                              // corePoolSize: 5
    5,                              // maximumPoolSize: 5 (same as core)
    0L,                             // keepAliveTime: 0 (doesn't matter)
    TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<Runnable>() // Unbounded queue
);
```

**Characteristics:**

| Property | Value |
|----------|-------|
| Number of threads | Fixed at 5 |
| Queue type | LinkedBlockingQueue (unbounded) |
| Queue size | Unlimited |
| Thread creation | Create up to 5, then queue tasks |
| Thread termination | Never (unless shutdown) |

**Execution flow:**

```
Submit Task-1: Create Thread-1, execute Task-1
Submit Task-2: Create Thread-2, execute Task-2
Submit Task-3: Create Thread-3, execute Task-3
Submit Task-4: Create Thread-4, execute Task-4
Submit Task-5: Create Thread-5, execute Task-5
Submit Task-6: Queue Task-6 (all threads busy)
Submit Task-7: Queue Task-7
...
Submit Task-1000: Queue Task-1000

Thread-1 finishes: Picks Task-6 from queue
Thread-2 finishes: Picks Task-7 from queue
... and so on
```

**When to use:**
- ✅ Known, consistent workload
- ✅ Want to limit concurrent execution
- ✅ Tasks can wait in queue
- ✅ Long-running application
- ❌ Unpredictable bursts (queue can grow huge!)

**Example Use Case:**
```java
// Web server handling requests
ExecutorService webServer = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2
);

// Process incoming requests
for (HttpRequest request : incomingRequests) {
    webServer.submit(() -> handleRequest(request));
}
```

---

### 2. Cached Thread Pool

```java
ExecutorService executor = Executors.newCachedThreadPool();
```

**Under the hood:**
```java
// What Executors.newCachedThreadPool() actually creates:
new ThreadPoolExecutor(
    0,                              // corePoolSize: 0 (no permanent threads)
    Integer.MAX_VALUE,              // maxPoolSize: virtually unlimited
    60L,                            // keepAliveTime: 60 seconds
    TimeUnit.SECONDS,
    new SynchronousQueue<Runnable>() // Zero-capacity queue
);
```

**Characteristics:**

| Property | Value |
|----------|-------|
| Number of threads | 0 to unlimited |
| Queue type | SynchronousQueue (no storage) |
| Queue size | 0 (must hand off immediately) |
| Thread creation | Create new thread for each task if none available |
| Thread termination | After 60 seconds idle |

**Execution flow:**

```
Submit Task-1:
  Check: Any idle thread? NO
  Action: Create Thread-1
  Result: Thread-1 executes Task-1

Submit Task-2 (while Task-1 still running):
  Check: Any idle thread? NO (Thread-1 busy)
  Action: Create Thread-2
  Result: Thread-2 executes Task-2

... Creates as many threads as needed!

After 60 seconds:
  Thread-1 idle for 60s → Terminate Thread-1
  Thread-2 idle for 60s → Terminate Thread-2
```

**When to use:**
- ✅ Many short-lived tasks
- ✅ Unpredictable workload
- ✅ Tasks need immediate execution
- ✅ Short-running application
- ❌ Long-running tasks (creates too many threads!)
- ❌ Memory-constrained environment

**Example Use Case:**
```java
// Async I/O operations
ExecutorService ioPool = Executors.newCachedThreadPool();

for (File file : filesToRead) {
    ioPool.submit(() -> {
        // Quick I/O operation
        String content = readFile(file);
        processContent(content);
    });
}
```

---

### 3. Single Thread Executor

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
```

**Under the hood:**
```java
// What Executors.newSingleThreadExecutor() actually creates:
new FinalizableDelegatedExecutorService(
    new ThreadPoolExecutor(
        1,                          // corePoolSize: 1
        1,                          // maxPoolSize: 1 (only one thread ever)
        0L,                         // keepAliveTime: 0
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>() // Unbounded queue
    )
);
```

**Characteristics:**

| Property | Value |
|----------|-------|
| Number of threads | Exactly 1 |
| Queue type | LinkedBlockingQueue (unbounded) |
| Queue size | Unlimited |
| Execution order | Sequential (FIFO) |
| Thread termination | Never (unless shutdown) |

**Execution flow:**

```
Submit Task-1: Thread-1 executes Task-1
Submit Task-2: Queue Task-2 (Thread-1 busy)
Submit Task-3: Queue Task-3
Queue: [Task-2, Task-3]

Task-1 completes:
Thread-1 picks Task-2 from queue
Queue: [Task-3]

Task-2 completes:
Thread-1 picks Task-3 from queue
Queue: []
```

**When to use:**
- ✅ Tasks must execute in order
- ✅ Shared resource allows only one thread
- ✅ Need serialization of operations
- ✅ Event-driven processing
- ❌ High-throughput requirements

**Example Use Case:**
```java
// Sequential file writing
ExecutorService fileWriter = Executors.newSingleThreadExecutor();

for (LogEntry log : logs) {
    fileWriter.submit(() -> {
        // Write to file (must be sequential)
        writeToFile(log);
    });
}
```

---

### 4. Scheduled Thread Pool

```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
```

**Under the hood:**
```java
// What Executors.newScheduledThreadPool(3) actually creates:
new ScheduledThreadPoolExecutor(3);

// Which extends ThreadPoolExecutor with:
super(
    corePoolSize,                   // 3
    Integer.MAX_VALUE,              // maxPoolSize (not really used)
    0,                              // keepAliveTime
    NANOSECONDS,
    new DelayedWorkQueue()          // Special queue for scheduling
);
```

**Characteristics:**

| Property | Value |
|----------|-------|
| Number of threads | Fixed at 3 |
| Queue type | DelayedWorkQueue (priority-based) |
| Scheduling support | Yes (delays and periodic) |
| Execution order | By scheduled time |

**When to use:**
- ✅ Periodic tasks (polling, monitoring)
- ✅ Delayed execution
- ✅ Recurring jobs
- ✅ Maintenance tasks
- ❌ Simple one-time tasks (use regular executor)

**Example Use Case:**
```java
// Health check every 30 seconds
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(2);

monitor.scheduleAtFixedRate(() -> {
    checkDatabaseHealth();
    checkServiceHealth();
}, 0, 30, TimeUnit.SECONDS);
```

---

### 5. Work Stealing Pool (Java 8+)

```java
ExecutorService executor = Executors.newWorkStealingPool();
```

**Under the hood:**
```java
// What Executors.newWorkStealingPool() actually creates:
new ForkJoinPool(
    Runtime.getRuntime().availableProcessors(), // Parallelism level
    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
    null,                                        // Exception handler
    true                                         // Async mode
);
```

**Characteristics:**

| Property | Value |
|----------|-------|
| Number of threads | Number of CPU cores |
| Queue type | Deque (double-ended queue) per thread |
| Work stealing | Yes (idle threads steal from busy ones) |
| Best for | Recursive, divide-and-conquer tasks |

**How work stealing works:**

```
Initially:
Thread-1 Queue: [Task-1, Task-2, Task-3, Task-4]
Thread-2 Queue: []

Thread-1: Busy processing Task-1
Thread-2: Idle, looks for work

Work Stealing:
Thread-2 "steals" Task-4 from Thread-1's queue
Thread-1 Queue: [Task-1, Task-2, Task-3]
Thread-2 Queue: [Task-4]

Both threads now working!
```

**When to use:**
- ✅ Parallel processing
- ✅ Recursive algorithms
- ✅ Divide-and-conquer tasks
- ✅ CPU-intensive work
- ❌ Simple sequential tasks

---

## ExecutorService Deep Dive

### Task Submission Methods

#### 1. execute() - Fire and Forget

```java
executor.execute(Runnable task)
```

**What it does:**
- Accepts only Runnable
- Returns void (nothing)
- No way to check status or get result
- Exceptions swallowed (gone forever!)

**Example:**
```java
ExecutorService executor = Executors.newFixedThreadPool(2);

executor.execute(() -> {
    System.out.println("Task running in: " + Thread.currentThread().getName());
    // No return value
    // Exceptions lost!
});

// Cannot get result or status
```

**Use when:**
- Don't care about result
- Don't need to track completion
- Fire-and-forget operations
- Example: Logging, analytics events

---

#### 2. submit(Runnable) - Track Completion

```java
Future<?> future = executor.submit(Runnable task)
```

**What it does:**
- Accepts Runnable
- Returns Future<?> (wildcard)
- Can check completion status
- Returns null on get()

**Example:**
```java
Future<?> future = executor.submit(() -> {
    System.out.println("Task running");
    // Do some work
});

// Check if done
if (future.isDone()) {
    System.out.println("Task completed");
}

// Wait for completion (blocks)
future.get(); // Returns null for Runnable
```

**Use when:**
- Need to know when task completes
- Want to cancel task
- Don't need return value
- Example: Background cleanup tasks

---

#### 3. submit(Runnable, T result) - Predetermined Result

```java
Future<T> future = executor.submit(Runnable task, T result)
```

**What it does:**
- Accepts Runnable + result object
- Returns Future<T>
- Returns the provided result object on get()
- Result object can be modified by task

**Example:**
```java
StringBuilder result = new StringBuilder();

Future<StringBuilder> future = executor.submit(() -> {
    result.append("Task completed");
}, result);

// Get the result object (same as we passed in)
StringBuilder output = future.get();
System.out.println(output); // "Task completed"
```

**Use when:**
- Want to populate an object
- Collect results in shared container
- Example: Building a report object

---

#### 4. submit(Callable) - Get Return Value

```java
Future<T> future = executor.submit(Callable<T> task)
```

**What it does:**
- Accepts Callable (not Runnable)
- Returns Future<T>
- Returns actual result from call()
- Can throw checked exceptions

**Example:**
```java
Future<Integer> future = executor.submit(() -> {
    // Callable can return a value
    return 42;
});

// Get the actual result
Integer result = future.get(); // Returns 42
System.out.println(result); // 42
```

**Use when:**
- Need return value from task
- Task can throw checked exceptions
- Most common use case
- Example: Database queries, calculations

---

### Lifecycle Management

**Executor States:**

```
                NEW
                 ↓
           (tasks submitted)
                 ↓
              RUNNING
                 ↓
           (shutdown called)
                 ↓
             SHUTDOWN
                 ↓
        (all tasks complete)
                 ↓
            TERMINATED
```

#### shutdown() - Graceful Shutdown

```java
executor.shutdown();
```

**What happens:**
1. Stops accepting new tasks (submit() will throw RejectedExecutionException)
2. Previously submitted tasks continue executing
3. Waits for all tasks to complete
4. Then terminates threads

**Example:**
```java
ExecutorService executor = Executors.newFixedThreadPool(3);

// Submit 10 tasks
for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        Thread.sleep(5000);
        System.out.println("Task complete");
    });
}

// Initiate shutdown (no new tasks accepted)
executor.shutdown();

// This will throw RejectedExecutionException!
executor.submit(() -> System.out.println("New task"));

// Wait for all 10 tasks to finish
executor.awaitTermination(1, TimeUnit.MINUTES);
```

---

#### shutdownNow() - Immediate Shutdown

```java
List<Runnable> pendingTasks = executor.shutdownNow();
```

**What happens:**
1. Stops accepting new tasks
2. Attempts to interrupt running tasks (sends interrupt signal)
3. Returns list of tasks that never started
4. Does NOT guarantee running tasks will stop!

**Example:**
```java
ExecutorService executor = Executors.newFixedThreadPool(2);

// Submit 10 tasks (only 2 running, 8 in queue)
for (int i = 0; i < 10; i++) {
    final int taskId = i;
    executor.submit(() -> {
        try {
            Thread.sleep(60000); // 1 minute
            System.out.println("Task " + taskId + " complete");
        } catch (InterruptedException e) {
            System.out.println("Task " + taskId + " interrupted!");
        }
    });
}

Thread.sleep(1000); // Let 2 tasks start

// Force shutdown
List<Runnable> pending = executor.shutdownNow();
System.out.println("Pending tasks: " + pending.size()); // 8

// Output:
// Task 0 interrupted!
// Task 1 interrupted!
// Pending tasks: 8
```

**Important:** Tasks must check interrupted flag!

```java
executor.submit(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // Do work
        if (someCondition) {
            break; // Handle interrupt
        }
    }
});
```

---

#### awaitTermination() - Wait for Completion

```java
boolean finished = executor.awaitTermination(long timeout, TimeUnit unit);
```

**What it does:**
- Blocks until all tasks complete OR timeout occurs
- Returns true if all tasks completed
- Returns false if timeout occurred
- Must call shutdown() first!

**Example: Proper shutdown pattern**

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

try {
    // Submit tasks
    for (int i = 0; i < 100; i++) {
        executor.submit(() -> doWork());
    }
} finally {
    // Shutdown executor
    executor.shutdown();
    
    try {
        // Wait 60 seconds for completion
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            // Timeout occurred, force shutdown
            executor.shutdownNow();
            
            // Wait again for forced shutdown
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate");
            }
        }
    } catch (InterruptedException e) {
        // Current thread interrupted
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

---

### Batch Operations

#### invokeAll() - Execute All, Wait for All

```java
List<Future<T>> results = executor.invokeAll(Collection<Callable<T>> tasks);
```

**What it does:**
- Submits all tasks
- Blocks until ALL complete
- Returns list of Futures (same order as input)
- All Futures are guaranteed to be done

**Example:**
```java
ExecutorService executor = Executors.newFixedThreadPool(5);

List<Callable<Integer>> tasks = Arrays.asList(
    () -> { Thread.sleep(1000); return 1; },
    () -> { Thread.sleep(2000); return 2; },
    () -> { Thread.sleep(3000); return 3; }
);

// Blocks for 3 seconds (longest task)
List<Future<Integer>> results = executor.invokeAll(tasks);

// All futures are done
for (Future<Integer> future : results) {
    System.out.println(future.get()); // No blocking, already done
}
// Output: 1, 2, 3
```

**With timeout:**
```java
// Wait maximum 2 seconds
List<Future<Integer>> results = executor.invokeAll(tasks, 2, TimeUnit.SECONDS);

// Some tasks might be cancelled
for (Future<Integer> future : results) {
    if (future.isCancelled()) {
        System.out.println("Task cancelled (timeout)");
    } else {
        System.out.println(future.get());
    }
}
```

---

#### invokeAny() - Execute All, Return First

```java
T result = executor.invokeAny(Collection<Callable<T>> tasks);
```

**What it does:**
- Submits all tasks
- Returns result of FIRST completed task
- Cancels remaining tasks
- Throws exception if all fail

**Example:**
```java
List<Callable<String>> tasks = Arrays.asList(
    () -> { Thread.sleep(3000); return "Slow server"; },
    () -> { Thread.sleep(1000); return "Fast server"; },
    () -> { Thread.sleep(2000); return "Medium server"; }
);

// Returns "Fast server" (finishes first)
String result = executor.invokeAny(tasks);
System.out.println(result); // "Fast server"

// Other tasks are cancelled (waste of resources avoided)
```

**Use case: Redundant requests**
```java
// Query multiple databases, use first response
List<Callable<Data>> tasks = Arrays.asList(
    () -> queryDatabase1(),
    () -> queryDatabase2(),
    () -> queryDatabase3()
);

Data data = executor.invokeAny(tasks); // Fastest wins
```

---

## Callable and Future Explained

### Runnable vs Callable

**Runnable (old):**
```java
public interface Runnable {
    void run(); // Cannot return value, cannot throw checked exception
}
```

**Callable (new):**
```java
public interface Callable<V> {
    V call() throws Exception; // Can return value and throw exceptions
}
```

### Comparison Table

| Feature | Runnable | Callable |
|---------|----------|----------|
| Return value | No (void) | Yes (generic type V) |
| Checked exceptions | No | Yes |
| Method name | run() | call() |
| Used with | execute(), submit() | submit() only |
| Result tracking | No | Yes (via Future) |

### Callable Example

```java
Callable<Integer> task = new Callable<Integer>() {
    @Override
    public Integer call() throws Exception {
        // Can return value
        int result = performExpensiveCalculation();
        
        // Can throw checked exception
        if (result < 0) {
            throw new IllegalStateException("Invalid result");
        }
        
        return result;
    }
};

// With lambda (Java 8+)
Callable<Integer> task2 = () -> {
    return performExpensiveCalculation();
};
```

---

### Future Interface Deep Dive

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
```

#### Future States

```
                WAITING
                   ↓
            (task running)
                   ↓
        ┌──────────┴──────────┐
        ↓                     ↓
   COMPLETED             CANCELLED
```

#### Method 1: get() - Blocking Wait

```java
V result = future.get();
```

**What happens:**
- Blocks current thread until task completes
- Returns result of Callable.call()
- Throws ExecutionException if task threw exception
- Throws InterruptedException if current thread interrupted

**Example:**
```java
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(5000); // 5 second task
    return 42;
});

System.out.println("Waiting for result...");
Integer result = future.get(); // Blocks for 5 seconds
System.out.println("Result: " + result); // 42
```

**Danger:** Can deadlock if not careful!

```java
// BAD: Single thread executor calling get() on its own task
ExecutorService executor = Executors.newSingleThreadExecutor();

Future<Integer> future = executor.submit(() -> {
    // This task waits for another task
    Future<Integer> innerFuture = executor.submit(() -> 42);
    return innerFuture.get(); // DEADLOCK! No thread to run inner task
});

future.get(); // Blocks forever!
```

---

#### Method 2: get(timeout) - Timed Wait

```java
V result = future.get(long timeout, TimeUnit unit);
```

**What happens:**
- Waits maximum timeout duration
- Returns result if completed in time
- Throws TimeoutException if timeout exceeded
- Does NOT cancel task on timeout!

**Example:**
```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(10000); // 10 seconds
    return "Done";
});

try {
    // Wait maximum 5 seconds
    String result = future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("Task too slow!");
    future.cancel(true); // Cancel manually
}
```

**Best Practice: Always use timeout in production!**

```java
// Good practice
try {
    Result result = future.get(30, TimeUnit.SECONDS);
    processResult(result);
} catch (TimeoutException e) {
    logger.warn("Task timeout, cancelling");
    future.cancel(true);
    handleTimeout();
} catch (ExecutionException e) {
    logger.error("Task failed", e.getCause());
    handleError(e.getCause());
}
```

---

#### Method 3: isDone() - Check Status

```java
boolean isDone = future.isDone();
```

**What it does:**
- Returns true if task completed (success or failure)
- Returns true if task was cancelled
- Returns false if still running
- Non-blocking (returns immediately)

**Example:**
```java
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(5000);
    return 42;
});

while (!future.isDone()) {
    System.out.println("Still working...");
    Thread.sleep(1000);
}

System.out.println("Done! Result: " + future.get());
```

**Polling pattern:**
```java
Future<Report> future = executor.submit(() -> generateReport());

// Poll every second, max 60 seconds
for (int i = 0; i < 60; i++) {
    if (future.isDone()) {
        return future.get();
    }
    Thread.sleep(1000);
    updateProgress((i + 1) * 100 / 60); // Update UI
}

throw new TimeoutException("Report generation timeout");
```

---

#### Method 4: cancel() - Stop Execution

```java
boolean cancelled = future.cancel(boolean mayInterruptIfRunning);
```

**Parameters:**
- `mayInterruptIfRunning = true`: Send interrupt signal to thread
- `mayInterruptIfRunning = false`: Only cancel if not started

**Return value:**
- `true`: Cancellation successful
- `false`: Could not cancel (already done or cancelled)

**Example:**
```java
Future<String> future = executor.submit(() -> {
    for (int i = 0; i < 100; i++) {
        if (Thread.currentThread().isInterrupted()) {
            System.out.println("Task interrupted!");
            return "Cancelled";
        }
        Thread.sleep(100);
    }
    return "Completed";
});

Thread.sleep(2000); // Let it run for 2 seconds

// Cancel with interrupt
boolean cancelled = future.cancel(true);
System.out.println("Cancelled: " + cancelled); // true

// Attempting to get result
try {
    future.get();
} catch (CancellationException e) {
    System.out.println("Task was cancelled");
}
```

**Cancellation scenarios:**

```java
// Scenario 1: Task not started yet
Future<?> future1 = executor.submit(task);
future1.cancel(false); // true (cancelled before execution)

// Scenario 2: Task running, interrupt allowed
Future<?> future2 = executor.submit(task);
Thread.sleep(100); // Let it start
future2.cancel(true); // true (sends interrupt)

// Scenario 3: Task running, no interrupt
Future<?> future3 = executor.submit(task);
Thread.sleep(100);
future3.cancel(false); // false (can't cancel running task)

// Scenario 4: Task already done
Future<?> future4 = executor.submit(shortTask);
Thread.sleep(1000);
future4.cancel(true); // false (already completed)
```

---

#### Method 5: isCancelled() - Check Cancellation

```java
boolean isCancelled = future.isCancelled();
```

**What it does:**
- Returns true if cancel() was called and succeeded
- Returns false otherwise
- Useful for conditional logic

**Example:**
```java
Future<Data> future = executor.submit(() -> fetchData());

// User clicks cancel button
if (userClickedCancel) {
    future.cancel(true);
}

// Later, check status
if (future.isCancelled()) {
    showMessage("Operation cancelled by user");
} else if (future.isDone()) {
    Data data = future.get();
    displayData(data);
}
```

---

### Future Exception Handling

**ExecutionException wraps task exceptions:**

```java
Future<Integer> future = executor.submit(() -> {
    if (someCondition) {
        throw new IllegalArgumentException("Invalid input");
    }
    return 42;
});

try {
    Integer result = future.get();
} catch (ExecutionException e) {
    // Get the actual exception
    Throwable cause = e.getCause();
    
    if (cause instanceof IllegalArgumentException) {
        System.out.println("Invalid input: " + cause.getMessage());
    }
}
```

**Complete exception handling:**

```java
Future<Result> future = executor.submit(() -> processData());

try {
    Result result = future.get(30, TimeUnit.SECONDS);
    handleSuccess(result);
    
} catch (TimeoutException e) {
    logger.warn("Processing timeout");
    future.cancel(true);
    handleTimeout();
    
} catch (CancellationException e) {
    logger.info("Processing cancelled");
    handleCancellation();
    
} catch (ExecutionException e) {
    logger.error("Processing failed", e.getCause());
    handleError(e.getCause());
    
} catch (InterruptedException e) {
    logger.warn("Thread interrupted");
    Thread.currentThread().interrupt();
    handleInterruption();
}
```

---

## ScheduledExecutorService Deep Dive

### Scheduling Methods

#### 1. schedule() - One-time Delayed Execution

```java
ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
```

**What it does:**
- Executes task once after specified delay
- Returns ScheduledFuture
- Can cancel scheduled task

**Example:**
```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

// Run task after 5 seconds
ScheduledFuture<?> future = scheduler.schedule(() -> {
    System.out.println("Executed after 5 seconds");
}, 5, TimeUnit.SECONDS);

// Cancel before it runs
Thread.sleep(2000);
future.cancel(false); // Cancels the scheduled task
```

**Timeline:**
```
Time: 0s - schedule() called
         Task queued for execution at 5s
         
Time: 1s - Still waiting...
Time: 2s - Still waiting...
Time: 3s - Still waiting...
Time: 4s - Still waiting...
Time: 5s - Task executes!
         Output: "Executed after 5 seconds"
```

**Use cases:**
- Retry after delay
- Cache expiration
- Delayed notifications
- Timeout implementation

---

#### 2. scheduleAtFixedRate() - Periodic Execution (Fixed Rate)

```java
ScheduledFuture<?> scheduleAtFixedRate(
    Runnable command,
    long initialDelay,
    long period,
    TimeUnit unit
)
```

**What it does:**
- First execution after initialDelay
- Subsequent executions every period
- **Does NOT wait for previous execution to complete**
- If execution takes longer than period, next starts immediately

**Example:**
```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Task at: " + System.currentTimeMillis());
    Thread.sleep(2000); // Takes 2 seconds
}, 0, 1, TimeUnit.SECONDS); // Every 1 second
```

**Timeline:**
```
Time: 0s - First execution starts (initialDelay = 0)
          Task-1 running...

Time: 1s - Second execution SHOULD start (period = 1s)
          But Task-1 still running (takes 2s)
          Task-2 WAITS

Time: 2s - Task-1 finishes
          Task-2 starts IMMEDIATELY (was supposed to start at 1s)
          
Time: 3s - Third execution SHOULD start
          But Task-2 still running
          Task-3 WAITS
          
Time: 4s - Task-2 finishes
          Task-3 starts IMMEDIATELY
```

**Critical Point:** Tasks execute at fixed rate (1 second apart in this example), regardless of execution time. If task takes longer than period, tasks queue up!

**Real example:**
```java
// Check stock prices every 10 seconds
scheduler.scheduleAtFixedRate(() -> {
    double price = fetchStockPrice("AAPL");
    System.out.println("AAPL: $" + price);
}, 0, 10, TimeUnit.SECONDS);

// Timeline (if fetch takes 3 seconds):
// 0s:  Fetch starts
// 3s:  Fetch completes, print price
// 10s: Next fetch starts (regardless of previous completion time)
// 13s: Fetch completes
// 20s: Next fetch starts
```

---

#### 3. scheduleWithFixedDelay() - Periodic Execution (Fixed Delay)

```java
ScheduledFuture<?> scheduleWithFixedDelay(
    Runnable command,
    long initialDelay,
    long delay,
    TimeUnit unit
)
```

**What it does:**
- First execution after initialDelay
- Waits for current execution to complete
- **Then waits delay duration**
- Then starts next execution

**Example:**
```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("Task at: " + System.currentTimeMillis());
    Thread.sleep(2000); // Takes 2 seconds
}, 0, 1, TimeUnit.SECONDS); // 1 second AFTER completion
```

**Timeline:**
```
Time: 0s - First execution starts (initialDelay = 0)
          Task-1 running...

Time: 2s - Task-1 finishes (took 2 seconds)
          Wait 1 second (delay)

Time: 3s - Second execution starts
          Task-2 running...

Time: 5s - Task-2 finishes
          Wait 1 second

Time: 6s - Third execution starts
          Task-3 running...
```

**Critical Point:** Total time between starts = execution time + delay. More predictable than fixed rate.

**Real example:**
```java
// Poll external API (don't want to overwhelm it)
scheduler.scheduleWithFixedDelay(() -> {
    Data data = callExternalAPI(); // Might take 0-5 seconds
    processData(data);
}, 0, 2, TimeUnit.SECONDS); // Always wait 2 seconds AFTER completion

// This guarantees minimum 2 second gap between calls
```

---

### Fixed Rate vs Fixed Delay

**Visual Comparison:**

```
scheduleAtFixedRate (period = 10s, execution = 3s):
|─Task1─|       |─Task2─|       |─Task3─|
0s     3s      10s    13s      20s    23s
       └─7s gap─┘      └─7s gap─┘
Total: 10s between starts

scheduleWithFixedDelay (delay = 10s, execution = 3s):
|─Task1─|          |─Task2─|          |─Task3─|
0s     3s        13s    16s        26s    29s
       └─10s gap──┘      └─10s gap──┘
Total: 13s between starts (3s exec + 10s delay)
```

**When task is faster than period/delay:**

```
Fixed Rate (period = 10s, execution = 2s):
|─T1─|        |─T2─|        |─T3─|
0s  2s       10s 12s       20s 22s
    └──8s gap──┘   └──8s gap──┘

Fixed Delay (delay = 10s, execution = 2s):
|─T1─|          |─T2─|          |─T3─|
0s  2s        12s 14s        24s 26s
    └──10s gap──┘   └──10s gap──┘
```

**When task is slower than period:**

```
Fixed Rate (period = 5s, execution = 10s):
|───Task1────|─Task2──|  (Task2 waits, starts immediately when Task1 done)
0s          10s      20s

Fixed Delay (delay = 5s, execution = 10s):
|───Task1────|     |───Task2────|
0s          10s   15s          25s
             └─5s──┘
```

**Decision table:**

| Use Case | Use This | Why |
|----------|----------|-----|
| Exact timing (monitoring) | Fixed Rate | Maintains schedule |
| API polling (rate-limited) | Fixed Delay | Prevents overload |
| Health checks | Fixed Delay | Wait for completion |
| Metrics collection | Fixed Rate | Consistent intervals |
| Cleanup tasks | Fixed Delay | Won't pile up |

---

### ScheduledFuture Interface

```java
public interface ScheduledFuture<V> extends Delayed, Future<V> {
    long getDelay(TimeUnit unit);
}
```

**Additional method:**
```java
long getDelay(TimeUnit unit);
```

**What it does:**
- Returns remaining delay until execution
- Negative if task overdue (waiting for thread)
- Decreases over time

**Example:**
```java
ScheduledFuture<?> future = scheduler.schedule(() -> {
    System.out.println("Task executed");
}, 10, TimeUnit.SECONDS);

// Check remaining time
while (future.getDelay(TimeUnit.SECONDS) > 0) {
    long remaining = future.getDelay(TimeUnit.SECONDS);
    System.out.println("Executing in " + remaining + " seconds");
    Thread.sleep(1000);
}

// Output:
// Executing in 10 seconds
// Executing in 9 seconds
// ...
// Executing in 1 seconds
// Task executed
```

**Cancelling periodic tasks:**

```java
// Start periodic task
ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Heartbeat");
}, 0, 1, TimeUnit.SECONDS);

// Run for 10 seconds, then stop
Thread.sleep(10000);
future.cancel(false); // Stop periodic execution

// No more heartbeats after this point
```

---

## ThreadPoolExecutor Parameters

### Complete Constructor

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    int corePoolSize,
    int maximumPoolSize,
    long keepAliveTime,
    TimeUnit unit,
    BlockingQueue<Runnable> workQueue,
    ThreadFactory threadFactory,           // Optional
    RejectedExecutionHandler handler       // Optional
);
```

### Parameter Breakdown

#### 1. corePoolSize (Baseline Threads)

**Definition:** Minimum number of threads kept alive (even when idle).

**Behavior:**
- Threads created on-demand (not upfront)
- Kept alive forever (unless `allowCoreThreadTimeOut(true)`)
- New tasks create new threads until corePoolSize reached

**Example:**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, // corePoolSize
    10, 30, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>()
);

// Initially: 0 threads
System.out.println(executor.getPoolSize()); // 0

// Submit first task
executor.submit(() -> doWork());
System.out.println(executor.getPoolSize()); // 1

// Submit 10 more tasks
for (int i = 0; i < 10; i++) {
    executor.submit(() -> doWork());
}
System.out.println(executor.getPoolSize()); // 5 (hits corePoolSize)

// After all tasks complete
Thread.sleep(60000);
System.out.println(executor.getPoolSize()); // Still 5 (core threads stay alive)
```

**Prestarting core threads:**
```java
// Create all core threads immediately
executor.prestartAllCoreThreads();
System.out.println(executor.getPoolSize()); // 5 (all created)

// Or create one core thread
executor.prestartCoreThread();
```

---

#### 2. maximumPoolSize (Peak Capacity)

**Definition:** Maximum number of threads allowed in pool.

**When extras are created:**
- Only when queue is full
- And current threads < maximumPoolSize

**Example:**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                              // core
    5,                              // max
    30, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(3)     // queue size = 3
);

// Submit 10 tasks
for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        Thread.sleep(5000);
    });
}

// Thread creation:
// Task 1-2: Create Thread-1, Thread-2 (core threads)
// Task 3-5: Queue (queue has room)
// Task 6-8: Create Thread-3, Thread-4, Thread-5 (queue full, create extras)
// Task 9-10: Rejected (max reached, queue full)

System.out.println(executor.getPoolSize()); // 5 (max reached)
```

**With unbounded queue (LinkedBlockingQueue):**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 10, 30, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>()  // Unbounded
);

// Submit 1000 tasks
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> Thread.sleep(5000));
}

System.out.println(executor.getPoolSize()); // 2 (never exceeds core!)
System.out.println(executor.getQueue().size()); // 998

// maximumPoolSize is USELESS with unbounded queue!
```

---

#### 3. keepAliveTime (Thread Timeout)

**Definition:** How long extra threads (above core) stay alive when idle.

**Applies to:**
- By default: Only threads above corePoolSize
- If `allowCoreThreadTimeOut(true)`: Also core threads

**Example:**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 5, 10, TimeUnit.SECONDS,  // 10 second timeout
    new ArrayBlockingQueue<>(3)
);

// Create scenario with 5 threads
submitTasksToMax(executor);

System.out.println(executor.getPoolSize()); // 5 threads

// Wait 15 seconds (longer than keepAliveTime)
Thread.sleep(15000);

System.out.println(executor.getPoolSize()); // 2 (extras terminated, core remains)
```

**Allowing core thread timeout:**
```java
executor.allowCoreThreadTimeOut(true);

// Now ALL threads can timeout
Thread.sleep(15000);
System.out.println(executor.getPoolSize()); // 0 (all terminated)
```

**Setting to zero (threads never timeout):**
```java
new ThreadPoolExecutor(
    2, 5, 
    0, TimeUnit.MILLISECONDS,  // 0 means no timeout
    queue
);

// Extra threads stay alive forever (like core threads)
```

---

#### 4. workQueue (Task Queue)

**Definition:** Queue that holds tasks waiting for available thread.

**Common implementations:**

**ArrayBlockingQueue (Bounded):**
```java
new ArrayBlockingQueue<>(100); // Max 100 tasks in queue
```
- Fixed capacity
- Tasks rejected when full
- Triggers extra thread creation

**LinkedBlockingQueue (Unbounded):**
```java
new LinkedBlockingQueue<>(); // No limit (dangerous!)
```
- Grows indefinitely
- Never triggers extra threads
- Can cause OutOfMemoryError

**SynchronousQueue (No storage):**
```java
new SynchronousQueue<>(); // Capacity = 0
```
- No buffering
- Must hand off directly to thread
- Creates new thread or rejects

**PriorityBlockingQueue (Priority-based):**
```java
PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>(
    11, 
    Comparator.comparing(r -> ((PriorityTask) r).priority)
);
```
- Tasks ordered by priority
- Unbounded

---

#### 5. ThreadFactory (Thread Creator)

**Definition:** Factory for creating new threads.

**Default implementation:**
```java
Executors.defaultThreadFactory()
```

**Custom implementation:**
```java
ThreadFactory factory = new ThreadFactory() {
    private AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("MyPool-" + counter.incrementAndGet());
        t.setDaemon(false);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
};

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 5, 30, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10),
    factory  // Custom thread factory
);
```

**Use cases:**
```java
// 1. Custom naming
ThreadFactory naming = r -> new Thread(r, "Worker-" + System.currentTimeMillis());

// 2. Daemon threads
ThreadFactory daemon = r -> {
    Thread t = new Thread(r);
    t.setDaemon(true);  // JVM can exit even if these threads running
    return t;
};

// 3. Exception handling
ThreadFactory withHandler = r -> {
    Thread t = new Thread(r);
    t.setUncaughtExceptionHandler((thread, ex) -> {
        System.err.println("Thread " + thread.getName() + " threw: " + ex);
    });
    return t;
};

// 4. Thread groups
ThreadGroup group = new ThreadGroup("MyGroup");
ThreadFactory grouped = r -> new Thread(group, r);
```

---

#### 6. RejectedExecutionHandler (Rejection Policy)

**Definition:** What to do when task cannot be accepted.

**When rejection occurs:**
- Executor is shutdown
- AND/OR maximum threads reached AND queue is full

**Built-in policies:**

**AbortPolicy (default):**
```java
new ThreadPoolExecutor.AbortPolicy()
```
- Throws RejectedExecutionException
- Caller must handle

**CallerRunsPolicy:**
```java
new ThreadPoolExecutor.CallerRunsPolicy()
```
- Caller thread executes task
- Provides backpressure

**DiscardPolicy:**
```java
new ThreadPoolExecutor.DiscardPolicy()
```
- Silently discards task
- No exception, no execution

**DiscardOldestPolicy:**
```java
new ThreadPoolExecutor.DiscardOldestPolicy()
```
- Removes oldest queued task
- Adds new task

**Custom policy:**
```java
RejectedExecutionHandler custom = (task, executor) -> {
    // Log rejection
    logger.warn("Task rejected: " + task);
    
    // Add to backup queue
    backupQueue.offer(task);
    
    // Alert monitoring
    metrics.incrementRejections();
    
    // Optionally retry
    retryLater(task);
};
```

---

## Execution Flow (Step-by-Step)

### Decision Tree

```
                    Task Submitted
                         ↓
          ┌──────────────┴──────────────┐
          ↓                             ↓
    Are there < corePoolSize       Executor shutdown?
         threads?                         ↓
          ↓                            YES → Reject
         YES                              
          ↓                              
    Create new core thread               
    Execute task immediately             
         DONE                            
          ↓                              
         NO                              
          ↓                              
    Can add to queue?                    
          ↓                              
         YES                             
          ↓                              
    Add to queue                         
    Thread will pick it up               
         DONE                            
          ↓                              
         NO (queue full)                 
          ↓                              
    Are there < maxPoolSize              
         threads?                        
          ↓                              
         YES                             
          ↓                              
    Create new extra thread              
    Execute task immediately             
         DONE                            
          ↓                              
         NO                              
          ↓                              
    Invoke rejection handler             
         DONE                            
```

### Detailed Example

**Configuration:**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                              // corePoolSize
    4,                              // maximumPoolSize
    30, TimeUnit.SECONDS,           // keepAliveTime
    new ArrayBlockingQueue<>(3),    // queue capacity = 3
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

**Submit 10 tasks (each takes 10 seconds):**

```java
for (int i = 1; i <= 10; i++) {
    final int taskId = i;
    System.out.println("Submitting Task-" + taskId);
    executor.submit(() -> {
        System.out.println("Task-" + taskId + " started by " + 
                         Thread.currentThread().getName());
        Thread.sleep(10000);
        System.out.println("Task-" + taskId + " completed");
    });
    Thread.sleep(100); // Small delay for clarity
}
```

**Execution Timeline:**

```
Time: 0ms - Submit Task-1
  Pool: 0 threads
  Decision: 0 < corePoolSize (2)
  Action: Create Thread-1
  Result: Thread-1 executes Task-1
  Status: Pool=1, Queue=0, Active=1

Time: 100ms - Submit Task-2
  Pool: 1 thread (Thread-1 busy)
  Decision: 1 < corePoolSize (2)
  Action: Create Thread-2
  Result: Thread-2 executes Task-2
  Status: Pool=2, Queue=0, Active=2

Time: 200ms - Submit Task-3
  Pool: 2 threads (both busy)
  Decision: 2 = corePoolSize, check queue
  Queue: Empty (0/3)
  Action: Add Task-3 to queue
  Result: Task-3 waits in queue
  Status: Pool=2, Queue=1, Active=2

Time: 300ms - Submit Task-4
  Decision: Queue not full (1/3)
  Action: Add Task-4 to queue
  Status: Pool=2, Queue=2, Active=2

Time: 400ms - Submit Task-5
  Decision: Queue not full (2/3)
  Action: Add Task-5 to queue
  Status: Pool=2, Queue=3, Active=2 (QUEUE FULL!)

Time: 500ms - Submit Task-6
  Pool: 2 threads
  Queue: FULL (3/3)
  Decision: 2 < maxPoolSize (4)
  Action: Create Thread-3 (extra thread)
  Result: Thread-3 executes Task-6
  Status: Pool=3, Queue=3, Active=3

Time: 600ms - Submit Task-7
  Pool: 3 threads (all busy)
  Queue: FULL
  Decision: 3 < maxPoolSize (4)
  Action: Create Thread-4 (extra thread)
  Result: Thread-4 executes Task-7
  Status: Pool=4, Queue=3, Active=4 (MAX REACHED!)

Time: 700ms - Submit Task-8
  Pool: 4 threads (MAX, all busy)
  Queue: FULL
  Decision: Cannot create more threads
  Action: CallerRunsPolicy triggered
  Result: Main thread executes Task-8
  Status: Pool=4, Queue=3, Active=4, Main thread busy

Time: 10700ms - Task-8 completes (main thread)
Time: 800ms - Submit Task-9
  Pool: Still maxed out
  Decision: Reject
  Action: CallerRunsPolicy
  Result: Main thread executes Task-9

Time: 20800ms - Task-9 completes
Time: 900ms - Submit Task-10
  Pool: Still maxed out
  Decision: Reject
  Action: CallerRunsPolicy
  Result: Main thread executes Task-10

Time: 10000ms - Thread-1 finishes Task-1
  Action: Thread-1 checks queue
  Queue: [Task-3, Task-4, Task-5]
  Result: Thread-1 picks Task-3
  Status: Pool=4, Queue=2, Active=4

Time: 10100ms - Thread-2 finishes Task-2
  Action: Thread-2 checks queue
  Queue: [Task-4, Task-5]
  Result: Thread-2 picks Task-4
  Status: Pool=4, Queue=1, Active=4

... and so on
```

**Final output:**
```
Task-1 started by Thread-1
Task-2 started by Thread-2
Task-6 started by Thread-3
Task-7 started by Thread-4
Task-8 started by main         (CallerRunsPolicy!)
Task-1 completed
Task-3 started by Thread-1     (from queue)
Task-2 completed
Task-4 started by Thread-2     (from queue)
...
```

---
