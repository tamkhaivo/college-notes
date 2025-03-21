/* Time Complexity: O(V^2)
Space Complexity: O(V) */

function solution(graph, startingVertex) {
  const verticies = graph.length;
  const paths = new Array(verticies).fill(Infinity);
  const visitedVerticies = new Array(verticies).fill(false);
  paths[startingVertex] = 0;

  for (let vertexCycle = 0; vertexCycle < verticies; vertexCycle++) {
    let minDistance = Infinity;
    let minVertex = -1;
    // find the smallest distance between start vertex and neighboring vertex.
    for (let vertex = 0; vertex < verticies; vertex++) {
      if (!visitedVerticies[vertex] && paths[vertex] < minDistance) {
        minDistance = paths[vertex];
        minVertex = vertex;
      }
    }

    // if vertex is isolated return paths;
    if (minVertex == -1) {
      return paths;
    }
    visitedVerticies[minVertex] = true;

    // find the smallest distance between startingVertex to minVertex through other Verticies
    for (let neighbor = 0; neighbor < verticies; neighbor++) {
      if (graph[minVertex][neighbor] != -1) {
        let alternatePathDistance =
          paths[minVertex] + graph[minVertex][neighbor];
        if (alternatePathDistance < paths[neighbor]) {
          paths[neighbor] = alternatePathDistance;
        }
      }
    }
  }
  return paths;
}

// strongly connected directed graph
