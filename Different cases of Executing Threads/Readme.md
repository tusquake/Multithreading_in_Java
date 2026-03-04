# Executing Threads - Common Cases & Edge Cases

## Table of Contents
1. [Introduction](#introduction)
2. [Case 1: `start()` vs `run()`](#case-1-start-vs-run)
3. [Case 2: Overriding `start()`](#case-2-overriding-start)
4. [Case 3: Restarting a Thread](#case-3-restarting-a-thread)
5. [Case 4: The Empty `run()` Method](#case-4-the-empty-run-method)
6. [Case 5: Overloading `run()`](#case-5-overloading-run)

---

## Introduction

This folder explores various scenarios and common mistakes when starting and executing threads in Java.

---

## Case 1: `start()` vs `run()`

- **`start()`:** Creates a new call stack and executes `run()` in a new thread.
- **`run()`:** Executes the code in the **existing** call stack (just like a normal method call).

---

## Case 2: Overriding `start()`

If you override the `start()` method, the `Thread` class's own logic (which creates the new thread) is ignored unless you call `super.start()`.

```java
@Override
public void start() {
    super.start(); // Essential for multithreading
    System.out.println("Custom start logic");
}
```

---

## Case 3: Restarting a Thread

You **cannot** call `start()` twice on the same thread object.

```java
Thread t1 = new Thread();
t1.start();
t1.start(); // Throws IllegalThreadStateException
```

---

## Case 4: The Empty `run()` Method

If you extend `Thread` but do not override `run()`, the thread will start and finish immediately because the default `run()` in the `Thread` class does nothing.

---

## Case 5: Overloading `run()`

You can overload the `run()` method (e.g., `run(int i)`), but the `start()` method will always call the zero-argument `run()` method.

---

## Summary of Execution Flows

| Scenario | Result |
|----------|--------|
| `t.start()` | New thread created -> `run()` executed. |
| `t.run()` | No new thread -> `run()` executed in current thread. |
| Overridden `start()` without `super.start()` | No new thread created. |
| Overloaded `run(int i)` | Ignored by the `start()` method. |
