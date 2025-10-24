#include <stdio.h>
#include <pthread.h>
#include <unistd.h>



#define MAX 10 // Define a size for the buffer
#define LOOPS 300 // Define the number of items to produce/consume

// From Figure 30.13
int buffer[MAX];
int fill_ptr = 0;
int use_ptr = 0;
int count = 0;

void put(int value) {
    buffer[fill_ptr] = value;
    fill_ptr = (fill_ptr + 1) % MAX;
    count++;
}

int get() {
    int tmp = buffer[use_ptr];
    use_ptr = (use_ptr + 1) % MAX;
    count--;
    return tmp;
}

// From Figure 30.14
pthread_cond_t empty, fill;
pthread_mutex_t mutex;

void *producer(void *arg) {
    int i;
    for (i = 0; i < LOOPS; i++) {
        pthread_mutex_lock(&mutex);                   // p1
        while (count == MAX)                          // p2
            pthread_cond_wait(&empty, &mutex);        // p3
        put(i);                                       // p4
        printf("Produced: %d\n", i);
        pthread_cond_signal(&fill);                   // p5
        pthread_mutex_unlock(&mutex);                 // p6
    }
    return NULL;
}

void *consumer(void *arg) {
    int i;
    for (i = 0; i < LOOPS * 2; i++) {
        pthread_mutex_lock(&mutex);                   // c1
        while (count == 0)                            // c2
            pthread_cond_wait(&fill, &mutex);         // c3
        int tmp = get();                              // c4
        pthread_cond_signal(&empty);                  // c5
        pthread_mutex_unlock(&mutex);                 // c6
        printf("Consumed: %d\n", tmp);
    }
    return NULL;
}

int main(int argc, char *argv[]) {
    pthread_t p1, p2, c;

    // Initialize mutex and condition variables
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&empty, NULL);
    pthread_cond_init(&fill, NULL);

    printf("Starting producer and consumer...\n");

    // Create producer and consumer threads
    pthread_create(&p1, NULL, producer, NULL);
    pthread_create(&p2, NULL, producer, NULL);
    pthread_create(&c, NULL, consumer, NULL);

    // Wait for threads to finish
    pthread_join(p1, NULL);
    pthread_join(p2, NULL);
    pthread_join(c, NULL);

    printf("Producer and consumer finished.\n");

    // Clean up
    pthread_mutex_destroy(&mutex);
    pthread_cond_destroy(&empty);
    pthread_cond_destroy(&fill);

    return 0;
}
