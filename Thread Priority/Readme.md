# Thread Priority - The Scheduler's Hint

## Table of Contents
1. [Introduction](#introduction)
2. [Priority Levels](#priority-levels)
3. [The Inheritance Rule](#the-inheritance-rule)
4. [The Big Reality Check](#the-big-reality-check)
5. [Complete Example](#complete-example)
6. [Common Interview Questions](#common-interview-questions)

---

## Introduction

Java allows you to assign a "priority" to a thread. This serves as a hint to the thread scheduler about which threads should be favored for CPU time.

---

## Priority Levels

Thread priorities are integers ranging from **1 to 10**:

- `Thread.MIN_PRIORITY` = 1
- `Thread.NORM_PRIORITY` = 5 (Default)
- `Thread.MAX_PRIORITY` = 10

```java
Thread t1 = new Thread();
t1.setPriority(Thread.MAX_PRIORITY); // Priority 10
t1.setPriority(8); // Custom priority
```

---

## The Inheritance Rule

A child thread **inherits** the priority of its parent thread.

```java
// Main thread priority is 5 by default
Thread.currentThread().setPriority(10);
Thread t1 = new Thread(); // t1 will have priority 10
```

---

## The Big Reality Check

**Priority is NOT a guarantee.**
1. Thread scheduling is **Platform Dependent**. 
2. A high-priority thread might still wait while a low-priority thread runs.
3. Windows, Linux, and Mac handle priorities differently.
4. Designing a system depending strictly on thread priority is considered a **Bad Practice**.

---

## Complete Example

### `ThreadPriorityDemo.java`
```java
class MyThread extends Thread {
    public void run() {
        System.out.println(Thread.currentThread().getName() + 
                         " Priority : " + Thread.currentThread().getPriority());
    }
}

public class ThreadPriorityDemo {
    public static void main(String[] args) {
        // Change Main Thread Priority
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        
        MyThread t1 = new MyThread();
        t1.setPriority(Thread.MIN_PRIORITY);
        
        MyThread t2 = new MyThread();
        t2.setPriority(Thread.NORM_PRIORITY);
        
        t1.start();
        t2.start();
    }
}
```

---

## Common Interview Questions

### 1. What is the default priority of a thread?
- It depends on the parent. For the main thread, it is 5. For others, it matches whoever created them.

### 2. Can we set priority to 11?
- No. It throws `IllegalArgumentException`. The range is 1-10.

### 3. Why is it said that priorities are not reliable?
- Because Java's thread model maps to OS threads, and different Operating Systems have different scheduling algorithms. Some might ignore Java priorities entirely.

### 4. What is "Priority Inversion"?
- A scenario where a low-priority thread holds a lock needed by a high-priority thread, effectively causing the high-priority thread to wait for the low-priority one.
