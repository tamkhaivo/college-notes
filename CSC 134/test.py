def reflect_tail(xs, acc = []):
    if len(xs) == len(acc):
        return xs + acc
    return reflect_tail(xs, acc + [xs[len(xs) - len(acc) - 1]])

