/*
    Tam Vo
        This program creates a parent and child process 
        that force context switches between each other using pipes.
        And displays the execution metrics of these. 

        Works for both linux and MacOs
    
    gcc -o context_switch context_switch.c -Wall -Werror -O

*/
#define _GNU_SOURCE

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <sys/wait.h>

#ifdef __linux__
#include <sched.h>
#elif __APPLE__
#include <mach/mach.h>
#include <mach/thread_policy.h>
#endif

int main() {
    const int num_iterations = 100000;
    int pipe1[2]; // Parent -> Child
    int pipe2[2]; // Child -> Parent

    if (pipe(pipe1) == -1 || pipe(pipe2) == -1) {
        perror("pipe");
        return EXIT_FAILURE;
    }
    
#ifdef __APPLE__
    // macOS-specific CPU affinity
    thread_port_t mach_thread = mach_thread_self();
    thread_affinity_policy_data_t policy = { .affinity_tag = 1 }; // Use core 1
    if (thread_policy_set(mach_thread, THREAD_AFFINITY_POLICY, (thread_policy_t)&policy, THREAD_AFFINITY_POLICY_COUNT) != KERN_SUCCESS) {
        // It's common for this to fail on modern macOS versions without special entitlements,
        // so we'll print a warning but not exit.
        fprintf(stderr, "Warning: Could not set CPU affinity. Results may be less accurate.\n");
    }
#elif __linux__
    // Pin the process to a single CPU to ensure context switching
    cpu_set_t set;
    CPU_ZERO(&set);
    CPU_SET(0, &set);
    if (sched_setaffinity(0, sizeof(cpu_set_t), &set) == -1) {
        perror("sched_setaffinity");
        // May fail in some environments like Docker/WSL without specific permissions
        fprintf(stderr, "Warning: Could not set CPU affinity. Results may be less accurate.\n");
    }
#endif

    pid_t pid = fork();

    if (pid == -1) {
        perror("fork");
        return EXIT_FAILURE;
    }

    if (pid == 0) { // Child Process
        char buf;
        close(pipe1[1]); // Close write-end of parent-to-child pipe
        close(pipe2[0]); // Close read-end of child-to-parent pipe

        for (int i = 0; i < num_iterations; i++) {
            read(pipe1[0], &buf, 1);  // Read from parent, blocks if empty
            write(pipe2[1], "c", 1); // Write to parent
        }

        close(pipe1[0]);
        close(pipe2[1]);
        exit(EXIT_SUCCESS);
    } else { // Parent Process
        struct timespec start, end;
        char buf;

        close(pipe1[0]); // Close read-end of parent-to-child pipe
        close(pipe2[1]); // Close write-end of child-to-parent pipe
        
        if (clock_gettime(CLOCK_MONOTONIC, &start) == -1) {
            perror("clock_gettime");
            return EXIT_FAILURE;
        }

        for (int i = 0; i < num_iterations; i++) {
            write(pipe1[1], "p", 1); // Write to child
            read(pipe2[0], &buf, 1);  // Read from child, blocks if empty
        }

        if (clock_gettime(CLOCK_MONOTONIC, &end) == -1) {
            perror("clock_gettime");
            return EXIT_FAILURE;
        }

        wait(NULL); // Wait for child to terminate

        close(pipe1[1]);
        close(pipe2[0]);

        // Each loop iteration is a round trip, which involves TWO context switches
        long long total_ns = (end.tv_sec - start.tv_sec) * 1000000000LL + (end.tv_nsec - start.tv_nsec);
        double avg_round_trip_ns = (double)total_ns / num_iterations;
        double avg_context_switch_ns = avg_round_trip_ns / 2.0;

        printf("Number of round trips (2 context switches each): %d\n", num_iterations);
        printf("Total time: %lld ns\n", total_ns);
        printf("Average context switch cost: %.2f ns (%.2f us)\n", avg_context_switch_ns, avg_context_switch_ns / 1000.0);
    }

    return EXIT_SUCCESS;
}
