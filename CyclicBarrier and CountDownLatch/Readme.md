# CyclicBarrier and CountDownLatch - Simple Guide

## Quick Comparison

| Feature | CountDownLatch | CyclicBarrier |
|---------|---------------|---------------|
| **Reusable** | ❌ One-time use | ✅ Reusable |
| **Who waits** | One or more threads | All threads wait for each other |
| **Count direction** | Down (5→4→3→2→1→0) | Up (wait for N threads) |
| **Use case** | Wait for tasks to complete | Synchronize threads at common point |

---

# CountDownLatch

## What is CountDownLatch?

CountDownLatch makes one or more threads wait until a set of operations (in other threads) completes.

**Think of it as:** Rocket launch countdown. Rocket waits until all checks complete (5, 4, 3, 2, 1, 0 → Launch!)

---

## Real-World Analogy: Airport Departure

**Scenario:** Flight can't take off until all pre-flight checks complete.

```
Flight crew waiting for:
[✓] Fuel check (Engineer-1)
[✓] Safety check (Engineer-2)
[✓] Luggage loaded (Ground crew)
[✓] Catering loaded (Catering team)
[✓] Passengers boarded (Gate agent)

Count: 5 → 4 → 3 → 2 → 1 → 0

✈️ All checks done! Flight takes off!
```

---

## How It Works

```java
import java.util.concurrent.CountDownLatch;

public class CountDownLatchExample {
    public static void main(String[] args) throws InterruptedException {
        
        // Create latch with count = 3
        CountDownLatch latch = new CountDownLatch(3);
        
        // Three worker threads
        new Thread(new Worker(latch, "Worker-1")).start();
        new Thread(new Worker(latch, "Worker-2")).start();
        new Thread(new Worker(latch, "Worker-3")).start();
        
        System.out.println("Main thread waiting for workers...");
        latch.await(); // Wait for count to reach 0
        System.out.println("All workers done! Main thread proceeding");
    }
    
    static class Worker implements Runnable {
        private CountDownLatch latch;
        private String name;
        
        Worker(CountDownLatch latch, String name) {
            this.latch = latch;
            this.name = name;
        }
        
        @Override
        public void run() {
            try {
                System.out.println(name + " working...");
                Thread.sleep(2000); // Simulate work
                System.out.println(name + " finished!");
                latch.countDown(); // Decrement count
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

**Output:**
```
Main thread waiting for workers...
Worker-1 working...
Worker-2 working...
Worker-3 working...

Worker-1 finished!  (Count: 3 → 2)
Worker-2 finished!  (Count: 2 → 1)
Worker-3 finished!  (Count: 1 → 0)

All workers done! Main thread proceeding
```

---

## Key Methods

### 1. countDown() - Decrement Count

```java
latch.countDown(); // Count: 5 → 4
```

**Called by:** Worker threads when they finish

### 2. await() - Wait Until Zero

```java
latch.await(); // Blocks until count reaches 0
```

**Called by:** Thread(s) waiting for all workers to complete

### 3. await(timeout) - Timed Wait

```java
boolean completed = latch.await(5, TimeUnit.SECONDS);
if (completed) {
    System.out.println("All workers finished in time");
} else {
    System.out.println("Timeout! Some workers still running");
}
```

### 4. getCount() - Check Remaining

```java
long remaining = latch.getCount();
System.out.println("Remaining: " + remaining);
```

---

## Real-World Example: Service Startup

```java
public class ServiceStartup {
    public static void main(String[] args) throws InterruptedException {
        
        CountDownLatch latch = new CountDownLatch(3);
        
        // Start 3 services
        new Thread(() -> startService("Database", latch, 3000)).start();
        new Thread(() -> startService("Cache", latch, 2000)).start();
        new Thread(() -> startService("Message Queue", latch, 1000)).start();
        
        System.out.println("Waiting for all services to start...");
        latch.await(); // Wait for all services
        
        System.out.println("\n✅ All services started! Application ready!");
    }
    
    static void startService(String name, CountDownLatch latch, int time) {
        try {
            System.out.println("Starting " + name + "...");
            Thread.sleep(time);
            System.out.println(name + " started! ✓");
            latch.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Output:**
```
Waiting for all services to start...
Starting Database...
Starting Cache...
Starting Message Queue...

Message Queue started! ✓  (Count: 3 → 2)
Cache started! ✓          (Count: 2 → 1)
Database started! ✓       (Count: 1 → 0)

✅ All services started! Application ready!
```

---

## Important Points

**1. One-time use:**
```java
CountDownLatch latch = new CountDownLatch(3);
latch.countDown(); // 3 → 2
latch.countDown(); // 2 → 1
latch.countDown(); // 1 → 0
// Can't be reset! Create new latch for next use
```

**2. Multiple threads can await:**
```java
CountDownLatch latch = new CountDownLatch(3);

// Thread-1 waiting
new Thread(() -> { latch.await(); }).start();

// Thread-2 also waiting
new Thread(() -> { latch.await(); }).start();

// Both released when count reaches 0
```

**3. countDown() doesn't block:**
```java
latch.countDown(); // Returns immediately, doesn't wait
```

---

# CyclicBarrier

## What is CyclicBarrier?

CyclicBarrier makes a group of threads wait for each other at a common barrier point. All threads must arrive before any can proceed.

**Think of it as:** Group photo. Everyone must be in position before taking the photo. After photo, can take another (reusable).

---

## Real-World Analogy: Team Meeting

**Scenario:** 4 team members must all join before meeting starts.

```
Team Members:
Alice arrives → Waiting...
Bob arrives → Waiting...
Charlie arrives → Waiting...
Diana arrives → Everyone here! Meeting starts! 🎉

After meeting ends → Barrier resets → Ready for next meeting
```

---

## How It Works

```java
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierExample {
    public static void main(String[] args) {
        
        // Create barrier for 3 threads with action when all arrive
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("\n🎉 All threads arrived! Barrier reached!\n");
        });
        
        // Three worker threads
        new Thread(new Worker(barrier, "Thread-1")).start();
        new Thread(new Worker(barrier, "Thread-2")).start();
        new Thread(new Worker(barrier, "Thread-3")).start();
    }
    
    static class Worker implements Runnable {
        private CyclicBarrier barrier;
        private String name;
        
        Worker(CyclicBarrier barrier, String name) {
            this.barrier = barrier;
            this.name = name;
        }
        
        @Override
        public void run() {
            try {
                System.out.println(name + " doing work...");
                Thread.sleep((long) (Math.random() * 3000));
                
                System.out.println(name + " reached barrier, waiting for others...");
                barrier.await(); // Wait for all threads
                
                System.out.println(name + " proceeding after barrier!");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

**Output:**
```
Thread-1 doing work...
Thread-2 doing work...
Thread-3 doing work...

Thread-1 reached barrier, waiting for others...
Thread-3 reached barrier, waiting for others...
Thread-2 reached barrier, waiting for others...

🎉 All threads arrived! Barrier reached!

Thread-2 proceeding after barrier!
Thread-1 proceeding after barrier!
Thread-3 proceeding after barrier!
```

---

## Key Methods

### 1. await() - Wait at Barrier

```java
barrier.await(); // Wait for all threads to arrive
```

**Blocks until:** All threads call await()

### 2. await(timeout) - Timed Wait

```java
try {
    barrier.await(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("Not all threads arrived in time");
}
```

### 3. reset() - Reset Barrier

```java
barrier.reset(); // Reset to initial state (if needed manually)
```

### 4. getNumberWaiting() - Check Waiting Threads

```java
int waiting = barrier.getNumberWaiting();
System.out.println("Threads waiting: " + waiting);
```

---

## Real-World Example: Multiplayer Game

```java
public class MultiplayerGame {
    public static void main(String[] args) {
        
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("\n🎮 All players ready! Starting game...\n");
        });
        
        // 3 players
        new Thread(new Player(barrier, "Player-1")).start();
        new Thread(new Player(barrier, "Player-2")).start();
        new Thread(new Player(barrier, "Player-3")).start();
    }
    
    static class Player implements Runnable {
        private CyclicBarrier barrier;
        private String name;
        
        Player(CyclicBarrier barrier, String name) {
            this.barrier = barrier;
            this.name = name;
        }
        
        @Override
        public void run() {
            try {
                // Loading phase
                System.out.println(name + " loading game assets...");
                Thread.sleep((long) (Math.random() * 3000));
                System.out.println(name + " loaded! Waiting for others...");
                
                barrier.await(); // Wait for all players
                
                // Game starts
                System.out.println(name + " playing game!");
                Thread.sleep(2000);
                
                // End of round - wait again
                System.out.println(name + " finished round 1, waiting...");
                barrier.await(); // Reusable! Wait for next round
                
                System.out.println(name + " starting round 2!");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

**Output:**
```
Player-1 loading game assets...
Player-2 loading game assets...
Player-3 loading game assets...

Player-2 loaded! Waiting for others...
Player-1 loaded! Waiting for others...
Player-3 loaded! Waiting for others...

🎮 All players ready! Starting game...

Player-3 playing game!
Player-1 playing game!
Player-2 playing game!

Player-1 finished round 1, waiting...
Player-2 finished round 1, waiting...
Player-3 finished round 1, waiting...

🎮 All players ready! Starting game...

Player-1 starting round 2!
Player-2 starting round 2!
Player-3 starting round 2!
```

---

## Real-World Example: Parallel Data Processing

```java
public class ParallelProcessing {
    public static void main(String[] args) {
        
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("All chunks processed! Merging results...\n");
        });
        
        // Process 3 data chunks in parallel
        new Thread(new DataProcessor(barrier, "Chunk-1", 2000)).start();
        new Thread(new DataProcessor(barrier, "Chunk-2", 3000)).start();
        new Thread(new DataProcessor(barrier, "Chunk-3", 1000)).start();
    }
    
    static class DataProcessor implements Runnable {
        private CyclicBarrier barrier;
        private String chunkName;
        private int processTime;
        
        DataProcessor(CyclicBarrier barrier, String chunkName, int processTime) {
            this.barrier = barrier;
            this.chunkName = chunkName;
            this.processTime = processTime;
        }
        
        @Override
        public void run() {
            try {
                System.out.println("Processing " + chunkName + "...");
                Thread.sleep(processTime);
                System.out.println(chunkName + " processed! Waiting for others...");
                
                barrier.await(); // Wait for all chunks
                
                System.out.println(chunkName + " ready for merge!");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

**Output:**
```
Processing Chunk-1...
Processing Chunk-2...
Processing Chunk-3...

Chunk-3 processed! Waiting for others...
Chunk-1 processed! Waiting for others...
Chunk-2 processed! Waiting for others...

All chunks processed! Merging results...

Chunk-2 ready for merge!
Chunk-1 ready for merge!
Chunk-3 ready for merge!
```

---

## CountDownLatch vs CyclicBarrier

### Visual Comparison

**CountDownLatch:**
```
Main Thread: Waiting...
    ↑
    │ countDown()
    │
Worker-1 → finishes → countDown()
Worker-2 → finishes → countDown()
Worker-3 → finishes → countDown()
    ↓
Main Thread: Proceeds!
```

**CyclicBarrier:**
```
Thread-1 → reaches barrier → waits
Thread-2 → reaches barrier → waits
Thread-3 → reaches barrier → waits
    ↓
All threads released together!
    ↓
Thread-1, Thread-2, Thread-3 proceed
```

---

### When to Use Each

**Use CountDownLatch when:**
- Main thread waits for workers to complete
- One-time coordination
- Example: Wait for services to start

```java
CountDownLatch latch = new CountDownLatch(3);
// Workers countDown when done
// Main thread waits
```

**Use CyclicBarrier when:**
- Threads wait for each other
- Need to repeat coordination
- Example: Multi-phase algorithm, multiplayer game rounds

```java
CyclicBarrier barrier = new CyclicBarrier(3);
// All threads wait at barrier
// Released together
// Can reuse for next phase
```

---

## Key Differences Table

| Feature | CountDownLatch | CyclicBarrier |
|---------|---------------|---------------|
| **Reusable** | ❌ One-time | ✅ Reusable |
| **Who waits** | Separate threads wait | Threads wait for each other |
| **Action on release** | ❌ No | ✅ Optional Runnable |
| **Reset** | ❌ Can't reset | ✅ Auto-reset or manual |
| **Use case** | Initialization, shutdown | Parallel algorithms, games |

---

## Quick Summary

### CountDownLatch

| Concept | Description |
|---------|-------------|
| **Purpose** | Wait for N tasks to complete |
| **Analogy** | Rocket countdown (5, 4, 3, 2, 1, 0 → Launch) |
| **countDown()** | Decrement count (workers call this) |
| **await()** | Wait for count to reach 0 (main thread) |
| **Reusable** | No (one-time use) |
| **Example** | Service startup, parallel downloads |

### CyclicBarrier

| Concept | Description |
|---------|-------------|
| **Purpose** | Threads wait for each other |
| **Analogy** | Group photo (wait for everyone, then snap) |
| **await()** | Wait at barrier (all threads call this) |
| **Barrier action** | Runs when all threads arrive |
| **Reusable** | Yes (automatic reset) |
| **Example** | Multiplayer games, parallel processing |

---

## Memory Tricks

**CountDownLatch:** "Count DOWN to zero, one-time launch"
- Workers count down
- Main waits
- Can't reuse

**CyclicBarrier:** "Cycle through barriers together"
- Everyone waits together
- Released together
- Reusable (cyclic)

**Remember:**
- Latch = Boss waiting for employees to finish
- Barrier = Friends waiting to take group photo together