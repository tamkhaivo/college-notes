/*
    Tam Vo 
    The purpose of this code is to reverse the input lines. 
    This algorithm uses a dynamic linked list to reverse the order of the lines.

    This algorithm will first link the input and output to the specified argument cases.
    By default it will take the standard input and output to a file pointer.
    If there are 2 valid arguments, it will check the uniqueness and make sure there are resources to open the files.
    If there are 1 valid arguement, it will check the resources to open a file.

    While keeping track of the currentNode for the linked list, it will continue to allocate a new node for each string line.  

    After completing this, the link list will unroll and output the answer.

    gcc -o reverse reverse.c -Wall -Werror -O
    ./test-reverse.sh
*/
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>

// Linked list node structure to store lines
typedef struct node {
    char *line;
    struct node *next;
} node_t;

node_t* create_node(char *line) {
    node_t *new_node = malloc(sizeof(node_t));
    if (new_node == NULL) {
        fprintf(stderr, "malloc failed\n");
        exit(1);
    }
    new_node->line = line;
    new_node->next = NULL;
    return new_node;
}

node_t* insert_at_head(node_t *head, node_t *node_to_insert) {
    node_to_insert->next = head;
    return node_to_insert;
}

void free_list(node_t *head) {
    node_t *current = head;
    while (current != NULL) {
        node_t *temp = current;
        current = current->next;
        free(temp->line); // Free the line string allocated by getline
        free(temp);       // Free the node itself
    }
}

int main(int argc, char *argv[]) {
    // Check for correct number of arguments
    if (argc > 3) {
        fprintf(stderr, "usage: reverse <input> <output>\n");
        exit(1);
    }

    // Case: ./reverse

    FILE *input = stdin;
    FILE *output = stdout;

    // Case: ./reverse input.txt output.txt
    if (argc == 3) {
        struct stat stat_in, stat_out;
        if (stat(argv[1], &stat_in) == 0 && stat(argv[2], &stat_out) == 0) {
            if (stat_in.st_dev == stat_out.st_dev && stat_in.st_ino == stat_out.st_ino) {
                fprintf(stderr, "reverse: input and output file must differ\n");
                exit(1);
            }
        }
        input = fopen(argv[1], "r");
        if (input == NULL) {
            fprintf(stderr, "reverse: cannot open file '%s'\n", argv[1]);
            exit(1);
        }
        output = fopen(argv[2], "w");
        if (output == NULL) {
            fprintf(stderr, "reverse: cannot open file '%s'\n", argv[2]);
            fclose(input);
            exit(1);
        }
    } 
    // Case: ./reverse input.txt
    else if (argc == 2) {
        input = fopen(argv[1], "r");
        if (input == NULL) {
            fprintf(stderr, "reverse: cannot open file '%s'\n", argv[1]);
            exit(1);
        }
    }

    
    // Read lines from input and store in a linked list
    node_t *head = NULL;
    char *line = NULL;
    size_t len = 0;

    while (getline(&line, &len, input) != -1) {
        node_t *new_node = create_node(line);
        head = insert_at_head(head, new_node);
        line = NULL; // getline will allocate a new buffer for the next line
        len = 0;
    }

    if (!feof(input)) {
        fprintf(stderr, "malloc failed\n");
        free(line);
        free_list(head);
        if (input != stdin) fclose(input);
        if (output != stdout) fclose(output);
        exit(1);
    }

    free(line); // Free the buffer if getline fails or hits EOF

    // Write lines from linked list to output
    node_t *current = head;
    while (current != NULL) {
        fprintf(output, "%s", current->line);
        current = current->next;
    }

    // Cleanup
    free_list(head);
    if (input != stdin) {
        fclose(input);
    }
    if (output != stdout) {
        fclose(output);
    }

    return 0;
}
