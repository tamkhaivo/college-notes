import math

def keep_odd_indicies(xs): # Not Modifying CS and returning a new list 
    odd_list = []
    for x in xs:
        if x & 1:
            odd_list.append(x)
    return odd_list


def keep_odd_indicies_s(xs):
    i = 0
    while i < len(xs):
        xs.pop(i)
        i = i + 1


def exponential(a, b): # (2,16)  --> calls function 17 times with the 17th being 1
    if b == 0:
        return 1
    else: 
        return a * exponential(a, b-1)
    
def find_largest(arr, n):
    if n == 1:
        return arr[0]
    else:
        return max(arr[n-1], find_largest(arr, n-1))



def digit_sum(n):
    print(n)

    if(-10 < n and n < 10):
        return n
    else:
        if n < 0:
            return digit_sum(n//10) + (n%10 - 10)
        else:
            return digit_sum(math.floor(n/10)) + (n%10) 

def stutter_list(ints):
    if len(ints) == 0:
        return []
    return [ints[0], ints[0]] + stutter_list(ints[1:]) 

    


def count_equal(xs, x):
    if len(xs) == 0:
        return 0
    if xs[0] == x:
        return 1 + count_equal(xs[1:], x)
    else:
        return count_equal(xs[1:], x)


print(count_equal([1,2,2,3], 2))
