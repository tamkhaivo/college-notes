import math

class primeIter:
    def __init__(self, listPrimes):
        self.n = len(listPrimes) - 1 
        self.i = -1
        self.primes = listPrimes
        return None

    def __next__(self):
        if(self.i >= self.n):
            raise StopIteration
        self.i += 1
        return self.primes[self.i]



class primes: 
    def __init__(self, n):
        prime = [True] * n
        for i in range(2, int(math.sqrt(n))+1):
            if prime[i]:
                for j in range(i*i, n, i):
                    prime[j] = False
        self.prime = [i for i in range(2, n) if prime[i]]

    def __iter__(self):
        return primeIter(self.prime)


print(primes(190))


