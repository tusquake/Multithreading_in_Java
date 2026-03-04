# Engineering Digest - Multithreading Basics

## Overview
This folder contains a collection of basic multithreading concepts, likely structured as part of the "Engineering Digest" tutorial series. It covers the essential building blocks for any Java developer learning concurrent programming.

## Table of Contents
1. [Basics](#basics)
2. [Thread Class](#thread-class)
3. [Runnable Interface](#runnable-interface)
4. [Thread Life Cycle](#thread-life-cycle)
5. [Thread Methods Demo](#thread-methods-demo)

---

## 1. Basics
Concepts covered:
- What is a Thread?
- Process vs Thread.
- Benefits of Multithreading.

## 2. Thread Class
Implementation of threads by extending the `java.lang.Thread` class.
- Overriding the `run()` method.
- Using `start()` to initiate execution.

## 3. Runnable Interface
Implementation of threads using the `Runnable` interface.
- Preferred way in Java due to single inheritance limitation.
- Decoupling task logic from thread management.

## 4. Thread Life Cycle
Understanding various states of a thread:
- **New:** Thread is created but not started.
- **Runnable:** Thread is ready to run and waiting for CPU.
- **Running:** Thread is currently executing.
- **Blocked/Waiting:** Thread is waiting for a lock or a signal.
- **Terminated:** Thread has finished execution.

## 5. Thread Methods Demo
Practical demonstration of common thread methods:
- `getName()` / `setName()`
- `getPriority()` / `setPriority()`
- `sleep()`
- `join()`

---

## Summary
These examples serve as the foundation for the more advanced topics covered in other folders of this repository, such as Synchronization, Executors, and Locks.
