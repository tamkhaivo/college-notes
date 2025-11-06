/**
 * @file main.c
 * @brief A multi-threaded web server simulation demonstrating different synchronization mechanisms.
 *
 * This program simulates a producer-consumer problem where producer threads generate
 * "requests" and consumer threads "process" them. The requests are stored in a shared
 * buffer. The program showcases three different synchronization techniques:
 * 
 * 1. Mutexes
 * 2. Condition Variables
 * 3. Semaphores
 *
 * @author Tam Vo
 * @date November 4, 2025
 */

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <semaphore.h>
#include <string.h>
#include <fcntl.h> // For O_CREAT MACOS compatiablity 

#define BUFFER_SIZE 10
#define NUM_PRODUCERS 10
#define NUM_CONSUMERS 10
#define MAX_REQUESTS_PER_THREAD 2

#define SEM_EMPTY_NAME "/empty_slots"
#define SEM_FULL_NAME "/full_slots"

/**
 * @brief Shared buffer for requests.
 */
int buffer[BUFFER_SIZE];
int in = 0;
int out = 0;
int request_counter = 0;

/**
 * @brief Synchronization variables.
 */
pthread_mutex_t mutex;
pthread_mutex_t request_mutex;
pthread_mutex_t print_mutex;
pthread_mutex_t print_producer_mutex;
pthread_cond_t cond_producer;
pthread_cond_t cond_consumer;
sem_t *empty_slots;
sem_t *full_slots;

/**
 * @brief Represents the type of synchronization to use.
 */
typedef enum {
    MUTEX,
    CONDVAR,
    SEMAPHORE
} SyncType;

/**
 * @brief A structure to hold parameters for producer and consumer threads.
 */
typedef struct {
    SyncType sync_type;
} ThreadParams;

// Function signatures
void *producer(void *param);
void *consumer(void *param);

void produce_mutex(int request);
int consume_mutex();

void produce_condvar(int request);
int consume_condvar();

void produce_semaphore(int request);
int consume_semaphore();

/**
 * @brief Initializes synchronization primitives based on the selected type.
 * @param sync_type The synchronization type to initialize.
 */
void init_sync(SyncType sync_type) {
    pthread_mutex_init(&request_mutex, NULL);
    pthread_mutex_init(&print_mutex, NULL);
    pthread_mutex_init(&print_producer_mutex, NULL);
    switch (sync_type) {
        case MUTEX:
            pthread_mutex_init(&mutex, NULL);
            break;
        case CONDVAR:
            pthread_mutex_init(&mutex, NULL);
            pthread_cond_init(&cond_producer, NULL);
            pthread_cond_init(&cond_consumer, NULL);
            break;
        case SEMAPHORE:
            // Using named semaphores for macOS compatibility
            empty_slots = sem_open(SEM_EMPTY_NAME, O_CREAT, 0644, BUFFER_SIZE);
            full_slots = sem_open(SEM_FULL_NAME, O_CREAT, 0644, 0);
            pthread_mutex_init(&mutex, NULL); // Mutex for buffer access
            break;
    }
}

/**
 * @brief Destroys synchronization primitives.
 * @param sync_type The synchronization type to destroy.
 */
void destroy_sync(SyncType sync_type) {
    pthread_mutex_destroy(&request_mutex);
    pthread_mutex_destroy(&print_mutex);
    pthread_mutex_destroy(&print_producer_mutex);
    switch (sync_type) {
        case MUTEX:
            pthread_mutex_destroy(&mutex);
            break;
        case CONDVAR:
            pthread_mutex_destroy(&mutex);
            pthread_cond_destroy(&cond_producer);
            pthread_cond_destroy(&cond_consumer);
            break;
        case SEMAPHORE:
            sem_close(empty_slots);
            sem_close(full_slots);
            sem_unlink(SEM_EMPTY_NAME);
            sem_unlink(SEM_FULL_NAME);
            pthread_mutex_destroy(&mutex);
            break;
    }
}

/**
 * @brief Main function to run the web server simulation.
 * @param argc Argument count.
 * @param argv Argument vector.
 * @return 0 on success, 1 on error.
 */
int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <mutex|condvar|semaphore>\n", argv[0]);
        return 1;
    }

    SyncType sync_type;
    if (strcmp(argv[1], "mutex") == 0) {
        sync_type = MUTEX;
    } else if (strcmp(argv[1], "condvar") == 0) {
        sync_type = CONDVAR;
    } else if (strcmp(argv[1], "semaphore") == 0) {
        sync_type = SEMAPHORE;
    } else {
        fprintf(stderr, "Invalid synchronization type. Use 'mutex', 'condvar', or 'semaphore'.\n");
        return 1;
    }

    printf("Using synchronization type: %s\n", argv[1]);

    init_sync(sync_type);

    pthread_t producer_threads[NUM_PRODUCERS];
    pthread_t consumer_threads[NUM_CONSUMERS];
    ThreadParams params = {sync_type};

    for (int i = 0; i < NUM_PRODUCERS; i++) {
        pthread_create(&producer_threads[i], NULL, producer, &params);
    }

    for (int i = 0; i < NUM_CONSUMERS; i++) {
        pthread_create(&consumer_threads[i], NULL, consumer, &params);
    }

    for (int i = 0; i < NUM_PRODUCERS; i++) {
        pthread_join(producer_threads[i], NULL);
    }

    for (int i = 0; i < NUM_CONSUMERS; i++) {
        pthread_join(consumer_threads[i], NULL);
    }

    destroy_sync(sync_type);

    return 0;
}

/**
 * @brief Producer thread function.
 * @param param A pointer to ThreadParams.
 * @return NULL.
 */
void *producer(void *param) {
    ThreadParams *params = (ThreadParams *)param;
    
    for (int i = 0; i < MAX_REQUESTS_PER_THREAD; i++) {        
        pthread_mutex_lock(&request_mutex);
        int current_request = request_counter++;
        pthread_mutex_unlock(&request_mutex);

        switch (params->sync_type) {
            case MUTEX:
                produce_mutex(current_request);
                break;
            case CONDVAR:
                produce_condvar(current_request);
                break;
            case SEMAPHORE:
                produce_semaphore(current_request);
                break;
        }
        // Desynced output
        printf("Produced: %d\n", current_request);
    }
    pthread_exit(NULL);
}

/**
 * @brief Consumer thread function.
 * @param param A pointer to ThreadParams.
 * @return NULL.
 */
void *consumer(void *param) {
    ThreadParams *params = (ThreadParams *)param;

    for (int i = 0; i < MAX_REQUESTS_PER_THREAD; i++) {
        int request;
        switch (params->sync_type) {
            case MUTEX:
                request = consume_mutex();
                break;
            case CONDVAR:
                request = consume_condvar();
                break;
            case SEMAPHORE:
                request = consume_semaphore();
                break;
        }
        // Desynced output
        printf("Consumed: %d\n", request);
    }
    pthread_exit(NULL);
}

/**
 * @brief Produces a request using mutex for synchronization.
 * @param request The request to produce.
 */
void produce_mutex(int request) {
    pthread_mutex_lock(&mutex);
    while (((in + 1) % BUFFER_SIZE) == out) {
        // Buffer is full, busy wait
        pthread_mutex_unlock(&mutex);
        pthread_mutex_lock(&mutex);
    }
    buffer[in] = request;
    in = (in + 1) % BUFFER_SIZE;
    pthread_mutex_unlock(&mutex);
}

/**
 * @brief Consumes a request using mutex for synchronization.
 * @return The consumed request.
 */
int consume_mutex() {
    int request;
    pthread_mutex_lock(&mutex);
    while (in == out) { // Buffer is empty, busy wait
        pthread_mutex_unlock(&mutex);
        pthread_mutex_lock(&mutex);
    }
    request = buffer[out];
    out = (out + 1) % BUFFER_SIZE;
    pthread_mutex_unlock(&mutex);
    return request;
}

/**
 * @brief Produces a request using condition variables for synchronization.
 * @param request The request to produce.
 */
void produce_condvar(int request) {
    pthread_mutex_lock(&mutex);
    while (((in + 1) % BUFFER_SIZE) == out) {
        // Buffer is full, wait for consumer
        pthread_cond_wait(&cond_producer, &mutex);
    }
    buffer[in] = request;
    in = (in + 1) % BUFFER_SIZE;
    pthread_cond_signal(&cond_consumer);
    pthread_mutex_unlock(&mutex);
}

/**
 * @brief Consumes a request using condition variables for synchronization.
 * @return The consumed request.
 */
int consume_condvar() {
    int request;
    pthread_mutex_lock(&mutex);
    while (in == out) {
        // Buffer is empty, wait for producer
        pthread_cond_wait(&cond_consumer, &mutex);
    }
    request = buffer[out];
    out = (out + 1) % BUFFER_SIZE;
    pthread_cond_signal(&cond_producer);
    pthread_mutex_unlock(&mutex);
    return request;
}

/**
 * @brief Produces a request using semaphores for synchronization.
 * @param request The request to produce.
 */
void produce_semaphore(int request) {
    sem_wait(empty_slots);
    pthread_mutex_lock(&mutex);
    buffer[in] = request;
    in = (in + 1) % BUFFER_SIZE;
    pthread_mutex_unlock(&mutex);
    sem_post(full_slots);
}

/**
 * @brief Consumes a request using semaphores for synchronization.
 * @return The consumed request.
 */
int consume_semaphore() {
    int request;
    sem_wait(full_slots);
    pthread_mutex_lock(&mutex);
    request = buffer[out];
    out = (out + 1) % BUFFER_SIZE;
    pthread_mutex_unlock(&mutex);
    sem_post(empty_slots);
    return request;
}
