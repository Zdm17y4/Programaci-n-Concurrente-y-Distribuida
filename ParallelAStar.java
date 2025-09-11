package concurrente;
import java.util.*;

class Node {
    int x, y;
    double g, h;
    Node parent;

    Node(int x, int y) {
        this.x = x;
        this.y = y;
    }

    double f() {
        return g + h;
    }
}

class AStarThread extends Thread {
    private int[][] grid;
    private Node start, goal;
    private int heuristicType; // 0 = Manhattan, 1 = Euclidiana, 2 = Chebyshev

    AStarThread(int[][] grid, Node start, Node goal, String name, int heuristicType) {
        super(name);
        this.grid = grid;
        this.start = new Node(start.x, start.y); // copiar coordenadas
        this.goal = new Node(goal.x, goal.y);
        this.heuristicType = heuristicType;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        List<Node> path = aStarSearch();
        long endTime = System.currentTimeMillis();

        if (path != null) {
            System.out.println("Meta encontrada por " + getName() +
                    " usando heurística " + heuristicType +
                    " en " + (endTime - startTime) + " ms");
            System.out.println("Ruta encontrada: " + path.size() + " pasos\n");
            System.out.println();
        } else {
            System.out.println("No se encontró ruta por " + getName() +
                    " usando heurística " + heuristicType);
        }
    }

    private List<Node> aStarSearch() {
        int n = grid.length;
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        boolean[][] closed = new boolean[n][n];

        start.g = 0;
        start.h = heuristic(start, goal);
        open.add(start);

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (current.x == goal.x && current.y == goal.y) {
                return reconstructPath(current);
            }

            closed[current.x][current.y] = true;

            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                if (nx < 0 || ny < 0 || nx >= n || ny >= n) continue;
                if (grid[nx][ny] == 1 || closed[nx][ny]) continue;

                Node neighbor = new Node(nx, ny);
                neighbor.g = current.g + 1;
                neighbor.h = heuristic(neighbor, goal);
                neighbor.parent = current;

                open.add(neighbor);
            }
        }
        return null;
    }

    private double heuristic(Node a, Node b) {
        switch (heuristicType) {
            case 1: // Euclidiana
                return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
            case 2: // Chebyshev
                return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
            default: // Manhattan
                return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
        }
    }

    private List<Node> reconstructPath(Node current) {
        List<Node> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }
}

public class ParallelAStar {
    public static void main(String[] args) {
        int size = 50; 
        int[][] grid = new int[size][size];
        Random rand = new Random();

        // generar obstáculos (30% probabilidad)
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = rand.nextDouble() < 0.3 ? 1 : 0;
            }
        }

        // generar inicio y meta
        Node start = new Node(rand.nextInt(size), rand.nextInt(size));
        Node goal;
        do {
            goal = new Node(rand.nextInt(size), rand.nextInt(size));
        } while ((goal.x == start.x && goal.y == start.y));

        grid[start.x][start.y] = 0;
        grid[goal.x][goal.y] = 0;

        System.out.println("Inicio: (" + start.x + "," + start.y + ")");
        System.out.println("Meta:   (" + goal.x + "," + goal.y + ")\n");

        // diferentes cantidades de hilos
        int[] hilos = {2, 4, 6, 8, 10, 12};
        for (int h : hilos) {
            Thread[] threads = new Thread[h];

            long globalStart = System.currentTimeMillis();

            for (int i = 0; i < h; i++) {
                threads[i] = new AStarThread(grid, start, goal, "Hilo-" + (i + 1), i % 3);
                threads[i].start();
            }

            for (int i = 0; i < h; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            long globalEnd = System.currentTimeMillis();
            System.out.println("Número de hilos: " + h);
            System.out.println("Tiempo total de ejecución (paralelo) con "+ h + " hilos:  " + (globalEnd - globalStart) + " ms\n");
        }
    }
}
