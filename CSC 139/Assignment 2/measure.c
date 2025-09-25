/*
    Tam Vo:
    This program measures the average time taken for the getpid() system call.
        While taking into account the time accuracy of the gettimeofday() 
        function to time these getpid() system calls.

    gcc -o measure measure.c -Wall -Werror -O
    
*/


// syscall_cost.c
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
    if(argc != 2) return EXIT_FAILURE;

    // Assign argument number to the loop iterations.
    const long long num_iterations = atoi(argv[1]);
    struct timespec start, end;

    // Starting the Timer by Assign Starting Timestamp to Start
    if (clock_gettime(CLOCK_MONOTONIC, &start) == -1) {
        perror("clock_gettime");
        return EXIT_FAILURE;
    }

    // Call blocking system call 
    for (long long i = 0; i < num_iterations; i++) {
        getpid();
    }

    // Stop the timer by assigning the current time to end
    if (clock_gettime(CLOCK_MONOTONIC, &end) == -1) {
        perror("clock_gettime");
        return EXIT_FAILURE;
    }

    // Calculate the total time in nanoseconds
    long long total_ns = (end.tv_sec - start.tv_sec) * 1000000000LL + (end.tv_nsec - start.tv_nsec);

    // Calculate the average time per system call with floating point division
    double avg_ns = (double) total_ns / num_iterations;

    // Displaay the metrics
    printf("Number of system calls: %lld\n", num_iterations);
    printf("Total time: %lld ns\n", total_ns);
    printf("Average system call cost: %.2f ns\n", avg_ns);

    return EXIT_SUCCESS;
}