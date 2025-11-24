# CSC 139 Assignment 3: Files and Inodes

The following exercise is from the text book (ch.14) examines the relationship between files and inodes on a UNIX or Linux system. On these systems, files are represented with inodes. That is, an inode is a file (and vice versa). You can complete this exercise on the Linux virtual machine that is provided with this textbook. You can also complete the exercise on any Linux, UNIX, or macOS system, but it will require creating two simple text files named `file1.txt` and `file3.txt` whose contents are unique sentences.

In the source code available with this text (or your created file), open `file1.txt` and examine its contents. Next, obtain the inode number of this file with the command:

```bash
ls -li file1.txt
```

This will produce output similar to the following:

```text
16980 -rw-r--r-- 2 os os 22 Sep 14 16:13 file1.txt
```

where the inode number is boldfaced. (The inode number of `file1.txt` is likely to be different on your system.)

The UNIX `ln` command creates a link between a source and target file. This command works as follows:

```bash
ln [-s] <source file> <target file>
```

UNIX provides two types of links: (1) hard links and (2) soft links. A hard link creates a separate target file that has the same inode as the source file. Enter the following command to create a hard link between `file1.txt` and `file2.txt`:

```bash
ln file1.txt file2.txt
```

**Questions:**
*   What are the inode values of `file1.txt` and `file2.txt`?
*   Are they the same or different?
*   Do the two files have the same—or different—contents?

Next, edit `file2.txt` and change its contents. After you have done so, examine the contents of `file1.txt`.

**Question:**
*   Are the contents of `file1.txt` and `file2.txt` the same or different?

Next, enter the following command which removes `file1.txt`:

```bash
rm file1.txt
```

**Question:**
*   Does `file2.txt` still exist as well?

Now examine the man pages for both the `rm` and `unlink` commands. Afterwards, remove `file2.txt` by entering the command:

```bash
strace rm file2.txt
```

The `strace` command traces the execution of system calls as the command `rm file2.txt` is run.

**Question:**
*   What system call is used for removing `file2.txt`?

A soft link (or symbolic link) creates a new file that “points” to the name of the file it is linking to. In the source code available with this text, create a soft link to `file3.txt` by entering the following command:

```bash
ln -s file3.txt file4.txt
```

After you have done so, obtain the inode numbers of `file3.txt` and `file4.txt` using the command:

```bash
ls -li file*.txt
```

**Questions:**
*   Are the inodes the same, or is each unique?
*   Next, edit the contents of `file4.txt`. Have the contents of `file3.txt` been altered as well?
*   Last, delete `file3.txt`. After you have done so, explain what happens when you attempt to edit `file4.txt`.

## Submission

1.  A detailed PDF explaining the steps above and the results/answers to the questions with screenshots of performing/running the steps with results.
2.  The text files you created and any other supportive files.

