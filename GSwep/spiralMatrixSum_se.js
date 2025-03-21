function spiralMatrixSum(matrix) {
  function updateDirection(currentDirection) {
    switch (currentDirection) {
      case DIRECTION.RIGHT:
        if (col + 1 >= COLS || visited[row][col + 1]) {
          currentDirection = DIRECTION.DOWN;
        }
        break;
      case DIRECTION.DOWN:
        if (row + 1 >= ROWS || visited[row + 1][col]) {
          currentDirection = DIRECTION.LEFT;
        }
        break;
      case DIRECTION.LEFT:
        if (col - 1 < 0 || visited[row][col - 1]) {
          currentDirection = DIRECTION.UP;
        }
        break;
      case DIRECTION.UP:
        if (row - 1 < 0 || visited[row - 1][col]) {
          currentDirection = DIRECTION.RIGHT;
        }
        break;
      default:
        break;
    }
    return currentDirection;
  }
  const DIRECTION = {
    RIGHT: [0, 1],
    DOWN: [1, 0],
    LEFT: [0, -1],
    UP: [-1, 0],
  };
  if (!matrix || !matrix.length) {
    return 0;
  }
  const ROWS = matrix.length;
  const COLS = ROWS > 0 ? matrix[0].length : 0;
  let row = 0;
  let col = 0;
  let spiralMatrixSum = 0;
  let visited = new Array(ROWS).fill().map(() => new Array(COLS).fill(false));
  let currentDirection = DIRECTION.RIGHT;
  while (!visited[row][col]) {
    visited[row][col] = true;
    spiralMatrixSum += matrix[row][col];
    currentDirection = updateDirection(currentDirection);
    row += currentDirection[0];
    col += currentDirection[1];
  }

  return spiralMatrixSum;
}

console.log(spiralMatrixSum([]));
