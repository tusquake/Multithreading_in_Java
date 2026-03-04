# Thread Class vs Runnable Interface - The Foundation

## Table of Contents
1. [Introduction](#introduction)
2. [Analogies](#analogies)
3. [The Thread Class Approach](#the-thread-class-approach)
4. [The Runnable Interface Approach](#the-runnable-interface-approach)
5. [Comparison: Extends vs Implements](#comparison-extends-vs-implements)
6. [Complete Examples](#complete-examples)
7. [Common Interview Questions](#common-interview-questions)

---

## Introduction

In Java, there are two primary ways to create a thread:
1. By extending the `Thread` class.
2. By implementing the `Runnable` interface.

Both achieve the same goal—running code concurrently—but they differ in flexibility and design.

---

## Analogies

### 1. Extending Thread (The Specialized Robot)
Imagine you are building a **specialized robot** that is hardwired to only perform one specific task (e.g., painting). The robot *is* the task.

### 2. Implementing Runnable (The Universal Worker + Task)
Imagine you have a **universal robot** (The Thread) and you give it a **removable instruction chip** (The Runnable). You can put different chips into the same robot, and the worker is separate from the task.

---

## The Thread Class Approach

### How it works:
1. Create a class that extends `java.lang.Thread`.
2. Override the `run()` method.
3. Instantiate the class and call `start()`.

### Code Snippet:
```java
class MyThread extends Thread {
    public void run() {
        System.out.println("Thread is running...");
    }
}

// Usage
MyThread t = new MyThread();
t.start();
```

**Pros:**
- Simpler syntax for small tasks.
- You have direct access to `Thread` class methods (like `getName()`, `setPriority()`).

**Cons:**
- **Inflexible:** Java doesn't support multiple inheritance. If you extend `Thread`, you can't extend any other class.
- Tight coupling between the thread and the task.

---

## The Runnable Interface Approach

### How it works:
1. Create a class that implements `java.lang.Runnable`.
2. Implement the `run()` method.
3. Create a `Thread` object, passing the `Runnable` instance to its constructor.
4. Call `start()` on the thread object.

### Code Snippet:
```java
class MyTask implements Runnable {
    public void run() {
        System.out.println("Runnable task is running...");
    }
}

// Usage
Thread t = new Thread(new MyTask());
t.start();
```

**Pros:**
- **Flexible:** You can implement `Runnable` and still extend another class.
- **Better Design:** Separates the task (Runnable) from the runner (Thread).
- **Reusable:** The same `Runnable` instance can be shared among multiple threads.

**Cons:**
- Slightly more verbose.

---

## Comparison: Extends vs Implements

| Feature | `extends Thread` | `implements Runnable` |
|---------|------------------|-----------------------|
| **Inheritance** | Occupies the single inheritance slot. | Leaves the inheritance slot free for other classes. |
| **Object Sharing** | Each thread is a separate object. | Multiple threads can share the same `Runnable` instance. |
| **Design** | Tight coupling (Task IS ONLY a Thread). | Loose coupling (Task is SEPARATE from Thread). |
| **Ease of Use** | Slightly easier for beginners. | Preferred in professional software engineering. |

---

## Complete Examples

### Extending Thread (`Tdemo.java`)
```java
class MyThread extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(Thread.currentThread().getName() + " - " + i);
        }
    }
}

public class Tdemo {
    public static void main(String[] args) {
        MyThread t1 = new MyThread();
        t1.setName("Thread-A");
        t1.start();
    }
}
```

### Implementing Runnable (`Rdemo.java`)
```java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(Thread.currentThread().getName() + " - " + i);
        }
    }
}

public class Rdemo {
    public static void main(String[] args) {
        Thread t1 = new Thread(new MyRunnable());
        t1.setName("Runnable-Thread");
        t1.start();
    }
}
```

---

## Common Interview Questions

### 1. Why do we call `start()` instead of `run()`?
- `start()` creates a new thread and then calls `run()` in that new thread's context.
- Calling `run()` directly executes it in the **same thread** (like a normal method call), defeating the purpose of multithreading.

### 2. Can we extend the `Thread` class and also implement `Runnable`?
- Yes, you can, but it is redundant. `Thread` itself implements `Runnable`.

### 3. Which approach is better and why?
- `implements Runnable` is better because it allows your class to extend another class, promotes resource sharing, and follows the "Composition over Inheritance" principle.

### 4. What happens if we call `start()` twice on the same thread?
- It throws `IllegalThreadStateException`. A thread cannot be restarted once it has finished.
