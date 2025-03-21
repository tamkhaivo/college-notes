/*
You are given an integer array coins representing coins of different denominations 
and an integer amount representing a total amount of money.

Return the fewest number of coins that you need to make up that amount. 
If that amount of money cannot be made up by any combination of the coins, return -1.

You may assume that you have an infinite number of each kind of coin.

*/

function coinChange(coins, amount) {
  function initalizeMap() {
    const map = new Map();
    for (let index = 0; index <= amount; index++) {
      map.set(index, Infinity);
    }
    map.set(0, 0);
    return map;
  }
  const minCoins = initalizeMap();
  for (let initalAmount = 1; initalAmount <= amount; initalAmount++) {
    for (const coinDenomination of coins) {
      if (initalAmount - coinDenomination >= 0) {
        minCoins.set(
          initalAmount,
          Math.min(
            minCoins.get(initalAmount),
            1 + minCoins.get(initalAmount - coinDenomination)
          )
        );
      }
    }
  }
  return minCoins.get(amount) === Infinity ? -1 : minCoins.get(amount);
}

function coinChange(coins, amount) {
  const minCoins = new Array(amount + 1).fill(Infinity);
  minCoins[0] = 0;
  for (let initalAmount = 1; initalAmount <= amount; initalAmount++) {
    for (const coinDenomination of coins) {
      if (initalAmount - coinDenomination >= 0) {
        minCoins[initalAmount] = Math.min(
          minCoins[initalAmount],
          1 + minCoins[initalAmount - coinDenomination]
        );
      }
    }
  }
  return minCoins[amount] === Infinity ? -1 : minCoins[amount];
}

console.log(coinChange([1, 2, 5], 11));
