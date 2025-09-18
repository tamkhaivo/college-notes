// --- Grid Representation ---
// Define constants for grid elements to make the code more readable.
const BOX = "B";
const OBSTACLE = "O";
const EMPTY = ".";

/**
 * Detonates boxes in a 3x3 area centered on the obstacle.
 * It modifies the grid in place.
 * @param {string[][]} grid The n x m grid.
 * @param {number} obstacleRow The row index of the obstacle.
 * @param {number} obstacleCol The column index of the obstacle.
 */
function detonate(grid, obstacleRow, obstacleCol) {
  const numRows = grid.length;
  const numCols = grid[0].length;

  console.log(
    `Obstacle hit at [${obstacleRow}, ${obstacleCol}]. Detonating 3x3 area.`
  );

  // Iterate through the 3x3 area around the obstacle.
  for (let r = obstacleRow - 1; r <= obstacleRow + 1; r++) {
    for (let c = obstacleCol - 1; c <= obstacleCol + 1; c++) {
      // Check for grid boundaries to avoid errors.
      if (r >= 0 && r < numRows && c >= 0 && c < numCols) {
        // Detonate if the cell contains a box.
        if (grid[r][c] === BOX) {
          grid[r][c] = EMPTY; // The box is "detonated" and becomes empty space.
        }
      }
    }
  }
}

/**
 * Simulates a box moving in a given direction until it hits an obstacle or the edge.
 * @param {string[][]} grid The n x m grid.
 * @param {number} startRow The starting row of the box.
 * @param {number} startCol The starting column of the box.
 * @param {object} direction An object with dr (delta row) and dc (delta col).
 * e.g., { dr: 0, dc: 1 } for moving right.
 * @returns {string[][]} The modified grid after the simulation.
 */
function moveBox(grid, startRow, startCol, direction) {
  // Make a deep copy to avoid modifying the original grid directly.
  const newGrid = grid.map((row) => [...row]);

  if (newGrid[startRow][startCol] !== BOX) {
    console.log("No box at the starting position.");
    return newGrid;
  }

  let r = startRow;
  let c = startCol;

  // Clear the box's starting position.
  newGrid[r][c] = EMPTY;

  while (true) {
    // Calculate the next position.
    const nextR = r + direction.dr;
    const nextC = c + direction.dc;

    // Check for out-of-bounds.
    if (
      nextR < 0 ||
      nextR >= newGrid.length ||
      nextC < 0 ||
      nextC >= newGrid[0].length
    ) {
      console.log("Box moved off the grid.");
      break; // Stop if the box moves off the grid.
    }

    // Check for an obstacle.
    if (newGrid[nextR][nextC] === OBSTACLE) {
      detonate(newGrid, nextR, nextC);
      break; // Stop after hitting the obstacle and detonating.
    }

    // Check for another box (or any non-empty space).
    if (newGrid[nextR][nextC] !== EMPTY) {
      newGrid[r][c] = BOX; // Put the box back in its last valid position.
      console.log("Path blocked by another element.");
      break;
    }

    // Move to the next position.
    r = nextR;
    c = nextC;
  }

  return newGrid;
}

// --- Helper function to print the grid for visualization ---
function printGrid(grid) {
  grid.forEach((row) => console.log(row.join(" ")));
}

// --- Example Usage ---
let gameGrid = [
  [".", "B", ".", "B", "."],
  [".", "B", "B", "B", "."],
  [".", ".", ".", "O", "."],
  [".", ".", "B", "B", "B"],
  ["B", ".", ".", ".", "."],
];

console.log("Original Grid:");
printGrid(gameGrid);
console.log("\n--------------------------\n");

// Simulate the box at [0, 1] moving right.
const resultGrid = moveBox(gameGrid, 0, 1, { dr: 1, dc: 0 }); // Direction: right

console.log("\nGrid After Simulation:");
printGrid(resultGrid);
