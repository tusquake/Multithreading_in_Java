# Java Future and CompletableFuture - Deep Dive Guide

## Table of Contents
1. [Why Asynchronous Programming?](#why-asynchronous-programming)
2. [The Problem with Blocking](#the-problem-with-blocking)
3. [Restaurant Order Analogy](#restaurant-order-analogy)
4. [Future Interface](#future-interface)
5. [CompletableFuture](#completablefuture)
6. [Future vs CompletableFuture](#future-vs-completablefuture)

---

## Why Asynchronous Programming?

### The Synchronous Problem

```java
// Synchronous approach (blocking)
public void processOrder() {
    String userData = fetchUserData();        // Takes 2 seconds
    String productData = fetchProductData();  // Takes 3 seconds
    String paymentData = processPayment();    // Takes 2 seconds
    
    // Total time: 2 + 3 + 2 = 7 seconds
    System.out.println("Order processed");
}
```

**Problem:** Each operation blocks the next one. Thread sits idle waiting!

### The Asynchronous Solution

```java
// Asynchronous approach (non-blocking)
public void processOrder() {
    CompletableFuture<String> userData = fetchUserDataAsync();
    CompletableFuture<String> productData = fetchProductDataAsync();
    CompletableFuture<String> paymentData = processPaymentAsync();
    
    // All three run in parallel!
    // Total time: max(2, 3, 2) = 3 seconds
}
```

**Benefit:** Operations run concurrently. Much faster!

---

## The Problem with Blocking

### Blocking vs Non-Blocking

**Blocking (Synchronous):**
```
Main Thread:
[Fetch User Data] → Wait 2s → 
[Fetch Product Data] → Wait 3s → 
[Process Payment] → Wait 2s → 
[Done]

Total: 7 seconds
```

**Non-Blocking (Asynchronous):**
```
Main Thread:
[Submit all tasks] → Continue doing other work

Background Threads:
Thread-1: [Fetch User Data] → 2s → Done
Thread-2: [Fetch Product Data] → 3s → Done
Thread-3: [Process Payment] → 2s → Done

Total: 3 seconds (parallel execution)
```

---

## Restaurant Order Analogy

### Synchronous (Bad Service)

**Customer orders pizza:**

```
Customer: "I want pizza"
Waiter: "Ok, let me go to kitchen"
[Waiter walks to kitchen... 2 minutes]
[Chef makes pizza... 15 minutes]
[Waiter brings pizza... 2 minutes]
Waiter: "Here's your pizza"

Total: 19 minutes (customer waited entire time!)
```

**Problem:** Waiter blocked, can't serve other customers.

---

### Asynchronous (Good Service)

**Customer orders pizza:**

```
Customer: "I want pizza"
Waiter: "Sure! Here's your token number"
[Waiter submits order to kitchen]
[Waiter serves other customers]

Meanwhile in kitchen:
Chef: [Making pizza... 15 minutes]

Waiter: [Checks if pizza ready]
Waiter: "Token #5, your pizza is ready!"

Total: Customer free to do other things, waiter serves multiple customers
```

**Benefit:** Non-blocking! Waiter and customer both productive.

---

## Future Interface

### What is Future?

Future represents the result of an asynchronous computation. It's like a **receipt** or **token** - you get it immediately, but the actual result comes later.

### Basic Usage

```java
import java.util.concurrent.*;

public class FutureExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Submit task, get Future immediately
        Future<String> future = executor.submit(() -> {
            System.out.println("Task started in: " + Thread.currentThread().getName());
            Thread.sleep(3000); // Simulate long operation
            return "Result from background task";
        });
        
        System.out.println("Task submitted, doing other work...");
        
        // Do other work here
        System.out.println("Main thread doing other things");
        
        // Get result (blocks if not ready)
        String result = future.get(); // Waits for completion
        System.out.println("Got result: " + result);
        
        executor.shutdown();
    }
}
```

**Output:**
```
Task submitted, doing other work...
Main thread doing other things
Task started in: pool-1-thread-1
Got result: Result from background task
```

---

### Future Methods

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit) throws TimeoutException;
}
```

#### 1. get() - Blocking Get

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(2000);
    return "Done";
});

String result = future.get(); // Blocks for 2 seconds
System.out.println(result);
```

**Behavior:**
- Blocks current thread until result is ready
- Throws ExecutionException if task failed
- Throws InterruptedException if interrupted

---

#### 2. get(timeout) - Timed Wait

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(5000);
    return "Done";
});

try {
    String result = future.get(2, TimeUnit.SECONDS); // Wait max 2 seconds
    System.out.println(result);
} catch (TimeoutException e) {
    System.out.println("Task took too long!");
}
```

**Use Case:** Don't want to wait forever, set timeout

---

#### 3. isDone() - Check Completion

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(2000);
    return "Done";
});

while (!future.isDone()) {
    System.out.println("Task still running...");
    Thread.sleep(500);
}

String result = future.get(); // Now we know it's ready
System.out.println(result);
```

**Output:**
```
Task still running...
Task still running...
Task still running...
Task still running...
Done
```

---

#### 4. cancel() - Stop Task

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(5000);
    return "Done";
});

Thread.sleep(1000);
boolean cancelled = future.cancel(true); // Try to cancel

if (cancelled) {
    System.out.println("Task cancelled");
} else {
    System.out.println("Could not cancel");
}
```

**Parameters:**
- `true`: Interrupt thread if running
- `false`: Don't interrupt, just mark as cancelled

---

### Limitations of Future

**Problem 1: Can't Chain Operations**

```java
// Can't do this with Future:
// Fetch user → Fetch orders → Process orders → Send email
```

**Problem 2: No Callbacks**

```java
// Can't attach callback when task completes
future.whenComplete(() -> {
    System.out.println("Done!"); // Not possible with Future
});
```

**Problem 3: Can't Combine Futures**

```java
// Can't easily combine results
Future<String> future1 = ...;
Future<String> future2 = ...;
// How to combine results? Must manually get() both
```

**Problem 4: get() Blocks**

```java
String result = future.get(); // Main thread blocked!
```

**Solution:** CompletableFuture solves all these problems!

---

## CompletableFuture

### What is CompletableFuture?

CompletableFuture is an enhanced Future with:
- Non-blocking operations
- Callback support
- Chaining capabilities
- Combining multiple futures
- Exception handling

**Think of it as:** Future on steroids!

---

### Creating CompletableFuture

#### 1. supplyAsync (Returns Value)

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    System.out.println("Running in: " + Thread.currentThread().getName());
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return "Hello from CompletableFuture";
});

System.out.println("Main thread continues...");
String result = future.get(); // Get result when ready
System.out.println(result);
```

**Output:**
```
Main thread continues...
Running in: ForkJoinPool.commonPool-worker-1
Hello from CompletableFuture
```

---

#### 2. runAsync (No Return Value)

```java
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("Task running in: " + Thread.currentThread().getName());
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    System.out.println("Task completed");
});

System.out.println("Main thread continues...");
future.get(); // Wait for completion
```

**Output:**
```
Main thread continues...
Task running in: ForkJoinPool.commonPool-worker-1
Task completed
```

---

### Callbacks (Non-Blocking)

#### 1. thenApply - Transform Result

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Hello";
}).thenApply(result -> {
    return result + " World"; // Transform
});

System.out.println(future.get()); // Output: Hello World
```

**Flow:**
```
supplyAsync() → "Hello"
    ↓
thenApply() → "Hello World"
```

---

#### 2. thenAccept - Consume Result

```java
CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
    return "Processing complete";
}).thenAccept(result -> {
    System.out.println("Got: " + result); // Just consume, no return
});

future.get(); // Wait for completion
```

**Output:**
```
Got: Processing complete
```

---

#### 3. thenRun - Execute After Completion

```java
CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
    return "Data";
}).thenRun(() -> {
    System.out.println("Task finished, cleanup done"); // No access to result
});

future.get();
```

**Output:**
```
Task finished, cleanup done
```

---

### Chaining Operations

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    System.out.println("Step 1: Fetch user data");
    return "User-123";
}).thenApply(userId -> {
    System.out.println("Step 2: Fetch orders for " + userId);
    return "Order-456";
}).thenApply(orderId -> {
    System.out.println("Step 3: Process " + orderId);
    return "Processed";
}).thenApply(status -> {
    System.out.println("Step 4: Send notification");
    return "Notification sent";
});

System.out.println("Final: " + future.get());
```

**Output:**
```
Step 1: Fetch user data
Step 2: Fetch orders for User-123
Step 3: Process Order-456
Step 4: Send notification
Final: Notification sent
```

---

### Combining Multiple Futures

#### 1. thenCombine - Combine Two Futures

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(2000);
    return "Hello";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(3000);
    return "World";
});

CompletableFuture<String> combined = future1.thenCombine(future2, (result1, result2) -> {
    return result1 + " " + result2;
});

System.out.println(combined.get()); // Output: Hello World
// Takes 3 seconds (max of 2 and 3), not 5 seconds!
```

**Flow:**
```
future1: [2s] → "Hello"
future2: [3s] → "World"
         ↓
thenCombine → "Hello World"
```

---

#### 2. allOf - Wait for All

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(2000);
    return "Task 1";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(3000);
    return "Task 2";
});

CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Task 3";
});

CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);

allFutures.get(); // Wait for all to complete (3 seconds)

System.out.println("All tasks completed!");
System.out.println(future1.get()); // Task 1
System.out.println(future2.get()); // Task 2
System.out.println(future3.get()); // Task 3
```

**Output:**
```
All tasks completed!
Task 1
Task 2
Task 3
```

---

#### 3. anyOf - Wait for Any One

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(5000);
    return "Slow task";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(2000);
    return "Fast task";
});

CompletableFuture<Object> fastest = CompletableFuture.anyOf(future1, future2);

System.out.println("First completed: " + fastest.get());
// Output: First completed: Fast task (after 2 seconds)
```

---

### Exception Handling

#### 1. exceptionally - Handle Exception

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (true) {
        throw new RuntimeException("Something went wrong!");
    }
    return "Success";
}).exceptionally(ex -> {
    System.out.println("Error: " + ex.getMessage());
    return "Default value"; // Fallback
});

System.out.println(future.get()); // Output: Default value
```

---

#### 2. handle - Handle Both Success and Error

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // Might throw exception
    if (Math.random() > 0.5) {
        throw new RuntimeException("Failed!");
    }
    return "Success";
}).handle((result, exception) -> {
    if (exception != null) {
        return "Error: " + exception.getMessage();
    }
    return result;
});

System.out.println(future.get());
```

---

#### 3. whenComplete - Execute on Completion

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Task completed";
}).whenComplete((result, exception) -> {
    if (exception != null) {
        System.out.println("Failed: " + exception.getMessage());
    } else {
        System.out.println("Success: " + result);
    }
});

future.get();
```

**Output:**
```
Success: Task completed
```

---

## Real-World Example: E-commerce Order Processing

```java
public class OrderProcessor {
    
    public static void main(String[] args) throws Exception {
        processOrder("ORDER-123");
    }
    
    public static void processOrder(String orderId) throws Exception {
        System.out.println("Processing order: " + orderId);
        
        CompletableFuture<String> future = CompletableFuture
            // Step 1: Validate order
            .supplyAsync(() -> {
                System.out.println("[1] Validating order...");
                sleep(1000);
                return orderId;
            })
            // Step 2: Check inventory
            .thenApply(id -> {
                System.out.println("[2] Checking inventory for " + id);
                sleep(2000);
                return id + ":AVAILABLE";
            })
            // Step 3: Process payment
            .thenApply(data -> {
                System.out.println("[3] Processing payment for " + data);
                sleep(1500);
                return data + ":PAID";
            })
            // Step 4: Ship order
            .thenApply(data -> {
                System.out.println("[4] Shipping order " + data);
                sleep(1000);
                return data + ":SHIPPED";
            })
            // Step 5: Send confirmation
            .thenApply(data -> {
                System.out.println("[5] Sending confirmation for " + data);
                return "Order processed successfully!";
            })
            // Handle errors
            .exceptionally(ex -> {
                System.out.println("Error: " + ex.getMessage());
                return "Order processing failed";
            });
        
        // Main thread continues
        System.out.println("Main thread free to do other work...");
        
        // Get final result
        String result = future.get();
        System.out.println("\nFinal result: " + result);
    }
    
    static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Output:**
```
Processing order: ORDER-123
Main thread free to do other work...
[1] Validating order...
[2] Checking inventory for ORDER-123
[3] Processing payment for ORDER-123:AVAILABLE
[4] Shipping order ORDER-123:AVAILABLE:PAID
[5] Sending confirmation for ORDER-123:AVAILABLE:PAID:SHIPPED

Final result: Order processed successfully!
```

---

## Real-World Example: Parallel API Calls

```java
public class ParallelAPICallsExample {
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        
        // Three independent API calls
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching user data...");
            sleep(2000);
            return "User{name=John}";
        });
        
        CompletableFuture<String> productFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching product data...");
            sleep(3000);
            return "Product{id=123, price=1000}";
        });
        
        CompletableFuture<String> reviewsFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching reviews...");
            sleep(1500);
            return "Reviews{count=50, avg=4.5}";
        });
        
        // Wait for all
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            userFuture, productFuture, reviewsFuture
        );
        
        allFutures.get();
        
        // Get results
        System.out.println("\nResults:");
        System.out.println(userFuture.get());
        System.out.println(productFuture.get());
        System.out.println(reviewsFuture.get());
        
        long end = System.currentTimeMillis();
        System.out.println("\nTotal time: " + (end - start) + "ms");
        // Output: ~3000ms (not 2000+3000+1500=6500ms!)
    }
    
    static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Output:**
```
Fetching user data...
Fetching product data...
Fetching reviews...

Results:
User{name=John}
Product{id=123, price=1000}
Reviews{count=50, avg=4.5}

Total time: 3002ms
```

**Sequential would take:** 2000 + 3000 + 1500 = 6500ms
**Parallel takes:** max(2000, 3000, 1500) = 3000ms
**Speed improvement:** 2.17x faster!

---

## Future vs CompletableFuture

### Feature Comparison

| Feature | Future | CompletableFuture |
|---------|--------|-------------------|
| **Get result** | ✅ get() | ✅ get() |
| **Non-blocking** | ❌ No | ✅ Yes (callbacks) |
| **Chaining** | ❌ No | ✅ thenApply, thenAccept |
| **Combining** | ❌ No | ✅ thenCombine, allOf, anyOf |
| **Exception handling** | ❌ Limited | ✅ exceptionally, handle |
| **Manual completion** | ❌ No | ✅ complete(), completeExceptionally() |
| **Async execution** | ✅ Via Executor | ✅ supplyAsync, runAsync |
| **Callbacks** | ❌ No | ✅ whenComplete, thenAccept |

---

### When to Use Future

✅ **Use Future when:**
- Simple async task with single result
- Don't need chaining or combining
- Basic fire-and-forget operation
- Working with legacy code

```java
ExecutorService executor = Executors.newFixedThreadPool(1);
Future<String> future = executor.submit(() -> {
    return "Simple result";
});
String result = future.get();
```

---

### When to Use CompletableFuture

✅ **Use CompletableFuture when:**
- Need to chain multiple async operations
- Need to combine results from multiple tasks
- Want non-blocking callbacks
- Need sophisticated error handling
- Building complex async workflows

```java
CompletableFuture.supplyAsync(() -> fetchUser())
    .thenApply(user -> fetchOrders(user))
    .thenApply(orders -> processOrders(orders))
    .thenAccept(result -> sendEmail(result))
    .exceptionally(ex -> handleError(ex));
```

---

## Summary Table

| Concept | Description | Example |
|---------|-------------|---------|
| **Future** | Receipt for async result | `executor.submit(() -> "task")` |
| **get()** | Blocking wait for result | `future.get()` blocks |
| **CompletableFuture** | Enhanced Future with callbacks | `CompletableFuture.supplyAsync()` |
| **supplyAsync** | Create CF that returns value | `supplyAsync(() -> "value")` |
| **runAsync** | Create CF with no return | `runAsync(() -> doWork())` |
| **thenApply** | Transform result | `thenApply(x -> x * 2)` |
| **thenAccept** | Consume result | `thenAccept(x -> print(x))` |
| **thenCombine** | Combine two futures | `future1.thenCombine(future2)` |
| **allOf** | Wait for all | `CompletableFuture.allOf(f1, f2)` |
| **anyOf** | Wait for fastest | `CompletableFuture.anyOf(f1, f2)` |
| **exceptionally** | Handle error | `exceptionally(ex -> fallback)` |

---

## Key Takeaways

1. **Future = Blocking:** get() blocks until result ready
2. **CompletableFuture = Non-blocking:** Callbacks don't block
3. **Chaining = Sequential:** One after another
4. **Combining = Parallel:** Multiple tasks simultaneously
5. **Always handle exceptions:** Use exceptionally() or handle()
6. **Use allOf for parallel:** Much faster than sequential

**Remember:** CompletableFuture is Future++. Use it for modern async programming!