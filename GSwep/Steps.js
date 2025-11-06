function solution(steps) {
  // Dynamic Programming = Constant Space
  let prevAnswers = [1, 1]; // prev - 1, prev - 2
  let prevAnswerIdx = false;

  for (let step = 2; step <= steps; step++) {
    prevAnswerIdx = !prevAnswerIdx;
    prevAnswers[Number(prevAnswerIdx)] = prevAnswers[0] + prevAnswers[1];
  }
  return prevAnswers[Number(prevAnswerIdx)];
}

// log(n) search on sorted array
function binarySeach(array, value) {
  let left = 0;
  let right = array.length;

  while (left <= right) {
    mid = Math.floor((left + right) / 2);

    if (array[mid] == value) {
      return mid;
    }
    if (array[mid] < value) {
      left = mid + 1;
    } else {
      right = mid - 1;
    }
  }
  return mid;
}
