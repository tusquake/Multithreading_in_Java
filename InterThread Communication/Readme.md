# Inter-Thread Communication - `wait`, `notify`, and `notifyAll`

## Table of Contents
1. [Introduction](#introduction)
2. [Analogy: The Producer-Consumer Cinema](#analogy-the-producer-consumer-cinema)
3. [The Core Trio](#the-core-trio)
    - [wait()](#wait)
    - [notify()](#notify)
    - [notifyAll()](#notifyall)
4. [Complete Example: Movie Booking](#complete-example-movie-booking)
5. [Common Interview Questions](#common-interview-questions)

---

## Introduction

Inter-thread communication allows threads to communicate with each other about a specific condition. Instead of polling (repeatedly checking a variable), threads can efficienty "wait" and be "notified."

---

## Analogy: The Producer-Consumer Cinema

Imagine a **Cinema Ticket Counter**:
- **Consumer (Customer):** Arrives to buy a ticket. If the "Earnings" are not calculated yet, they **Wait** in a corner.
- **Producer (Accountant):** Calculates the total earnings for the day. Once done, they **Notify** the customer that the result is ready.
- **The Result:** The customer can now see the total earnings.

---

## The Core Trio

These methods are part of the `Object` class, not the `Thread` class. They **must** be called from within a `synchronized` context.

### `wait()`
Tells the current thread to release the lock and go to sleep until another thread invokes `notify()`.

### `notify()`
Wakes up a single thread that is waiting on this object's monitor.

### `notifyAll()`
Wakes up all threads that are waiting on this object's monitor.

---

## Complete Example: Movie Booking

### The Calculation Task (`TotalEarnings.java`)
```java
class TotalEarnings extends Thread {
    int total = 0;

    @Override
    public void run() {
        synchronized (this) {
            for (int i = 1; i <= 10; i++) {
                total = total + 100;
            }
            this.notify(); // Calculation done, notify waiting threads
        }
    }
}
```

### The App Logic (`MovieBookApp.java`)
```java
public class MovieBookApp {
    public static void main(String[] args) {
        TotalEarnings te = new TotalEarnings();
        te.start();

        synchronized (te) {
            try {
                // Wait for the calculation to finish
                te.wait();
                System.out.println("Total earnings : " + te.total + " Rs");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

## Common Interview Questions

### 1. Why are `wait`, `notify`, and `notifyAll` defined in `Object` class?
- Because synchronization happens at the **object level**. Every object has a monitor lock, and threads wait on/notify those specific objects.

### 2. Why must these be called from a `synchronized` block?
- To avoid race conditions. A thread must own the monitor of the object it is trying to wait on or notify. Without this, you get an `IllegalMonitorStateException`.

### 3. Difference between `notify()` and `notifyAll()`?
- `notify()` wakes up only one thread (chosen by the scheduler).
- `notifyAll()` wakes up all threads waiting for the lock. It is generally safer to use `notifyAll()`.

### 4. What is the difference between `sleep()` and `wait()`?
- `sleep()` keeps the lock.
- `wait()` releases the lock so other threads can enter the synchronized area.
