

# hamt_hw.py by Tam Vo (tamvo@csus.edu) for CSC 135 Spring 2025 
# Implements contains,  __iter__
# Worked with Ted Krovetz

# Set implemented as Hash Array Mapped Trie - Used in CSC 135, Sacramento State
# Written by Ted Krovetz, tdk@csus.edu, 2022-23
# This implementation requires objects in the set be hashable.
# Resulting trie will have expected log height if hash is random.
# In this implementaiton the root node always has _item == None


from functools import reduce



class hamt:
    
    DEG  = 4     # Children per node (can be set to any power of 2)
    
    def __init__(self, item = None, children = None):
        self._item = item
        if children == None:
            self._children = [None] * hamt.DEG  # List of DEG Nodes
        else:
            self._children = children
    
    # returns copy of self node with child i set to node x
    def _updatechild(self, i, x):
        updatedchildlist = list(self._children)    # copy self's child list
        updatedchildlist[i] = x                    # update child i
        return hamt(self._item, updatedchildlist)  # build and return new node

    # Returns reference to new root if change made, else None
    def _add(self, item, hashbits):
        if self._item == item:
            # This node matches item. Return None to indicate no change.
            return None
        else:
            # Continue search using hashbits to pick direction
            child_num = hashbits % hamt.DEG
            if self._children[child_num] == None:
                # item not in trie. Add it as new leaf node.
                return self._updatechild(child_num, hamt(item))
            else:
                # Ask appropriate child to add item, receive updated reference
                shiftedhashbits = hashbits // hamt.DEG
                newchild = self._children[child_num]._add(item, shiftedhashbits)
                if newchild == None:
                    return None
                else:
                    return self._updatechild(child_num, newchild)
    
    def add(self, item):
        # Pass item and hashbits to private recursive helper
        return self._add(item, hash(item))
    
    # Returns True if item in trie, else False. Expected rutime O(log n).
    # Should follow the same logic as add but should not create any new nodes
    def contains(self, item):
        return self._contains(item, hash(item))

    def _contains(self, item, hashbits):
        # if item in root 
        if self._item == item:
            return True
        else:
            # Continue search using hashbits to pick direction
            child_num = hashbits % hamt.DEG
            if self._children[child_num] == None:
                return False
            else:
                shiftedhashbits = hashbits // hamt.DEG
                if self._children[child_num] == item:
                    return True
                return self._contains(self._children[child_num], hash(item))
                


    # A breadth-first search of a tree follows this pseudocode and can
    # be used to organize your iterator:
    # Initialize a queue with reference(s) to the initial node(s) to visit
    # while the queue is not empty:
    #     remove next node from queue
    #     add the node's child references (if any) to the queue
    #     do whatever you want with the node
    def __iter__(self):
        return HamtIter(self)

    # Adds self's item string to acc list, then ask each child to do the same
    def _str(self, acc):
        if self._item != None:           # Don't do anything in fake root node
            acc.append(str(self._item))
        for child in self._children:
            if child != None:
                child._str(acc)
    
    # Build list of all items in trie, then join them with ", " in between
    def __str__(self):
        acc = []
        self._str(acc)
        return "{" + ", ".join(acc) + "}"

class HamtIter:
    # Iterator needs to know how to start
    def __init__(self, firstitem):
        self.queue = [firstitem.children]
        self.item

        
    def __next__(self):
        if(len(queue) == 0):
            raise StopIteration
        self.item = self.queue.pop(0)
        self.queue += self.queue + self.item.children
        return item
            
# The following is a trick to make this testing code be ignored
# when this file is being imported, but run when run directly
# https://codefather.tech/blog/if-name-main-python/
if __name__ == '__main__':
    a = hamt()
    b = a.add("B")
    c = b.add("C")
    d = c.add("D")
    e = d.add("E")
    f = e.add("F")
    g = f.add("F")

    print(a)
    print(b)
    print(c)
    print(d)
    print(e)
    print(f)
    print(f.contains("F"))
    print(f.contains("F"))
    print(f.contains("A"))
