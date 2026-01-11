# Java Thread Pool Executor - Deep Dive Guide

## Table of Contents
1. [Why Thread Pools?](#why-thread-pools)
2. [Restaurant Analogy (Extended)](#restaurant-analogy-extended)
3. [Core Parameters Explained](#core-parameters-explained)
4. [Execution Flow (Step-by-Step)](#execution-flow-step-by-step)
5. [Queue Types Deep Dive](#queue-types-deep-dive)
6. [Rejection Policies Explained](#rejection-policies-explained)
7. [Thread Lifecycle](#thread-lifecycle)
8. [Real-World Scenarios](#real-world-scenarios)
9. [Performance Tuning](#performance-tuning)
10. [Complete Examples](#complete-examples)

---

## Why Thread Pools?

### The Problem with Creating Threads Manually

```java
// Bad approach - creating new thread for each task
for (int i = 0; i < 1000; i++) {
    new Thread(() -> {
        // Do some work
    }).start();
}
```

**Problems:**
1. **Memory Overhead:** Each thread consumes ~1MB of stack memory
    - 1000 threads = ~1GB RAM!
2. **CPU Overhead:** Creating/destroying threads is expensive
    - Thread creation takes ~1-2ms
3. **Context Switching:** Too many threads = CPU wastes time switching between them
4. **Resource Exhaustion:** Can crash JVM with OutOfMemoryError

### The Solution: Thread Pool

```java
// Good approach - reuse threads
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> {
        // Do some work
    });
}
```

**Benefits:**
1. **Memory Efficient:** Only 10 threads in memory
2. **Fast:** Threads are reused, no creation overhead
3. **Controlled:** Limits concurrent execution
4. **Predictable:** System resources are managed

---

## Restaurant Analogy (Extended)

Let's use a **Pizza Restaurant** to understand every concept:

### The Setup

```
Restaurant Capacity:
- Kitchen has 2 permanent chefs (corePoolSize = 2)
- Can hire up to 4 chefs total during rush hour (maxPoolSize = 4)
- Order board can hold 5 tickets (queue size = 5)
- Temporary chefs leave after 30 seconds of no orders (keepAliveTime = 30s)
```

### Scenario: 10 Customers Arrive

**Customer 1 arrives:**
```
Kitchen Status: Empty
Action: Hire Chef-1 (permanent)
Chef-1: Makes Pizza-1
```

**Customer 2 arrives:**
```
Kitchen Status: Chef-1 busy
Action: Hire Chef-2 (permanent)
Chef-2: Makes Pizza-2
```

**Customers 3-7 arrive (while both chefs are busy):**
```
Kitchen Status: Both chefs busy
Action: Place orders on board (queue)
Order Board: [Pizza-3, Pizza-4, Pizza-5, Pizza-6, Pizza-7]
Chefs: Will pick these up when free
```

**Customer 8 arrives:**
```
Kitchen Status: Both chefs busy, board full
Action: Hire Chef-3 (temporary)
Chef-3: Makes Pizza-8
```

**Customer 9 arrives:**
```
Kitchen Status: All chefs busy, board full
Action: Hire Chef-4 (temporary - last one allowed)
Chef-4: Makes Pizza-9
```

**Customer 10 arrives:**
```
Kitchen Status: 4 chefs busy, board full, can't hire more
Action: CallerRunsPolicy kicks in
Customer-10: Makes their own pizza!
```

**After rush hour:**
```
- Chef-1, Chef-2: Stay in kitchen (permanent)
- Chef-3, Chef-4: Leave after 30 seconds of no work (temporary)
```

---

## Core Parameters Explained

### 1. Core Pool Size (Permanent Staff)

```java
corePoolSize: 2
```

**Deep Explanation:**

Core threads are the **backbone** of your thread pool. They are:
- Created on-demand as tasks arrive
- Kept alive even when idle (by default)
- Not subject to keepAliveTime timeout (by default)

**Creation Timeline:**
```
Task 1 arrives → Create core thread-1
Task 2 arrives → Create core thread-2
Task 3 arrives → core thread-1 or thread-2 handles it (no new thread)
```

**Important:** Core threads are NOT created upfront (unless you call `prestartAllCoreThreads()`).

**Example:**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 4, 30, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(5)
);

// Initially: 0 threads exist
executor.prestartAllCoreThreads(); // Now: 2 threads created and waiting
```

**Why is this useful?**
- Ensures consistent performance baseline
- Avoids thread creation latency for first tasks
- Provides predictable resource usage

---

### 2. Maximum Pool Size (Peak Capacity)

```java
maxPoolSize: 4
```

**Deep Explanation:**

Maximum pool size defines the **absolute limit** of threads. Extra threads (beyond core) are:
- Created only when queue is full
- Temporary workers (subject to keepAliveTime)
- Terminated when idle for too long

**Creation Conditions:**
```
Extra thread is created ONLY if:
1. Current threads = corePoolSize (all core threads created)
2. All core threads are busy
3. Queue is completely full
4. Current threads < maxPoolSize
```

**Real Example:**

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                              // core
    4,                              // max
    30, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(3)     // queue size = 3
);

// Submit 10 tasks
for (int i = 1; i <= 10; i++) {
    executor.submit(() -> Thread.sleep(5000));
}

// Thread creation:
Task 1 → Create Thread-1 (core)
Task 2 → Create Thread-2 (core)
Task 3 → Queue [Task-3]
Task 4 → Queue [Task-3, Task-4]
Task 5 → Queue [Task-3, Task-4, Task-5] (queue full!)
Task 6 → Create Thread-3 (extra) because queue is full
Task 7 → Create Thread-4 (extra)
Task 8 → Rejection policy triggers (can't create more)
```

**Critical Understanding:**

Many developers expect Thread-3 to be created at Task-3, but it's created only when:
- Core threads are maxed out (✓)
- Queue is full (✓)

This design ensures queue is used before creating expensive threads.

---

### 3. Keep Alive Time (Idle Timeout)

```java
keepAliveTime: 30
unit: TimeUnit.SECONDS
```

**Deep Explanation:**

Keep alive time determines how long **extra threads** (above core) stay alive when idle.

**Detailed Flow:**
```
1. Thread-3 (extra thread) completes Task-6
2. Thread-3 checks queue for new tasks
3. Queue is empty
4. Thread-3 waits for 30 seconds
5. If no task arrives in 30 seconds → Thread-3 terminates
6. If task arrives within 30 seconds → Thread-3 handles it, timer resets
```

**Important Points:**

**By default:**
- Applies only to threads above corePoolSize
- Core threads are immortal (never timeout)

**But you can change this:**
```java
executor.allowCoreThreadTimeOut(true);
// Now even core threads can timeout
```

**Use Cases:**

**Short keepAliveTime (10 seconds):**
- Burst workloads (traffic spikes)
- Want to free resources quickly
- Example: Handling web requests during flash sale

**Long keepAliveTime (10 minutes):**
- Frequent but irregular tasks
- Thread creation overhead is significant
- Example: Background processing jobs

**Infinite keepAliveTime (0):**
```java
keepAliveTime: 0
unit: TimeUnit.MILLISECONDS
```
- Extra threads never timeout
- Essentially same as fixed pool

---

### 4. Work Queue (Task Waiting Area)

```java
new ArrayBlockingQueue<>(5)
```

**Deep Explanation:**

The queue is where tasks **wait** when all threads are busy. Choice of queue dramatically affects behavior.

#### Queue Type 1: ArrayBlockingQueue (Bounded)

```java
new ArrayBlockingQueue<>(5) // Fixed capacity: 5 tasks
```

**Characteristics:**
- Fixed size, cannot grow
- Blocks when full (waits for space)
- Fair ordering (FIFO)
- Memory efficient

**Behavior:**
```
Thread Pool: 2 core, 4 max
Queue Size: 5

Submit 10 tasks:
Tasks 1-2: Core threads handle
Tasks 3-7: Queue (5 slots)
Tasks 8-9: Extra threads created
Task 10: Rejected (queue full, threads maxed)
```

**Best For:**
- Known max workload
- Need to limit memory usage
- Want to trigger extra threads when busy

---

#### Queue Type 2: LinkedBlockingQueue (Unbounded)

```java
new LinkedBlockingQueue<>() // No size limit
```

**Characteristics:**
- Can grow indefinitely
- Never "full" (until memory runs out)
- Extra threads never created!

**Behavior:**
```
Thread Pool: 2 core, 4 max
Queue Size: Unbounded

Submit 10 tasks:
Tasks 1-2: Core threads handle
Tasks 3-10: All go to queue
Extra threads: NEVER CREATED!
```

**Critical Point:** With unbounded queue, maxPoolSize is meaningless!

**Best For:**
- Workload is unpredictable
- Tasks can wait indefinitely
- Don't want rejections

**Danger:** Can cause OutOfMemoryError if tasks arrive faster than processed.

---

#### Queue Type 3: SynchronousQueue (Zero Capacity)

```java
new SynchronousQueue<>()
```

**Characteristics:**
- Capacity = 0 (no storage)
- Task must be handed off directly to thread
- If no thread available, creates new one or rejects

**Behavior:**
```
Thread Pool: 2 core, 4 max
Queue Size: 0

Submit 10 tasks:
Task 1: Create Thread-1
Task 2: Create Thread-2
Task 3: Create Thread-3 (no queue to hold it!)
Task 4: Create Thread-4
Task 5: Rejected (maxed out)
```

**Best For:**
- Need immediate processing
- Task latency is critical
- Example: CachedThreadPool uses this

---

#### Queue Type 4: PriorityBlockingQueue (Priority-based)

```java
PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>(
    10,
    Comparator.comparing(Task::getPriority)
);
```

**Characteristics:**
- Tasks sorted by priority
- High priority tasks processed first
- Unbounded (can grow)

**Example:**
```java
class Task implements Runnable, Comparable<Task> {
    int priority;
    
    public int compareTo(Task other) {
        return Integer.compare(other.priority, this.priority); // Higher first
    }
}

// Submit tasks
executor.submit(new Task(priority: 1)); // Low
executor.submit(new Task(priority: 10)); // High priority handled first
```

---

### 5. Rejection Policies (Overflow Handling)

```java
new ThreadPoolExecutor.CallerRunsPolicy()
```

**Deep Explanation:**

Rejection happens when:
- All threads are busy
- Queue is full
- Cannot create more threads (reached max)

#### Policy 1: CallerRunsPolicy (Caller Does the Work)

```java
new ThreadPoolExecutor.CallerRunsPolicy()
```

**Behavior:**
```
Main Thread submits Task-10
→ Pool is full
→ Main Thread executes Task-10 itself
```

**Example:**
```java
public static void main(String[] args) {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
    
    // This runs in main thread if rejected!
    executor.submit(() -> {
        System.out.println("Running in: " + Thread.currentThread().getName());
        // Output might be: "Running in: main"
    });
}
```

**Pros:**
- No task is lost
- Provides backpressure (slows down submitter)
- Good for must-process scenarios

**Cons:**
- Caller thread blocked (can't submit more tasks)
- Can slow down entire application

**Use Case:** Critical tasks that must complete (e.g., payment processing)

---

#### Policy 2: AbortPolicy (Throw Exception)

```java
new ThreadPoolExecutor.AbortPolicy() // Default policy
```

**Behavior:**
```
Task submitted when pool full
→ Throws RejectedExecutionException
→ Task is lost
```

**Example:**
```java
try {
    executor.submit(() -> { /* task */ });
} catch (RejectedExecutionException e) {
    System.out.println("Task rejected! Pool is full.");
    // Log to monitoring system
    // Retry later or alert ops team
}
```

**Pros:**
- Fails fast
- Caller is notified immediately
- Can implement custom retry logic

**Cons:**
- Must handle exception
- Task is lost if not caught

**Use Case:** When task loss is acceptable, or you have retry mechanism

---

#### Policy 3: DiscardPolicy (Silent Drop)

```java
new ThreadPoolExecutor.DiscardPolicy()
```

**Behavior:**
```
Task submitted when pool full
→ Silently discarded
→ No exception, no execution
```

**Example:**
```java
executor.submit(() -> {
    sendAnalyticsEvent(); // Might never run!
});
// No error, no indication task was dropped
```

**Pros:**
- No exception handling needed
- Doesn't block caller

**Cons:**
- Silent failure (dangerous!)
- No way to know task was dropped
- Can hide problems

**Use Case:** Non-critical tasks (analytics, logging) where occasional loss is OK

---

#### Policy 4: DiscardOldestPolicy (FIFO Eviction)

```java
new ThreadPoolExecutor.DiscardOldestPolicy()
```

**Behavior:**
```
Task submitted when pool full
→ Removes oldest task from queue
→ Adds new task to queue
```

**Example:**
```
Queue: [Task-3, Task-4, Task-5]
Submit Task-10
→ Remove Task-3 (oldest)
→ Queue: [Task-4, Task-5, Task-10]
```

**Pros:**
- Always processes latest data
- Good for time-sensitive tasks

**Cons:**
- Old tasks are lost
- Can starve long-waiting tasks

**Use Case:** Real-time data (stock prices, sensor readings) where latest value matters most

---

#### Custom Policy

```java
RejectedExecutionHandler customPolicy = (task, executor) -> {
    System.out.println("Task rejected, adding to retry queue");
    retryQueue.add(task);
    alertMonitoring("ThreadPool overloaded!");
};

executor.setRejectedExecutionHandler(customPolicy);
```

---

## Execution Flow (Step-by-Step)

Let's trace the execution of 10 tasks with detailed explanations:

### Configuration
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                               // corePoolSize
    4,                               // maxPoolSize
    30, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(5),    // queue capacity
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

### Task Submission Timeline

**Time: 0ms - Submit Task-1**
```
Pool Status:
- Active threads: 0
- Queue: []

Decision Tree:
1. Are there < corePoolSize threads? YES (0 < 2)
2. Create new core thread

Action: Create Thread-1
Result: Thread-1 executes Task-1

Pool Status After:
- Active threads: 1 (Thread-1)
- Queue: []
```

**Time: 10ms - Submit Task-2**
```
Pool Status:
- Active threads: 1 (Thread-1 busy)
- Queue: []

Decision Tree:
1. Are there < corePoolSize threads? YES (1 < 2)
2. Create new core thread

Action: Create Thread-2
Result: Thread-2 executes Task-2

Pool Status After:
- Active threads: 2 (Thread-1, Thread-2)
- Queue: []
```

**Time: 20ms - Submit Task-3**
```
Pool Status:
- Active threads: 2 (both busy)
- Queue: []

Decision Tree:
1. Are there < corePoolSize threads? NO (2 = 2)
2. Is queue full? NO (0/5)
3. Add to queue

Action: Add Task-3 to queue
Result: Task-3 waits in queue

Pool Status After:
- Active threads: 2
- Queue: [Task-3]
```

**Time: 30-60ms - Submit Tasks 4-7**
```
Same process: All added to queue

Pool Status After Task-7:
- Active threads: 2
- Queue: [Task-3, Task-4, Task-5, Task-6, Task-7] (FULL!)
```

**Time: 70ms - Submit Task-8**
```
Pool Status:
- Active threads: 2 (both busy)
- Queue: [5/5] (FULL!)

Decision Tree:
1. Are there < corePoolSize threads? NO
2. Is queue full? YES
3. Are there < maxPoolSize threads? YES (2 < 4)
4. Create new thread (extra)

Action: Create Thread-3
Result: Thread-3 executes Task-8

Pool Status After:
- Active threads: 3 (Thread-1, Thread-2, Thread-3)
- Queue: [Task-3, Task-4, Task-5, Task-6, Task-7]
```

**Time: 80ms - Submit Task-9**
```
Pool Status:
- Active threads: 3 (all busy)
- Queue: [5/5] (FULL!)

Decision Tree:
1. Are there < maxPoolSize threads? YES (3 < 4)
2. Create last available thread

Action: Create Thread-4
Result: Thread-4 executes Task-9

Pool Status After:
- Active threads: 4 (MAX REACHED!)
- Queue: [Task-3, Task-4, Task-5, Task-6, Task-7]
```

**Time: 90ms - Submit Task-10**
```
Pool Status:
- Active threads: 4 (MAX, all busy)
- Queue: [5/5] (FULL!)

Decision Tree:
1. Are there < maxPoolSize threads? NO (4 = 4)
2. Can add to queue? NO (full)
3. Trigger rejection policy

Action: CallerRunsPolicy → Main thread executes Task-10
Result: Main thread blocked until Task-10 completes

Pool Status After:
- Active threads: 4
- Queue: [Task-3, Task-4, Task-5, Task-6, Task-7]
- Main thread: Executing Task-10
```

**Time: 2090ms - Thread-1 finishes Task-1**
```
Pool Status Before:
- Thread-1: Just finished Task-1
- Queue: [Task-3, Task-4, Task-5, Task-6, Task-7]

Action: Thread-1 polls queue
Result: Thread-1 picks up Task-3 from queue

Pool Status After:
- Active threads: 4 (Thread-1 now on Task-3)
- Queue: [Task-4, Task-5, Task-6, Task-7]
```

**Time: 30000ms - Thread-3 idle for 30 seconds**
```
Pool Status:
- Thread-3: Idle (no tasks in queue)
- keepAliveTime: 30s expired

Decision Tree:
1. Is Thread-3 a core thread? NO
2. Has it been idle for > keepAliveTime? YES
3. Terminate thread

Action: Thread-3 terminates
Result: Pool shrinks back

Pool Status After:
- Active threads: 3 or less
- Thread-3: Terminated
```

---

## Thread Lifecycle

### Complete State Diagram

```
         [NEW]
           ↓
       (start())
           ↓
      [RUNNABLE] ←──────┐
           ↓             │
     (pick task)         │
           ↓             │
      [RUNNING]          │
           ↓             │
    (task completes)     │
           ↓             │
       [IDLE] ───────────┘
           ↓
  (keepAliveTime expires)
           ↓
    [TERMINATED]
```

### Detailed State Explanations

**1. NEW:**
- Thread object created but not started
- Not consuming resources yet

**2. RUNNABLE:**
- Thread started and ready to execute
- Waiting for task from queue

**3. RUNNING:**
- Actively executing a task
- CPU is doing work

**4. IDLE:**
- No task to execute
- Waiting for new task or timeout
- Core threads: Wait forever (unless allowCoreThreadTimeOut = true)
- Extra threads: Wait for keepAliveTime, then terminate

**5. TERMINATED:**
- Thread finished and removed from pool
- Resources freed

---

## Real-World Scenarios

### Scenario 1: Web Server (Tomcat-like)

**Requirements:**
- Handle HTTP requests
- 100 concurrent users
- Each request takes 500ms
- Occasional spikes to 200 users

**Configuration:**
```java
ThreadPoolExecutor webServer = new ThreadPoolExecutor(
    20,                              // corePoolSize: baseline load
    100,                             // maxPoolSize: handle spikes
    60, TimeUnit.SECONDS,            // keepAliveTime: hold threads for 1 min
    new ArrayBlockingQueue<>(50),   // queue: 50 waiting requests
    new ThreadPoolExecutor.CallerRunsPolicy() // backpressure on overload
);
```

**Why these values?**
- **20 core threads:** Handle 40 requests/second (20 threads × 2 req/sec)
- **100 max threads:** Handle 200 req/sec during spikes
- **50 queue:** Buffer for sudden bursts
- **CallerRunsPolicy:** Slows down client if system overloaded

---

### Scenario 2: Background Processing (Email Service)

**Requirements:**
- Send emails in background
- Not time-critical
- Limited SMTP connections
- Process thousands of emails

**Configuration:**
```java
ThreadPoolExecutor emailService = new ThreadPoolExecutor(
    2,                               // corePoolSize: minimal baseline
    5,                               // maxPoolSize: SMTP connection limit
    120, TimeUnit.SECONDS,           // keepAliveTime: hold threads longer
    new LinkedBlockingQueue<>(),    // unbounded: emails can wait
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

**Why these values?**
- **2 core threads:** Save resources, emails not urgent
- **5 max threads:** Match SMTP server connection limit
- **Unbounded queue:** OK for emails to wait in queue
- **120s keepAliveTime:** Email sending is slow, hold threads

---

### Scenario 3: Real-Time Data Processing

**Requirements:**
- Process stock price updates
- Need latest data only
- High frequency updates
- Low latency critical

**Configuration:**
```java
ThreadPoolExecutor stockProcessor = new ThreadPoolExecutor(
    10,                              // corePoolSize: always ready
    10,                              // maxPoolSize: fixed size
    0, TimeUnit.MILLISECONDS,
    new SynchronousQueue<>(),       // zero queue: immediate processing
    new ThreadPoolExecutor.DiscardOldestPolicy() // drop old data
);

executor.allowCoreThreadTimeOut(false); // keep threads alive
```

**Why these values?**
- **Fixed 10 threads:** Predictable performance
- **SynchronousQueue:** No queueing delay
- **DiscardOldestPolicy:** Latest price is what matters

---

## Performance Tuning

### Finding Optimal Thread Count

**Formula:**
```
Optimal Threads = Number of CPUs × (1 + Wait Time / Compute Time)
```

**Example Calculations:**

**CPU-bound task (image processing):**
```
CPUs: 8
Wait Time: 0ms (pure computation)
Compute Time: 100ms
Optimal = 8 × (1 + 0/100) = 8 threads
```

**I/O-bound task (database queries):**
```
CPUs: 8
Wait Time: 900ms (waiting for DB)
Compute Time: 100ms
Optimal = 8 × (1 + 900/100) = 80 threads
```

**Mixed workload (web service):**
```
CPUs: 8
Wait Time: 400ms (network I/O)
Compute Time: 100ms
Optimal = 8 × (1 + 400/100) = 40 threads
```

### Monitoring and Adjustment

```java
ThreadPoolExecutor executor = ...;

// Periodically log metrics
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
monitor.scheduleAtFixedRate(() -> {
    System.out.println("=== Thread Pool Metrics ===");
    System.out.println("Pool Size: " + executor.getPoolSize());
    System.out.println("Active: " + executor.getActiveCount());
    System.out.println("Completed: " + executor.getCompletedTaskCount());
    System.out.println("Queue Size: " + executor.getQueue().size());
    System.out.println("Largest Pool Size: " + executor.getLargestPoolSize());
}, 0, 10, TimeUnit.SECONDS);
```

---

## Complete Examples

### Example 1: Detailed Logging

```java
public class DetailedThreadPoolExample {
    public static void main(String[] args) throws InterruptedException {
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5),
            new ThreadFactory() {
                private AtomicInteger counter = new AtomicInteger(0);
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "Worker-" + counter.incrementAndGet());
                    System.out.println("Created thread: " + t.getName());
                    return t;
                }
            },
            (task, exec) -> {
                System.out.println("REJECTED: Task rejected, running in caller thread");
            }
        );
        
        // Submit 10 tasks with detailed logging
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            
            System.out.println("\n--- Submitting Task-" + taskId + " ---");
            logPoolStatus(executor);
            
            try {
                executor.submit(() -> {
                    System.out.println("Task-" + taskId + " STARTED by " + 
                                     Thread.currentThread().getName());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("Task-" + taskId + " COMPLETED by " + 
                                     Thread.currentThread().getName());
                });
            } catch (RejectedExecutionException e) {
                System.out.println("Task-" + taskId + " was REJECTED");
            }
            
            Thread.sleep(500); // Delay between submissions for clarity
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        System.out.println("\n=== Final Statistics ===");
        logPoolStatus(executor);
    }
    
    static void logPoolStatus(ThreadPoolExecutor executor) {
        System.out.println("Pool Size: " + executor.getPoolSize() + 
                         " | Core: " + executor.getCorePoolSize() +
                         " | Max: " + executor.getMaximumPoolSize());
        System.out.println("Active Threads: " + executor.getActiveCount());
        System.out.println("Queue Size: " + executor.getQueue().size() + 
                         " / " + (executor.getQueue().size() + executor.getQueue().remainingCapacity()));
        System.out.println("Completed Tasks: " + executor.getCompletedTaskCount());
        System.out.println("Total Tasks: " + executor.getTaskCount());
    }
}
```

---

## Summary

**ThreadPoolExecutor is like managing a restaurant kitchen:**

| Parameter | Restaurant | Technical |
|-----------|-----------|-----------|
| **corePoolSize** | Permanent chefs | Always-alive threads |
| **maxPoolSize** | Max chefs during rush | Thread upper limit |
| **keepAliveTime** | Break time before leaving | Idle thread timeout |
| **workQueue** | Order board | Task buffer |
| **rejectionPolicy** | What to do when overwhelmed | Overload handling |

**Key Takeaways:**

1. **Thread pools reuse threads** → Better performance
2. **Core threads are permanent** → Baseline capacity
3. **Extra threads are temporary** → Handle spikes
4. **Queue buffers tasks** → Smooth out bursts
5. **Rejection handles overflow** → Protect system

**Remember:** Choose parameters based on your workload characteristics (CPU-bound vs I/O-bound, burst vs steady, critical vs optional).