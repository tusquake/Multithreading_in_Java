# Java Thread Methods - The Toolbox

## Table of Contents
1. [Introduction](#introduction)
2. [Analogies](#analogies)
3. [Core Methods Deep Dive](#core-methods-deep-dive)
    - [sleep()](#sleep)
    - [yield()](#yield)
    - [join()](#join)
    - [interrupt()](#interrupt)
4. [Daemon Threads](#daemon-threads)
5. [Summary Table](#summary-table)
6. [Common Interview Questions](#common-interview-questions)

---

## Introduction

The `Thread` class provides several methods to manage a thread's lifecycle, state, and interaction with other threads. Understanding these is key to writing efficient multithreaded code.

---

## Analogies

### 1. `sleep()` (The Power Nap)
A thread decides to take a break for a fixed amount of time. It doesn't lose its "locks" but stops using the CPU.

### 2. `yield()` (The Polite Driver)
A thread says, "I'm busy, but if another thread of equal importance needs the CPU, they can go first." It's a hint to the scheduler.

### 3. `join()` (The Dependent Worker)
One thread says, "I can't finish until *that* worker finishes their job. I will wait for them."

### 4. `interrupt()` (The Alarm Clock)
Waking up a thread that is sleeping or waiting.

---

## Core Methods Deep Dive

### `sleep(long millis)`
Causes the currently executing thread to cease execution for the specified period.

```java
try {
    Thread.sleep(2000); // Sleep for 2 seconds
} catch (InterruptedException e) {
    e.printStackTrace();
}
```
**Key Point:** High precision is not guaranteed; it depends on the OS scheduler.

### `yield()`
A hint to the scheduler that the current thread is willing to yield its current use of a processor.

```java
Thread.yield();
```
**Key Point:** It's only a hint. The scheduler is free to ignore it.

### `join()`
Waits for the thread on which it is called to die.

```java
Thread t1 = new Thread(new MyTask());
t1.start();
t1.join(); // Main thread waits until t1 finishes
```

### `interrupt()`
An interrupt is an indication to a thread that it should stop what it is doing and do something else.

```java
Thread t1 = new Thread(() -> {
    try {
        Thread.sleep(10000);
    } catch (InterruptedException e) {
        System.out.println("I was interrupted!");
    }
});
t1.start();
t1.interrupt(); // Wakes up t1
```

---

## Daemon Threads

A **Daemon Thread** is a low-priority thread that runs in the background to perform tasks such as garbage collection.

- **JVM termination:** The JVM exits when only daemon threads remain.
- **Set it:** `t.setDaemon(true);` (Must be called before `start()`).

```java
Thread t = new Thread(() -> {
    while(true) {
        System.out.println("Running in background...");
    }
});
t.setDaemon(true); 
t.start();
```

---

## Summary Table

| Method | Static? | Releases Lock? | Description |
|--------|---------|----------------|-------------|
| `sleep()` | Yes | No | Temporarily pauses execution. |
| `yield()` | Yes | No | Suggests giving CPU to other threads. |
| `join()` | No | No | Waits for another thread to complete. |
| `interrupt()`| No | No | Signal to stop or wake up. |
| `wait()` | No | **Yes** | (Object method) Waits for a signal. |

---

## Common Interview Questions

### 1. Difference between `sleep()` and `wait()`?
- `sleep()` belongs to the `Thread` class, `wait()` belongs to the `Object` class.
- `sleep()` **does NOT** release locks, `wait()` **DOES** release locks.
- `sleep()` is timed; `wait()` can be timed or wait for a `notify()`.

### 2. Can we call `join()` on the current thread?
- Yes, but it results in a deadlock where the current thread waits for itself to finish.

### 3. What is the purpose of `yield()`?
- To prevent one thread from hogging the CPU and allowing other threads of the same priority to run.

### 4. Is `setDaemon(true)` allowed after `start()`?
- No, it throws an `IllegalThreadStateException`.
