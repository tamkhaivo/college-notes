# CSC 139 Assignment 3 Submission

## Part 1: Hard Links

**Command:**
```bash
ls -li file1.txt
```

**Output:**
```text
117787238 -rw-r--r--@ 1 tvo  staff  45 Nov 24 11:45 file1.txt
```

**Command:**
```bash
ln file1.txt file2.txt
```

**Questions:**

*   **What are the inode values of `file1.txt` and `file2.txt`?**
    *   The inode values are identical. In this execution, both files have the inode number `117787238`.
    *   Output of `ls -li file1.txt file2.txt`:
        ```text
        117787238 -rw-r--r--@ 2 tvo  staff  45 Nov 24 11:45 file1.txt
        117787238 -rw-r--r--@ 2 tvo  staff  45 Nov 24 11:45 file2.txt
        ```

*   **Are they the same or different?**
    *   They are the **same**. A hard link is essentially another name for the exact same file (inode).

*   **Do the two files have the same—or different—contents?**
    *   They have the **same** contents because they point to the same data blocks on the disk.

**Action:**
*   Next, edit `file2.txt` and change its contents. After you have done so, examine the contents of `file1.txt`.

**Question:**

*   **Are the contents of `file1.txt` and `file2.txt` the same or different?**
    *   The contents remain the **same**. Any change made to `file2.txt` is immediately reflected in `file1.txt` because they are the same file.
    *   *Demonstration:*
        ```bash
        echo " appended text" >> file2.txt
        cat file1.txt
        ```
        *Output:*
        ```text
        The quick brown fox jumps over the lazy dog.
         appended text
        ```

**Action:**
*   Next, enter the following command which removes `file1.txt`: `rm file1.txt`

**Question:**

*   **Does `file2.txt` still exist as well?**
    *   **Yes**, `file2.txt` still exists.
    *   *Explanation:* Removing `file1.txt` only unlinks that specific name from the inode. The inode itself (and the data) is only deleted when the link count reaches zero. Since `file2.txt` still points to the inode, the file remains.
    *   *Output of `ls -l file2.txt`:*
        ```text
        -rw-r--r--@ 1 tvo  staff  60 Nov 24 11:45 file2.txt
        ```

## Part 2: System Calls

**Command:**
```bash
strace rm file2.txt
```

**Question:**

*   **What system call is used for removing `file2.txt`?**
    *   The system call used is **`unlink`** (or `unlinkat` on modern Linux systems).
    *   *Explanation:* The `rm` command uses the `unlink` system call to remove the directory entry for the file and decrement the link count of the inode.

## Part 3: Soft Links

**Command:**
```bash
ln -s file3.txt file4.txt
```

**Action:**
*   After you have done so, obtain the inode numbers of `file3.txt` and `file4.txt` using the command `ls -li file*.txt`.

**Questions:**

*   **Are the inodes the same, or is each unique?**
    *   Each inode is **unique**. A soft link (symbolic link) is a distinct file with its own inode that contains the path to the target file.
    *   *Output:*
        ```text
        117787239 -rw-r--r--@ 1 tvo  staff  41 Nov 24 11:45 file3.txt
        117787261 lrwxr-xr-x@ 1 tvo  staff   9 Nov 24 11:45 file4.txt -> file3.txt
        ```

*   **Next, edit the contents of `file4.txt`. Have the contents of `file3.txt` been altered as well?**
    *   **Yes**. When you edit a symbolic link, the operating system transparently redirects the operations to the target file (`file3.txt`).

*   **Last, delete `file3.txt`. After you have done so, explain what happens when you attempt to edit `file4.txt`.**
    *   When `file3.txt` is deleted, `file4.txt` becomes a **broken** or **dangling** link.
    *   Attempting to edit or view `file4.txt` will result in an error (e.g., "No such file or directory") because the file it points to no longer exists.
    *   *Demonstration:*
        ```bash
        cat file4.txt
        ```
        *Output:*
        ```text
        cat: file4.txt: No such file or directory
        ```
