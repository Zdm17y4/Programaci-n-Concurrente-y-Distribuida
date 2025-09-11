import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Implements a parallel version of the Shell Sort algorithm.
public class ShellSortParallel {
    // Sorts an array in parallel using the Shell Sort algorithm.
    public void sort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }

        // Calculate the gap sequence using Knuth's formula.
        ArrayList<Integer> gaps = new ArrayList<>();
        int h = 1; //'h' represents the current gap size.
        while (h < arr.length / 3) {
            h = 3 * h + 1;
        }
        while (h >= 1) {
            gaps.add(h);
            h = h / 3;
        }

        // Iterate over each gap from largest to smallest.
        for (int gap : gaps) {
            
            List<Thread> threads = new ArrayList<>(); // Holds the threads for the current gap iteration.

            for (int i = 0; i < gap; i++) {
                // Create a thread to sort the sub-list for the current start index.
                final int startIndex = i; // The starting index for this thread's sub-list.
                Thread thread = new Thread(() -> {
                    insertionSortWithGap(arr, startIndex, gap);
                });
                
                thread.start();
                threads.add(thread);
            }

            // Wait for all threads for the current gap to finish before proceeding.
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Performs an insertion sort on a sub-list defined by a starting index and a gap.
     */
    private void insertionSortWithGap(int[] arr, int start, int gap) {
        for (int i = start + gap; i < arr.length; i += gap) {
            int temp = arr[i]; // The element to be inserted.
            int j = i;

            // Shift larger elements to the right.
            while (j >= gap && arr[j - gap] > temp) {
                arr[j] = arr[j - gap];
                j -= gap;
            }
            arr[j] = temp; // Insert the element in its correct position.
        }
    }

    // Main method to test the parallel Shell Sort algorithm.
    public static void main(String[] args) {
        Random rand = new Random();
        int n = 100; 
        int [] data = new int[n];
        for (int i = 0; i < n; i++) {
            data[i] = rand.nextInt(100);
        }

        System.out.println("Original array: " + Arrays.toString(data));

        ShellSortParallel sorter = new ShellSortParallel();
        sorter.sort(data);

        System.out.println("Sorted array: " + Arrays.toString(data));
    }
}