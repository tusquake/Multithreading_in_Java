# Java Thread Synchronization - Complete Guide

## Table of Contents
1. [Why Synchronization?](#why-synchronization)
2. [The Race Condition Problem](#the-race-condition-problem)
3. [Bank Account Analogy](#bank-account-analogy)
4. [Synchronized Keyword](#synchronized-keyword)
5. [ReentrantLock](#reentrantlock)
6. [Synchronized vs ReentrantLock](#synchronized-vs-reentrantlock)

---

## Why Synchronization?

### The Problem: Shared Resources

Imagine two threads trying to update the same bank account balance simultaneously:

```java
// Shared resource
int balance = 1000;

// Thread-1: Withdraw 500
balance = balance - 500;

// Thread-2: Withdraw 300
balance = balance - 300;
```

**Expected Result:** balance = 200 (1000 - 500 - 300)
**Actual Result:** Could be 500 or 700 or even 1000! (Race condition)

---

## The Race Condition Problem

### Step-by-Step Breakdown

Let's see what happens at CPU instruction level:

```java
balance = balance - 500;
```

This simple line actually involves 3 steps:
1. **READ:** Load balance value from memory (1000)
2. **MODIFY:** Subtract 500 (1000 - 500 = 500)
3. **WRITE:** Store result back to memory (500)

### The Race Condition Scenario

**Timeline:**

```
Time  | Thread-1 (Withdraw 500)      | Thread-2 (Withdraw 300)      | Balance
------|------------------------------|------------------------------|----------
T0    | -                            | -                            | 1000
T1    | READ balance (1000)          | -                            | 1000
T2    | MODIFY: 1000 - 500 = 500     | -                            | 1000
T3    | -                            | READ balance (1000)          | 1000
T4    | -                            | MODIFY: 1000 - 300 = 700     | 1000
T5    | WRITE 500                    | -                            | 500
T6    | -                            | WRITE 700                    | 700 ❌
```

**Problem:** Thread-2's write overwrites Thread-1's write. Lost update!

**Expected:** 200 (both withdrawals)
**Actual:** 700 (only Thread-2's withdrawal counted)

---

## Bank Account Analogy

### Without Synchronization (Chaos)

**Scenario:** Two ATMs processing withdrawals from same account simultaneously.

```
ATM-1 (Mumbai):                ATM-2 (Delhi):
Customer wants ₹500            Customer wants ₹300
Check balance: ₹1000 ✓         Check balance: ₹1000 ✓
Dispense ₹500                  Dispense ₹300
Update: ₹1000 - ₹500 = ₹500    Update: ₹1000 - ₹300 = ₹700
Final balance: ₹700 ❌         Final balance: ₹700 ❌

Problem: ₹800 withdrawn but balance shows ₹700!
Bank lost ₹100!
```

### With Synchronization (Controlled)

```
ATM-1 (Mumbai):                ATM-2 (Delhi):
Acquire LOCK                   Waiting... (blocked)
Check balance: ₹1000 ✓
Dispense ₹500
Update: ₹500
Release LOCK                   
                               Acquire LOCK
                               Check balance: ₹500 ✓
                               Dispense ₹300
                               Update: ₹200
                               Release LOCK

Final balance: ₹200 ✓ Correct!
```

---

## Synchronized Keyword

### What is Synchronization?

Synchronization ensures that only **one thread** can access a shared resource at a time using locks.

**Key Concept:** Every object in Java has an **intrinsic lock** (monitor).

### 1. Synchronized Method

```java
public class BankAccount {
    private int balance = 1000;
    
    // Synchronized method - uses 'this' object's lock
    public synchronized void withdraw(int amount) {
        System.out.println(Thread.currentThread().getName() + 
                         " attempting to withdraw " + amount);
        
        if (balance >= amount) {
            System.out.println(Thread.currentThread().getName() + 
                             " checking balance: " + balance);
            
            // Simulate processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            balance -= amount;
            System.out.println(Thread.currentThread().getName() + 
                             " completed. New balance: " + balance);
        } else {
            System.out.println(Thread.currentThread().getName() + 
                             " insufficient funds!");
        }
    }
    
    public synchronized int getBalance() {
        return balance;
    }
}
```

**How it Works:**

```
Thread-1 calls withdraw(500):
1. Thread-1 acquires lock on BankAccount object
2. Thread-1 executes method
3. Thread-1 releases lock when method completes

Thread-2 calls withdraw(300) while Thread-1 inside:
1. Thread-2 tries to acquire lock
2. Lock already held by Thread-1
3. Thread-2 BLOCKS (waits)
4. When Thread-1 releases lock, Thread-2 acquires it
5. Thread-2 executes method
```

### 2. Synchronized Block

More fine-grained control than synchronized method:

```java
public class BankAccount {
    private int balance = 1000;
    private Object lock = new Object(); // Custom lock object
    
    public void withdraw(int amount) {
        // Non-critical code (no lock needed)
        System.out.println("Preparing to withdraw...");
        
        // Critical section - needs synchronization
        synchronized(lock) {
            if (balance >= amount) {
                balance -= amount;
                System.out.println("Withdrawn: " + amount);
            }
        }
        
        // Non-critical code
        System.out.println("Transaction complete");
    }
}
```

**Advantages:**
- Lock only the critical section
- Better performance (less blocking)
- Can use custom lock objects

### 3. Synchronized Static Method

```java
public class Counter {
    private static int count = 0;
    
    // Locks the Counter.class object
    public static synchronized void increment() {
        count++;
    }
}
```

**Difference:**
- Synchronized method → Locks instance (`this`)
- Synchronized static method → Locks class object (`Counter.class`)

---

## ReentrantLock

### What is ReentrantLock?

ReentrantLock is an explicit lock mechanism providing more features than `synchronized`.

**"Reentrant" means:** A thread can acquire the same lock multiple times without deadlocking itself.

### Basic Usage

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BankAccount {
    private int balance = 1000;
    private Lock lock = new ReentrantLock();
    
    public void withdraw(int amount) {
        lock.lock(); // Acquire lock
        try {
            System.out.println(Thread.currentThread().getName() + 
                             " acquired lock");
            
            if (balance >= amount) {
                System.out.println("Balance before: " + balance);
                Thread.sleep(100); // Simulate processing
                balance -= amount;
                System.out.println("Balance after: " + balance);
            } else {
                System.out.println("Insufficient funds");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock(); // Always release in finally!
            System.out.println(Thread.currentThread().getName() + 
                             " released lock");
        }
    }
}
```

**Critical Points:**

1. **Always use try-finally:**
```java
lock.lock();
try {
    // Critical section
} finally {
    lock.unlock(); // Guaranteed to execute
}
```

2. **Why finally?** If exception occurs in critical section, lock must still be released!

---

## Understanding Reentrant

### What Does "Reentrant" Mean?

A lock is reentrant if a thread can acquire it multiple times.

### Example Without Reentrancy (Hypothetical)

```java
public void methodA() {
    lock.lock();
    try {
        methodB(); // Calls another method that needs same lock
    } finally {
        lock.unlock();
    }
}

public void methodB() {
    lock.lock(); // DEADLOCK! Can't acquire lock it already holds
    try {
        // Do work
    } finally {
        lock.unlock();
    }
}
```

### Example With Reentrancy (ReentrantLock)

```java
public class ReentrantExample {
    private ReentrantLock lock = new ReentrantLock();
    
    public void outerMethod() {
        lock.lock(); // Hold count = 1
        try {
            System.out.println("Outer method");
            innerMethod(); // Calls method that also locks
        } finally {
            lock.unlock(); // Hold count = 0
        }
    }
    
    public void innerMethod() {
        lock.lock(); // Hold count = 2 (same thread!)
        try {
            System.out.println("Inner method");
        } finally {
            lock.unlock(); // Hold count = 1
        }
    }
}
```

**How it Works:**

```
Thread-1 calls outerMethod():
1. lock.lock() → Hold count = 1
2. Calls innerMethod()
3. lock.lock() → Hold count = 2 (allowed! same thread)
4. Exit innerMethod → lock.unlock() → Hold count = 1
5. Exit outerMethod → lock.unlock() → Hold count = 0 (lock released)
```

**Rule:** Lock must be unlocked same number of times it was locked.

---

## ReentrantLock Advanced Features

### 1. tryLock() - Non-Blocking Attempt

```java
public class BankAccount {
    private int balance = 1000;
    private ReentrantLock lock = new ReentrantLock();
    
    public boolean withdraw(int amount) {
        if (lock.tryLock()) { // Try to acquire, don't block
            try {
                if (balance >= amount) {
                    balance -= amount;
                    System.out.println("Withdrawn: " + amount);
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Could not acquire lock, try later");
            return false;
        }
    }
}
```

**Use Case:**
- Don't want thread to block
- Can do alternative work if lock not available
- Example: Try processing, if locked, process next item in queue

### 2. tryLock(timeout) - Timed Waiting

```java
public boolean withdraw(int amount) {
    try {
        if (lock.tryLock(2, TimeUnit.SECONDS)) { // Wait max 2 seconds
            try {
                if (balance >= amount) {
                    balance -= amount;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Timeout: Could not acquire lock in 2 seconds");
            return false;
        }
    } catch (InterruptedException e) {
        System.out.println("Interrupted while waiting for lock");
        return false;
    }
}
```

**Use Case:**
- Set maximum wait time
- Prevent indefinite blocking
- Good for user-facing operations (don't hang forever)

### 3. lockInterruptibly() - Respond to Interruption

```java
public void withdraw(int amount) throws InterruptedException {
    lock.lockInterruptibly(); // Can be interrupted while waiting
    try {
        balance -= amount;
    } finally {
        lock.unlock();
    }
}
```

**Difference from lock():**

| Method | Can be Interrupted? | Throws InterruptedException? |
|--------|---------------------|------------------------------|
| lock() | NO | NO |
| lockInterruptibly() | YES | YES |

**Use Case:** Long-running operations that should be cancellable

### 4. Fair vs Unfair Locks

```java
// Unfair lock (default) - faster but can starve threads
ReentrantLock unfairLock = new ReentrantLock();

// Fair lock - guarantees FIFO order
ReentrantLock fairLock = new ReentrantLock(true);
```

**Unfair Lock Behavior:**
```
Queue: [Thread-2, Thread-3, Thread-4]
Thread-1 releases lock
Thread-5 arrives and immediately grabs lock (barging!)
Queue: [Thread-2, Thread-3, Thread-4] still waiting
```

**Fair Lock Behavior:**
```
Queue: [Thread-2, Thread-3, Thread-4]
Thread-1 releases lock
Thread-2 (first in queue) gets lock
Queue: [Thread-3, Thread-4]
```

**Trade-offs:**

| Lock Type | Pros | Cons |
|-----------|------|------|
| **Unfair** | Faster (less context switching) | Can starve threads |
| **Fair** | No starvation, predictable | Slower (more overhead) |

---

## Synchronized vs ReentrantLock

### Feature Comparison

| Feature | synchronized | ReentrantLock |
|---------|--------------|---------------|
| **Ease of use** | Simple, automatic | Explicit lock/unlock required |
| **Lock release** | Automatic on exception | Must use try-finally |
| **Try lock** | ❌ No | ✅ Yes (tryLock) |
| **Timed lock** | ❌ No | ✅ Yes (tryLock with timeout) |
| **Interruptible** | ❌ No | ✅ Yes (lockInterruptibly) |
| **Fair lock** | ❌ No | ✅ Yes (constructor param) |
| **Hold count** | ❌ No info | ✅ Yes (getHoldCount) |
| **Lock status** | ❌ No info | ✅ Yes (isLocked, isHeldByCurrentThread) |
| **Performance** | Slightly faster | Slightly slower |
| **Code clarity** | More readable | More verbose |

### When to Use synchronized

✅ **Use synchronized when:**
- Simple locking needs
- Don't need advanced features
- Want cleaner code
- JVM handles everything automatically

```java
public synchronized void simpleMethod() {
    // Simple critical section
    counter++;
}
```

**Example:**
```java
public class Counter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int getCount() {
        return count;
    }
}
```

### When to Use ReentrantLock

✅ **Use ReentrantLock when:**
- Need tryLock (non-blocking)
- Need timeout on lock acquisition
- Need interruptible locking
- Need fair locking
- Need lock status information

```java
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try {
        // Critical section with timeout
    } finally {
        lock.unlock();
    }
}
```

**Example:**
```java
public class AdvancedCounter {
    private int count = 0;
    private ReentrantLock lock = new ReentrantLock(true); // Fair lock
    
    public boolean tryIncrement() {
        if (lock.tryLock()) {
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false; // Couldn't acquire lock
    }
    
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

---

## Complete Examples

### Example 1: Synchronized Method

```java
public class BankAccountSync {
    private int balance = 1000;
    
    public synchronized void withdraw(int amount) {
        if (balance >= amount) {
            System.out.println(Thread.currentThread().getName() + 
                             " withdrawing " + amount);
            balance -= amount;
            System.out.println(Thread.currentThread().getName() + 
                             " new balance: " + balance);
        } else {
            System.out.println("Insufficient balance");
        }
    }
    
    public static void main(String[] args) {
        BankAccountSync account = new BankAccountSync();
        
        Thread t1 = new Thread(() -> account.withdraw(500), "Thread-1");
        Thread t2 = new Thread(() -> account.withdraw(300), "Thread-2");
        Thread t3 = new Thread(() -> account.withdraw(400), "Thread-3");
        
        t1.start();
        t2.start();
        t3.start();
    }
}
```

**Output:**
```
Thread-1 withdrawing 500
Thread-1 new balance: 500
Thread-2 withdrawing 300
Thread-2 new balance: 200
Thread-3 withdrawing 400
Insufficient balance
```

### Example 2: ReentrantLock with tryLock

```java
public class BankAccountLock {
    private int balance = 1000;
    private ReentrantLock lock = new ReentrantLock();
    
    public void withdraw(int amount) {
        if (lock.tryLock()) {
            try {
                if (balance >= amount) {
                    System.out.println(Thread.currentThread().getName() + 
                                     " withdrawing " + amount);
                    balance -= amount;
                    System.out.println(Thread.currentThread().getName() + 
                                     " new balance: " + balance);
                } else {
                    System.out.println("Insufficient balance");
                }
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println(Thread.currentThread().getName() + 
                             " could not acquire lock, skipping");
        }
    }
    
    public static void main(String[] args) {
        BankAccountLock account = new BankAccountLock();
        
        Thread t1 = new Thread(() -> account.withdraw(500), "Thread-1");
        Thread t2 = new Thread(() -> account.withdraw(300), "Thread-2");
        Thread t3 = new Thread(() -> account.withdraw(400), "Thread-3");
        
        t1.start();
        t2.start();
        t3.start();
    }
}
```

**Output:**
```
Thread-1 withdrawing 500
Thread-1 new balance: 500
Thread-2 could not acquire lock, skipping
Thread-3 withdrawing 400
Insufficient balance
```

---

## Quick Summary

| Aspect | synchronized | ReentrantLock |
|--------|--------------|---------------|
| **Complexity** | Simple | More complex |
| **Flexibility** | Limited | High |
| **Try lock** | No | Yes |
| **Timeout** | No | Yes |
| **Fairness** | No | Yes |
| **Best for** | Simple cases | Advanced needs |