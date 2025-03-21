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
