import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BucketSortParallel {
    static int[] arr;                 // global array
    static List<Integer>[] buckets;   // shared buckets
    static int h = 10;   // number of buckets/ threads

    public static void main(String[] args) {
        int n = 20; // amount of data 
        arr = new int[n];
        Random rand = new Random();

        // fill array with random numbers between 0 and 99
        for (int i = 0; i < n; i++) {
            arr[i] = rand.nextInt(100);
        }

        // shows the array
        System.out.println("Original array:");
        printArray(arr);

        // initializing buckets
        buckets = new ArrayList[h];
        for (int i = 0; i < h; i++) {
            buckets[i] = new ArrayList<>();
        }

        // asign elements to the buckets
        for (int value : arr) {
            int bucketIndex = (value * h) / 100;   //range between 0 - 99
            buckets[bucketIndex].add(value);
        }

        Thread[] workers = new Thread[h]; //launch threads
        // each thread sorts a buckets
        for (int i = 0; i < h; i++) {
            workers[i] = new BucketWorker(i);
            workers[i].start();
        }

        // wait for all threads to finish
        for (int i = 0; i < h; i++) {
            try {
                workers[i].join();
            } catch (InterruptedException e) {
                System.out.println("Error: " + e);
            }
        }

        // concatenate all sorted buckets
        int index = 0;
        for (int i = 0; i < h; i++) {
            for (int val : buckets[i]) {
                arr[index++] = val;
            }
        }

        System.out.println("Sorted array:");
        printArray(arr);
    }

    // worker thread: sorts a single bucket
    static class BucketWorker extends Thread {
        int id;
        BucketWorker(int id) {
            this.id = id;
        }

        public void run() {
            System.out.println("Thread " + id + " sorting bucket with " + buckets[id].size() + " elements");
            Collections.sort(buckets[id]);
        }
    }

    // method to print array
    static void printArray(int[] array) {
        for (int val : array) {
            System.out.print(val + " ");
        }
        System.out.println();
    }
}