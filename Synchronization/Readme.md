# Java Thread Synchronization - The Lock & Key

## Table of Contents
1. [The Problem: Data Inconsistency](#the-problem-data-inconsistency)
2. [Analogies](#analogies)
3. [Types of Synchronization](#types-of-synchronization)
    - [Synchronized Method](#synchronized-method)
    - [Synchronized Block](#synchronized-block)
    - [Static Synchronization](#static-synchronization)
4. [Intrinsic Locks (Monitors)](#intrinsic-locks-monitors)
5. [Common Pitfalls](#common-pitfalls)
6. [Common Interview Questions](#common-interview-questions)

---

## The Problem: Data Inconsistency

When multiple threads access shared data (e.g., a counter or balance) simultaneously, they can overwrite each other's changes. This is called a **Race Condition**.

**Example:**
- Thread A reads balance: $100
- Thread B reads balance: $100
- Thread A adds $10 -> writes $110
- Thread B adds $20 -> writes $120
- **Final Result:** $120 (Should have been $130!)

---

## Analogies

### 1. The Single-Occupancy Restroom
Think of a **synchronized method** as a restroom with a lock.
- Only one person (Thread) can use it at a time.
- When they enter, they take the **Lock** (Key).
- Others must wait outside in a queue (**Entry Set**).
- When they leave, they return the lock, and the next person can enter.

---

## Types of Synchronization

### 1. Synchronized Method
Locks the entire method.

```java
public synchronized void increment() {
    count++;
}
```
**Lock used:** The `this` object.

### 2. Synchronized Block
Locks only a specific part of the code (fine-grained control).

```java
public void update() {
    System.out.println("No lock needed here");
    synchronized(this) {
        // Critical Section
        balance += amount;
    }
}
```

### 3. Static Synchronization
Used for static methods.

```java
public static synchronized void log() {
    // Class-level lock
}
```
**Lock used:** The `Class` object (e.g., `MyTable.class`).

---

## Intrinsic Locks (Monitors)

Every object in Java has an **intrinsic lock** associated with it. 
- A thread that needs exclusive access to an object's fields must **acquire** the object's intrinsic lock before accessing them.
- It **releases** the lock when it's done.

---

## Common Pitfalls

### 1. Deadlock
When Thread-1 holds Lock-A and waits for Lock-B, while Thread-2 holds Lock-B and waits for Lock-A. They wait forever.

### 2. Performance Overhead
Synchronization is expensive. Over-synchronizing can slow down the application significantly.

---

## Common Interview Questions

### 1. Can a thread acquire multiple locks?
- Yes, a thread can hold multiple locks at once (e.g., by calling one synchronized method from another).

### 2. What is a "Monitor" in Java?
- A monitor is a synchronization construct that allows threads to have both mutual exclusion and the ability to wait (block) for a certain condition to become true.

### 3. Difference between synchronized method and block?
- Method locks the whole object, while a block can lock a specific part or even a different object, providing better performance.

### 4. If a thread is in a synchronized method, can other threads call non-synchronized methods?
- **Yes.** Non-synchronized methods do not check for locks.
