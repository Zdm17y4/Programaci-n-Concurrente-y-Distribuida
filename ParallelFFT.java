package concurrente;

import java.util.Random;
import java.util.Scanner;

public class ParallelFFT {

    public static void fftParallel(double[] real, double[] imag, int h) {
        int n = real.length;
        if (n <= 1) return;

        double[] evenReal = new double[n / 2];
        double[] evenImag = new double[n / 2];
        double[] oddReal = new double[n / 2];
        double[] oddImag = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            evenReal[i] = real[2 * i];
            evenImag[i] = imag[2 * i];
            oddReal[i] = real[2 * i + 1];
            oddImag[i] = imag[2 * i + 1];
        }

        if (h > 1) {
            final int newH = h / 2;

            Thread t1 = new Thread(() -> fftParallel(evenReal, evenImag, newH));
            Thread t2 = new Thread(() -> fftParallel(oddReal, oddImag, newH));

            t1.start();
            t2.start();

            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            fftParallel(evenReal, evenImag, 1);
            fftParallel(oddReal, oddImag, 1);
        }

        for (int k = 0; k < n / 2; k++) {
            double angle = -2 * Math.PI * k / n;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double treal = cos * oddReal[k] - sin * oddImag[k];
            double timag = sin * oddReal[k] + cos * oddImag[k];

            real[k]       = evenReal[k] + treal;
            imag[k]       = evenImag[k] + timag;
            real[k + n/2] = evenReal[k] - treal;
            imag[k + n/2] = evenImag[k] - timag;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Ingrese m (n = 2^m): ");
        int m = sc.nextInt();
        int n = 1 << m; // 2^m

        // Generar datos UNA SOLA VEZ
        double[] realOriginal = new double[n];
        double[] imagOriginal = new double[n];

        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            realOriginal[i] = rand.nextDouble() * 10; // valores aleatorios
            imagOriginal[i] = 0.0; // parte imaginaria inicial
        }

        System.out.println("Datos de entrada (parte real):");
        for (int i = 0; i < n; i++) {
            System.out.printf("%.2f ", realOriginal[i]);
        }
        System.out.println("\n");

        // Probar con diferentes cantidades de hilos
        int[] hilos = {2, 4, 6, 8, 10, 12};
        for (int h : hilos) {
            // Copiar los datos originales para cada prueba
            double[] real = realOriginal.clone();
            double[] imag = imagOriginal.clone();

            long startTime = System.currentTimeMillis();
            fftParallel(real, imag, h);
            long endTime = System.currentTimeMillis();

            System.out.println("Número de hilos: " + h);
            System.out.println("Tiempo total de ejecución: " + (endTime - startTime) + " ms\n");
        }

        sc.close();
    }
}
