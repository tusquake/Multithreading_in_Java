# Java Semaphore - Concise Guide

## What is Semaphore?

Semaphore controls access to a shared resource by limiting the number of threads that can access it simultaneously.

**Think of it as:** Parking lot with limited spots. Only N cars can park at a time.

---

## Real-World Analogy: Restaurant Tables

**Scenario:** Restaurant with 3 tables

```
5 customers arrive:
Customer-1 → Gets table-1 ✅
Customer-2 → Gets table-2 ✅
Customer-3 → Gets table-3 ✅
Customer-4 → Waits outside ⏳ (no tables available)
Customer-5 → Waits outside ⏳

Customer-1 finishes → Leaves
Customer-4 → Gets table-1 ✅

Customer-2 finishes → Leaves
Customer-5 → Gets table-2 ✅
```

**Semaphore = Restaurant Manager** controlling table access.

---

## Problem Without Semaphore

```java
// Shared resource: Database connections (max 3)
public class ConnectionPool {
    private List<Connection> connections = new ArrayList<>(3);
    
    public Connection getConnection() {
        if (connections.size() < 3) {
            return new Connection(); // Problem: What if 10 threads call this?
        }
        return null; // Race condition!
    }
}
```

**Problem:** 10 threads might all pass the check and create 10 connections!

---

## Solution With Semaphore

```java
import java.util.concurrent.Semaphore;

public class ConnectionPool {
    private Semaphore semaphore = new Semaphore(3); // Max 3 permits
    
    public void useConnection() {
        try {
            semaphore.acquire(); // Get permit (blocks if none available)
            System.out.println(Thread.currentThread().getName() + " got connection");
            
            // Use connection
            Thread.sleep(2000);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release(); // Return permit
            System.out.println(Thread.currentThread().getName() + " released connection");
        }
    }
}
```

---

## Key Methods

### 1. acquire() - Get Permit

```java
semaphore.acquire(); // Blocks if no permits available
```

**Behavior:**
- If permits available → Take one, continue
- If no permits → Block until one is released

### 2. release() - Return Permit

```java
semaphore.release(); // Makes permit available
```

**Always use in finally block:**
```java
semaphore.acquire();
try {
    // Use resource
} finally {
    semaphore.release(); // Guaranteed to execute
}
```

### 3. tryAcquire() - Non-blocking

```java
if (semaphore.tryAcquire()) {
    try {
        // Use resource
    } finally {
        semaphore.release();
    }
} else {
    System.out.println("No permits available, skipping");
}
```

### 4. tryAcquire(timeout) - Timed Wait

```java
if (semaphore.tryAcquire(2, TimeUnit.SECONDS)) {
    try {
        // Use resource
    } finally {
        semaphore.release();
    }
} else {
    System.out.println("Timeout: Could not acquire permit");
}
```

---

## Complete Example: Database Connection Pool

```java
import java.util.concurrent.Semaphore;

public class DatabaseConnectionPool {
    
    // Only 3 connections allowed simultaneously
    private static final Semaphore semaphore = new Semaphore(3);
    
    public static void accessDatabase(int userId) {
        try {
            System.out.println("User-" + userId + " waiting for connection...");
            
            semaphore.acquire(); // Get permit
            System.out.println("User-" + userId + " got connection!");
            
            // Simulate database work
            System.out.println("User-" + userId + " using database...");
            Thread.sleep(2000);
            
            System.out.println("User-" + userId + " done with database");
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release(); // Always release
            System.out.println("User-" + userId + " released connection");
        }
    }
    
    public static void main(String[] args) {
        // 10 users try to access database (but only 3 connections available)
        for (int i = 1; i <= 10; i++) {
            int userId = i;
            new Thread(() -> accessDatabase(userId)).start();
        }
    }
}
```

**Output:**
```
User-1 waiting for connection...
User-2 waiting for connection...
User-3 waiting for connection...
User-4 waiting for connection...
User-5 waiting for connection...

User-1 got connection!
User-2 got connection!
User-3 got connection!

User-1 using database...
User-2 using database...
User-3 using database...

User-4 waiting...
User-5 waiting...

User-1 done with database
User-1 released connection

User-4 got connection!
User-4 using database...

(and so on...)
```

---

## Fair vs Unfair Semaphore

### Unfair (Default)

```java
Semaphore semaphore = new Semaphore(3); // Unfair
```

**Behavior:** Threads might not get permits in order (faster but can starve)

### Fair

```java
Semaphore semaphore = new Semaphore(3, true); // Fair (FIFO)
```

**Behavior:** Threads get permits in arrival order (slower but no starvation)

---

## Binary Semaphore (Mutex Alternative)

```java
Semaphore mutex = new Semaphore(1); // Only 1 permit = like a lock

public void criticalSection() {
    try {
        mutex.acquire();
        // Only one thread can be here
        counter++;
    } finally {
        mutex.release();
    }
}
```

**Similar to Lock, but:**
- Lock: Must be released by same thread that acquired it
- Semaphore: Any thread can release (more flexible but dangerous)

---

## Real-World Use Cases

### 1. Connection Pool

```java
Semaphore dbConnections = new Semaphore(10); // Max 10 DB connections
```

### 2. Rate Limiting

```java
Semaphore apiLimiter = new Semaphore(100); // Max 100 requests/second

public void callAPI() {
    if (apiLimiter.tryAcquire()) {
        try {
            // Make API call
        } finally {
            // Release after 1 second
            scheduler.schedule(() -> apiLimiter.release(), 1, TimeUnit.SECONDS);
        }
    }
}
```

### 3. Resource Pool (Printer Queue)

```java
Semaphore printers = new Semaphore(3); // 3 printers available

public void printDocument(String doc) {
    try {
        printers.acquire();
        System.out.println("Printing: " + doc);
        Thread.sleep(3000); // Print time
    } finally {
        printers.release();
    }
}
```

### 4. Parking Lot

```java
Semaphore parkingSpots = new Semaphore(50); // 50 parking spots

public void parkCar() {
    if (parkingSpots.tryAcquire()) {
        System.out.println("Car parked");
        // Park for some time
        parkingSpots.release();
    } else {
        System.out.println("Parking full!");
    }
}
```

---

## Semaphore vs Lock

| Feature | Semaphore | Lock |
|---------|-----------|------|
| **Permits** | N permits (1 or more) | Only 1 |
| **Release by** | Any thread | Same thread that acquired |
| **Use case** | Limit concurrent access | Mutual exclusion |
| **Fairness** | Optional (constructor param) | Optional (ReentrantLock) |
| **Try acquire** | ✅ tryAcquire() | ✅ tryLock() |

**When to use Semaphore:**
- Limit concurrent access to N threads
- Example: Connection pool, thread pool, resource pool

**When to use Lock:**
- Mutual exclusion (only 1 thread)
- Example: Protecting shared variable

---

## Important Points

### 1. Always Release in Finally

```java
semaphore.acquire();
try {
    // Use resource
} finally {
    semaphore.release(); // MUST release
}
```

### 2. Acquire and Release Balance

```java
// Bad: More releases than acquires
semaphore.acquire();
semaphore.release();
semaphore.release(); // ❌ Extra release! Now permits > initial

// Good: Balanced
semaphore.acquire();
semaphore.release(); // ✅
```

### 3. Check Available Permits

```java
int available = semaphore.availablePermits();
System.out.println("Available: " + available);
```

### 4. Drain All Permits

```java
int drained = semaphore.drainPermits(); // Takes all permits
System.out.println("Drained: " + drained);
```

---

## Complete Example: Parking Lot Simulation

```java
import java.util.concurrent.Semaphore;

public class ParkingLot {
    private static final Semaphore parking = new Semaphore(3, true); // 3 spots, fair
    
    public static void main(String[] args) {
        // 8 cars try to park
        for (int i = 1; i <= 8; i++) {
            new Thread(new Car(i)).start();
        }
    }
    
    static class Car implements Runnable {
        private int carNumber;
        
        Car(int carNumber) {
            this.carNumber = carNumber;
        }
        
        @Override
        public void run() {
            try {
                System.out.println("Car-" + carNumber + " arriving at parking lot");
                
                parking.acquire();
                System.out.println("Car-" + carNumber + " parked! (Spots left: " + 
                                 parking.availablePermits() + ")");
                
                // Park for random time (1-4 seconds)
                Thread.sleep((long) (Math.random() * 3000 + 1000));
                
                System.out.println("Car-" + carNumber + " leaving");
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                parking.release();
                System.out.println("Car-" + carNumber + " left. (Spots left: " + 
                                 parking.availablePermits() + ")");
            }
        }
    }
}
```

**Output:**
```
Car-1 arriving at parking lot
Car-2 arriving at parking lot
Car-3 arriving at parking lot
Car-4 arriving at parking lot
Car-5 arriving at parking lot

Car-1 parked! (Spots left: 2)
Car-2 parked! (Spots left: 1)
Car-3 parked! (Spots left: 0)

Car-4 waiting...
Car-5 waiting...

Car-1 leaving
Car-1 left. (Spots left: 1)

Car-4 parked! (Spots left: 0)

Car-2 leaving
Car-2 left. (Spots left: 1)

Car-5 parked! (Spots left: 0)
...
```

---

## Quick Summary

| Concept | Description |
|---------|-------------|
| **Semaphore** | Controls access to limited resources |
| **Permits** | Number of threads allowed simultaneously |
| **acquire()** | Get permit (blocks if unavailable) |
| **release()** | Return permit (must be in finally) |
| **tryAcquire()** | Non-blocking attempt |
| **Fair** | FIFO order (slower but fair) |
| **Unfair** | No order (faster but can starve) |

**Remember:** Semaphore = Bouncer at club with limited capacity. Only N people allowed inside at once!