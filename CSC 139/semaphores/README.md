# CSC 139 - Operating Systems: Synchronization Assignment

**Instructor:** Dr. Abeer Abdel Khaleq

**Term:** FA2025

## Project Overview

This project is a simulation of a multi-threaded web server in C. It demonstrates the producer-consumer problem with a shared buffer. A producer thread generates HTTPS requests and adds them to a shared server queue, while a consumer thread processes these requests from the queue. The project implements and compares three different synchronization mechanisms to manage access to the shared buffer:

1.  **Mutex/Locks (pthreads)**
2.  **Conditional Variables**
3.  **Semaphores**

## How to Compile and Run

### Compilation

To compile the project, use the provided `Makefile`. Simply run the `make` command in your terminal:

```bash
make
```

This will create an executable file named `main`.

### Running the Simulation

To run the simulation, execute the `main` program with one of the following command-line arguments to specify the synchronization method:

*   For Mutex/Locks:

    ```bash
    ./main mutex
    ```

*   For Conditional Variables:

    ```bash
    ./main condvar
    ```

*   For Semaphores:

    ```bash
    ./main semaphore
    ```

## Screenshot of Running the Code

*(TODO: Insert a screenshot of your code running with one of the synchronization methods)*

## Differences Between Synchronization Implementations

### 1. Mutex/Locks (pthreads)

*(TODO: Describe your implementation using mutexes. Explain how it works and what you did.)*

### 2. Conditional Variables

*(TODO: Describe your implementation using conditional variables. Explain how they improve upon the mutex-only solution.)*

### 3. Semaphores

*(TODO: Describe your implementation using semaphores. Explain how they manage access to the shared buffer.)*

## Challenges, Observations, and Conclusion

### Challenges Encountered

*(TODO: Describe any challenges you faced during the implementation. For example, did you encounter race conditions, deadlocks, or other synchronization issues?)*

### Observations

*(TODO: What did you observe while running the different implementations? Was there a noticeable difference in performance or behavior?)*

### What Worked and What Did Not Work

*(TODO: Reflect on what aspects of your implementations were successful and which were not. Did any of the methods surprise you?)*

### Which Synchronization Implementation Would You Use and Why?

*(TODO: Based on your experience, which synchronization mechanism would you choose for this web server simulation and why? Consider factors like efficiency, complexity, and prevention of issues like busy-waiting.)*
