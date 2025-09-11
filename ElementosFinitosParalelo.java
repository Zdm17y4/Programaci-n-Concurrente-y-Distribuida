package elementosfinitos;

import java.util.Scanner;

/**
 * Implementación paralela del Método de Elementos Finitos para una barra 1D empotrada con carga
 * axial
 *
 * <p>Problema: Barra de acero empotrada en un extremo con carga en el otro - Longitud: 1.0 m - Área
 * transversal: 0.01 m² - Módulo de Young: 200 GPa (acero) - Carga aplicada: 10000 N
 */
public class ElementosFinitosParalelo {

  // Parámetros del problema físico
  private static final double LONGITUD = 1.0; // metros
  private static final double AREA = 0.01; // m²
  private static final double MODULO_YOUNG = 200e9; // Pa (200 GPa para acero)
  private static final double CARGA = 10000; // N (Newton)

  // Variables compartidas entre hilos
  private double[][] matrizGlobal;
  private double[] vectorFuerzas;
  private double[] desplazamientos;
  private int numElementos;
  private int numNodos;
  private int numHilos;

  // Para sincronización
  private final Object lock = new Object();

  public static void main(String[] args) {
    new ElementosFinitosParalelo().ejecutar();
  }

  public void ejecutar() {
    Scanner scanner = new Scanner(System.in);

    // Entrada del usuario
    System.out.print("Ingrese el número de elementos finitos: ");
    numElementos = scanner.nextInt();

    System.out.print("Ingrese el número de hilos a usar: ");
    numHilos = scanner.nextInt();

    scanner.close();

    // Validación básica
    if (numElementos <= 0 || numHilos <= 0) {
      System.out.println("Error: Los valores deben ser positivos");
      return;
    }

    numNodos = numElementos + 1;

    // Inicializar estructuras de datos
    matrizGlobal = new double[numNodos][numNodos];
    vectorFuerzas = new double[numNodos];
    desplazamientos = new double[numNodos];

    System.out.println("\n=== INICIANDO CÁLCULO DE ELEMENTOS FINITOS ===");
    System.out.println("Número de elementos: " + numElementos);
    System.out.println("Número de nodos: " + numNodos);
    System.out.println("Número de hilos: " + numHilos);
    System.out.println("Longitud de cada elemento: " + (LONGITUD / numElementos) + " m");

    long tiempoInicio = System.currentTimeMillis();

    // PASO 1: Ensamblar matriz de rigidez en paralelo
    ensamblarMatrizParalelo();

    // PASO 2: Aplicar condiciones de frontera
    aplicarCondicionesFrontera();

    // PASO 3: Resolver sistema de ecuaciones (Gauss-Seidel paralelo)
    resolverSistemaParalelo();

    long tiempoFin = System.currentTimeMillis();

    // Mostrar resultados
    mostrarResultados();

    System.out.println("\nTiempo total de cálculo: " + (tiempoFin - tiempoInicio) + " ms");
  }

  /** Ensambla la matriz de rigidez global usando múltiples hilos */
  private void ensamblarMatrizParalelo() {
    System.out.println("\n--- Ensamblando matriz de rigidez en paralelo ---");

    int elementosPorHilo = numElementos / numHilos;
    int elementosRestantes = numElementos % numHilos;

    Thread[] hilos = new Thread[numHilos];

    for (int i = 0; i < numHilos; i++) {
      int inicio = i * elementosPorHilo;
      int fin = (i + 1) * elementosPorHilo;

      // El último hilo toma los elementos restantes
      if (i == numHilos - 1) {
        fin += elementosRestantes;
      }

      hilos[i] = new HiloEnsamblador(i, inicio, fin);
      hilos[i].start();
    }

    // Esperar a que todos los hilos terminen
    for (int i = 0; i < numHilos; i++) {
      try {
        hilos[i].join();
      } catch (InterruptedException e) {
        System.out.println("Error en hilo " + i + ": " + e.getMessage());
      }
    }
  }

  /** Hilo para ensamblar elementos de la matriz de rigidez */
  class HiloEnsamblador extends Thread {
    private int idHilo;
    private int elementoInicio;
    private int elementoFin;

    public HiloEnsamblador(int id, int inicio, int fin) {
      this.idHilo = id;
      this.elementoInicio = inicio;
      this.elementoFin = fin;
    }

    @Override
    public void run() {
      System.out.println(
          "Hilo "
              + idHilo
              + " procesando elementos ["
              + elementoInicio
              + " - "
              + (elementoFin - 1)
              + "]");

      double longitudElemento = LONGITUD / numElementos;
      double rigidezElemento = (AREA * MODULO_YOUNG) / longitudElemento;

      for (int elem = elementoInicio; elem < elementoFin; elem++) {
        // Matriz de rigidez local del elemento (2x2)
        double[][] kLocal = {
          {rigidezElemento, -rigidezElemento},
          {-rigidezElemento, rigidezElemento}
        };

        // Nodos del elemento
        int nodo1 = elem;
        int nodo2 = elem + 1;

        // Ensamblar en la matriz global (con sincronización)
        synchronized (lock) {
          matrizGlobal[nodo1][nodo1] += kLocal[0][0];
          matrizGlobal[nodo1][nodo2] += kLocal[0][1];
          matrizGlobal[nodo2][nodo1] += kLocal[1][0];
          matrizGlobal[nodo2][nodo2] += kLocal[1][1];
        }
      }

      System.out.println("Hilo " + idHilo + " completado");
    }
  }

  /** Aplica las condiciones de frontera al sistema */
  private void aplicarCondicionesFrontera() {
    // System.out.println("\n--- Aplicando condiciones de frontera ---");

    // Condición 1: Nodo 0 está empotrado (desplazamiento = 0)
    // Modificamos la primera fila y columna
    for (int i = 0; i < numNodos; i++) {
      matrizGlobal[0][i] = 0;
      matrizGlobal[i][0] = 0;
    }
    matrizGlobal[0][0] = 1;
    vectorFuerzas[0] = 0;

    // Condición 2: Carga aplicada en el último nodo
    vectorFuerzas[numNodos - 1] = CARGA;

    System.out.println("Empotramiento en nodo 0");
    // System.out.println("Carga de " + CARGA + " N aplicada en nodo " + (numNodos -
    // 1));
  }

  /** Resuelve el sistema de ecuaciones usando Gauss-Seidel paralelo */
  private void resolverSistemaParalelo() {
    System.out.println("\n--- Resolviendo sistema de ecuaciones ---");

    int maxIteraciones = 1000;
    double tolerancia = 1e-10;
    boolean convergencia = false;

    // Inicializar desplazamientos
    for (int i = 0; i < numNodos; i++) {
      desplazamientos[i] = 0;
    }

    // Iteraciones de Gauss-Seidel
    for (int iter = 0; iter < maxIteraciones && !convergencia; iter++) {
      double[] desplazamientosAnteriores = desplazamientos.clone();

      // Dividir el trabajo entre hilos
      int nodosPorHilo = numNodos / numHilos;
      Thread[] hilos = new Thread[numHilos];

      for (int h = 0; h < numHilos; h++) {
        int inicio = h * nodosPorHilo;
        int fin = (h == numHilos - 1) ? numNodos : (h + 1) * nodosPorHilo;

        hilos[h] = new HiloResolver(inicio, fin);
        hilos[h].start();
      }

      // Esperar a que terminen
      for (Thread hilo : hilos) {
        try {
          hilo.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      // Verificar convergencia
      double error = 0;
      for (int i = 0; i < numNodos; i++) {
        error += Math.abs(desplazamientos[i] - desplazamientosAnteriores[i]);
      }

      if (error < tolerancia) {
        convergencia = true;
        // System.out.println("Convergencia alcanzada en iteración " + iter);
      }
    }

    if (!convergencia) {
      // System.out.println("Advertencia: No se alcanzó convergencia");
    }
  }

  /** Hilo para resolver parte del sistema de ecuaciones */
  class HiloResolver extends Thread {
    private int nodoInicio;
    private int nodoFin;

    public HiloResolver(int inicio, int fin) {
      this.nodoInicio = inicio;
      this.nodoFin = fin;
    }

    @Override
    public void run() {
      for (int i = nodoInicio; i < nodoFin; i++) {
        if (i == 0) continue; // Nodo empotrado

        double suma = vectorFuerzas[i];
        for (int j = 0; j < numNodos; j++) {
          if (i != j) {
            suma -= matrizGlobal[i][j] * desplazamientos[j];
          }
        }

        if (Math.abs(matrizGlobal[i][i]) > 1e-10) {
          synchronized (lock) {
            desplazamientos[i] = suma / matrizGlobal[i][i];
          }
        }
      }
    }
  }

  /** Muestra los resultados del análisis */
  private void mostrarResultados() {
    // System.out.println("\n=== RESULTADOS DEL ANÁLISIS ===");
    // System.out.println("\nDesplazamientos nodales:");
    // System.out.println("Nodo\tPosición(m)\tDesplazamiento(m)\tDesplazamiento(mm)");
    // System.out.println("----\t-----------\t-----------------\t------------------");

    double longitudElemento = LONGITUD / numElementos;

    for (int i = 0; i < numNodos; i++) {
      double posicion = i * longitudElemento;
      /*
       * System.out.printf(
       * "%d\t%.4f\t\t%.6e\t\t%.4f\n", i, posicion, desplazamientos[i],
       * desplazamientos[i] * 1000);
       */
    }

    /*
     * Comento este trozo para que salida en la terminal salga mas limpia
     * // Calcular y mostrar deformaciones y esfuerzos
     * System.out.println("\nDeformaciones y esfuerzos por elemento:");
     * System.out.println("Elemento\tDeformación\tEsfuerzo(Pa)\tEsfuerzo(MPa)");
     * System.out.println("--------\t-----------\t------------\t-------------");
     *
     * for (int elem = 0; elem < numElementos; elem++) {
     * double deformacion = (desplazamientos[elem + 1] - desplazamientos[elem]) /
     * longitudElemento;
     * double esfuerzo = MODULO_YOUNG * deformacion;
     *
     * System.out.printf("%d\t\t%.6e\t%.6e\t%.2f\n", elem, deformacion, esfuerzo,
     * esfuerzo / 1e6);
     * }
     */
    // Verificación teórica
    double desplazamientoTeorico = (CARGA * LONGITUD) / (AREA * MODULO_YOUNG);
    System.out.println("\n=== VERIFICACIÓN ===");
    System.out.println("Desplazamiento teórico del extremo: " + desplazamientoTeorico + " m");
    System.out.println(
        "Desplazamiento calculado del extremo: " + desplazamientos[numNodos - 1] + " m");
    double error =
        Math.abs(desplazamientoTeorico - desplazamientos[numNodos - 1])
            / desplazamientoTeorico
            * 100;
    // System.out.printf("Error relativo: %.2f%%\n", error);
  }
}
