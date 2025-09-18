/*
    Tam Vo 
    Programming Assignment - Processes

    1. Write a program that calls fork(). Before calling fork(), have the main process access a variable (e.g., x) and set its value to something (e.g., 100).
    What value is the variable in the child process?
    What happens to the variable when both the child and parent change the value of x?

    2. Write a program that opens a file (with the open() system call) and then calls fork() to create a new process.
    Can both the child and parent access the file descriptor returned by open()?
    What happens when they are writing to the file concurrently, i.e., at the same time?

    3. Write another program using fork(). The child process should print “hello”; the parent process should print “goodbye”. You should try to ensure that the child process always prints first; can you do this without calling wait() in the parent?

    4. Write a program that calls fork() and then calls some form of exec() to run the program /bin/ls. See if you can try all of the variants of exec(), including (on Linux) execl(), execle(), execlp(), execv(), execvp(), and execvpe().
    Why do you think there are so many variants of the same basic call?

    5. Now write a program that uses wait() to wait for the child process to finish in the parent. What does wait() return? What happens if you use wait() in the child?

    6. Write a slight modification of the previous program, this time using waitpid() instead of wait(). When would waitpid() be
    useful?

    7. Write a program that creates a child process, and then in the child closes standard output (STDOUT FILENO).
    What happens if the child calls printf() to print some output after closing the descriptor?

    8. Write a program that creates two children, and connects the standard output of one to the standard input of the other, using the pipe() system call.
*/

/**
 * @file process.c
 * @brief Solutions to programming assignment questions on process management in C.
 *
 * Contains functions demonstrating the use of fork(), exec(), wait(),
 * waitpid(), file descriptors, and pipes.
 *
 * To run a specific question's code, uncomment its function call in main(),
 * then compile and execute.
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>     // For fork(), exec(), pipe(), sleep()
#include <sys/wait.h>   // For wait(), waitpid()
#include <fcntl.h>      // For open() and file flags
#include <string.h>     // For strerror()
#include <errno.h>      // For errno

// Forward declarations
void question1();
void question2();
void question3();
void question4();
void question5();
void question6();
void question7();
void question8();

int main(int argc, char* argv[]) {

    question1();
    question2();
    question3();
    question4();
    question5();
    question6();
    question7();
    question8();

    return 0;
}

/**
 * @brief Question 1: Demonstrates that child processes get a copy of variables.
 *
 * When fork() is called, the child process gets a separate copy of the parent's
 * memory. Changes to a variable in one process do not affect the other.
 */
void question1() {
    printf("--- Running Question 1 ---\n");
    int x = 100;
    printf("Variable x is initialized to %d\n", x);

    pid_t pid = fork();
    if (pid < 0) {
        perror("fork failed");
        exit(1);
    } else if (pid == 0) {  // Child process
        printf("CHILD: x is %d\n", x);
        x = 250;
        printf("CHILD: x is changed to %d.\n", x);
        exit(0);
    } else {                // Parent process
        printf("PARENT: x is %d.\n", x);
        x = 50;
        printf("PARENT: x is changed to %d.\n", x);

        wait(NULL); // Wait for the child to finish
        printf("PARENT: x is %d.\n", x);
    }

}

/**
 * @brief Question 2: Demonstrates shared file descriptions after fork().
 *
 * Both parent and child share the same underlying file description, including
 * the file offset (cursor). This results in a race condition where their
 * writes interleave based on scheduler timing.
 */
void question2() {
    printf("--- Running Question 2 ---\n");

    int fd = open("q2_output.txt", O_CREAT | O_WRONLY | O_TRUNC, 0644);
    if (fd < 0) {
        perror("open failed");
        exit(1);
    }

    pid_t pid = fork();
    if (pid < 0) {
        perror("fork failed");
        exit(1);
    } else if (pid == 0) {  // Child process
        const char* child_msg = "Message from the child process.\n";
        write(fd, child_msg, strlen(child_msg));
        exit(0);
    } else {                // Parent process
        const char* parent_msg = "Message from the parent process.\n";
        write(fd, parent_msg, strlen(parent_msg));
        wait(NULL);
    }
    close(fd);
    printf("Check 'q2_output.txt'. The messages may be in any order.\n");
}

/**
 * @brief Question 3: Tries to ensure child prints before parent without wait().
 *
 * This is not a guaranteed solution. A short sleep() in the parent gives the
 * OS scheduler a chance to run the child first, making the desired order
 * highly probable, but not certain.
 */
void question3() {
    printf("--- Running Question 3 ---\n");
    pid_t pid = fork();
    if (pid < 0) {
        perror("fork failed");
        exit(1);
    } else if (pid == 0) {  // Child process
        printf("hello\n");
        exit(0);
    } else {                // Parent process
        // By sleeping, we can give the scheduler a hint to execute after the child process
        sleep(1);
        printf("goodbye\n");
    }
}

/**
 * @brief Question 4: Demonstrates variants of the exec() system call.
 *
 * The letters indicate how arguments are passed:
 * l: arguments are a list (char*)
 * v: arguments are a vector (char*[])
 * p: the PATH environment variable is searched to find the executable
 * e: a custom environment is passed
 */
void question4() {
    printf("--- Running Question 4 ---\n");
    char* my_args[] = {"/bin/ls", "-l", "/", NULL};

    printf("\n--> Running execl():\n");
    if (fork() == 0) { // child process
        execl("/bin/ls", "ls", "-l", "/", NULL);
        perror("execl failed");      // This line only runs if execl fails
        exit(1);
    }
    wait(NULL);

    printf("\n--> Running execvp():\n");
    if (fork() == 0) {  // child process
        // 'p' means we don't need the full path "/bin/ls"
        execvp("ls", my_args+1); 
        perror("execvp failed");
        exit(1);
    }
    wait(NULL);
}

/**
 * @brief Question 5: Shows the return value of wait().
 *
 * wait() returns the PID of the terminated child. In a process with no
 * children, it returns -1.
 */
void question5() {
    printf("--- Running Question 5 ---\n");
    pid_t pid = fork();
    if (pid < 0) {
        perror("fork failed");
        exit(1);
    } else if (pid == 0) { // Child process
        printf("CHILD: I have no children to wait for.\n");
        pid_t waited_on = wait(NULL);
        printf("CHILD: wait() returned %d. Error: %s\n", waited_on, strerror(errno));
        exit(0);
    } else {                // Parent process
        printf("PARENT: I'm waiting for my child (PID: %d) to finish.\n", pid);
        pid_t waited_on = wait(NULL);
        printf("PARENT: My child has finished. wait() returned %d.\n", waited_on);
    }
}


/**
 * @brief Question 6: Demonstrates waitpid().
 *
 * waitpid() is useful for waiting on a specific child process or for
 * checking on children without blocking (using the WNOHANG option).
 */
void question6() {
    printf("--- Running Question 6 ---\n");
    pid_t child_pid = fork();
    if (child_pid < 0) {
        perror("fork failed");
        exit(1);
    } else if (child_pid == 0) { // Child process
        printf("CHILD: I am child with PID %d. I will sleep for 2 seconds.\n", getpid());
        sleep(2);
        printf("CHILD: I am done.\n");
        exit(0);
    } else {                    // Parent process
        printf("PARENT: Waiting specifically for child with PID %d.\n", child_pid);
        int status;
        pid_t waited_on = waitpid(child_pid, &status, 0); // 0 means no options
        printf("PARENT: Child %d finished. waitpid() returned %d.\n", child_pid, waited_on);
    }
}

/**
 * @brief Question 7: Closes standard output in the child.
 *
 * If a process closes STDOUT_FILENO (file descriptor 1), subsequent calls
 * to printf() will have no effect, as there is nowhere for the output to go.
 */
void question7() {
    printf("--- Running Question 7 ---\n");
    pid_t pid = fork();
    if (pid < 0) {
        perror("fork failed");
        exit(1);
    } else if (pid == 0) { // Child process
        printf("CHILD: This message should be visible.\n");

        // Close standard output
        close(STDOUT_FILENO);

        // This printf call will fail silently
        printf("CHILD: You should NOT see this message.\n");
        exit(0);
    } else { // Parent process
        wait(NULL);
        printf("PARENT: Child has finished. Note that one of its messages did not appear.\n");
    }
}

/**
 * @brief Question 8: Connects two children with a pipe.
 *
 * The standard output of the first child ("ls -l") is redirected to become
 * the standard input of the second child ("wc -l").
 */
void question8() {
    printf("--- Running Question 8 ---\n");
    int pipe_fd[2]; // [0] is for reading, [1] is for writing

    if (pipe(pipe_fd) == -1) {
        perror("pipe failed");
        exit(1);
    }

    // Fork for the first child (the writer, "ls -l")
    pid_t child1_pid = fork();
    if (child1_pid < 0) {
        perror("fork1 failed");
        exit(1);
    }

    if (child1_pid == 0) { // Child 1: The Writer ("ls -l")
        close(pipe_fd[0]); // This child doesn't read from the pipe
        // Redirect stdout (1) to the pipe's write end
        dup2(pipe_fd[1], STDOUT_FILENO);
        close(pipe_fd[1]); // Close original descriptor

        execlp("ls", "ls", "-l", NULL);
        perror("execlp ls failed"); // Should not be reached
        exit(1);
    }

    // Fork for the second child (the reader, "wc -l")
    pid_t child2_pid = fork();
    if (child2_pid < 0) {
        perror("fork2 failed");
        exit(1);
    }

    if (child2_pid == 0) { // Child 2: The Reader ("wc -l")
        close(pipe_fd[1]); // This child doesn't write to the pipe
        // Redirect stdin (0) to the pipe's read end
        dup2(pipe_fd[0], STDIN_FILENO);
        close(pipe_fd[0]); // Close original descriptor

        execlp("wc", "wc", "-l", NULL);
        perror("execlp wc failed"); // Should not be reached
        exit(1);
    }

    // Parent Process
    // The parent must close both ends of the pipe so that the reader (wc)
    // sees EOF when the writer (ls) is done.
    close(pipe_fd[0]);
    close(pipe_fd[1]);

    // Wait for both children to finish
    waitpid(child1_pid, NULL, 0);
    waitpid(child2_pid, NULL, 0);
    printf("--- Pipe demonstration complete ---\n");
}