import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.Locale;

public class BenchmarkOrdenacao {

    private static int[] gerarEntrada(int tamanho, String tipo, long seed) {
        int[] a = new int[tamanho];
        Random rand = new Random(seed);

        switch (tipo) {
            case "Aleatorio":
                for (int i = 0; i < tamanho; i++) {
                    a[i] = rand.nextInt(100000);
                }
                break;

            case "Ordenado":
                for (int i = 0; i < tamanho; i++) {
                    a[i] = i;
                }
                break;

            case "Inverso":
                for (int i = 0; i < tamanho; i++) {
                    a[i] = tamanho - 1 - i;
                }
                break;

            default:
                throw new IllegalArgumentException(
                    "Tipo de entrada desconhecido: " + tipo
                );
        }

        return a;
    }

    private static boolean estaOrdenado(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    private static void executarOrdenacao(
            String algoritmo, int[] a, int threads)
            throws InterruptedException {

        switch (algoritmo) {
            case "MergeSort":
                if (threads == 1) {
                    mergeSortSerial(a, 0, a.length - 1);
                } else {
                    try (ForkJoinPool pool =
                            new ForkJoinPool(threads)) {
                        pool.invoke(
                            new MergeSortTask(a, 0, a.length - 1)
                        );
                    }
                }
                break;

            case "QuickSort":
                if (threads == 1) {
                    quickSortSerial(a, 0, a.length - 1);
                } else {
                    try (ForkJoinPool pool =
                            new ForkJoinPool(threads)) {
                        pool.invoke(
                            new QuickSortTask(a, 0, a.length - 1)
                        );
                    }
                }
                break;

            case "InsertionSort":
                if (threads == 1) {
                    insertionSortSerial(a);
                } else {
                    insertionSortParallel(a, threads);
                }
                break;

            case "BubbleSort":
                if (threads == 1) {
                    bubbleSortSerial(a);
                } else {
                    bubbleSortParallel(a, threads);
                }
                break;

            default:
                throw new IllegalArgumentException(
                    "Algoritmo desconhecido: " + algoritmo
                );
        }
    }


    // --- 1. MERGE SORT (Serial e Paralelo com ForkJoin) ---
    public static void mergeSortSerial(int[] a, int l, int r) {
        if (l < r) {
            int m = l + (r - l) / 2;
            mergeSortSerial(a, l, m);
            mergeSortSerial(a, m + 1, r);
            merge(a, l, m, r);
        }
    }

    private static void merge(int[] a, int l, int m, int r) {
        int n1 = m - l + 1;
        int n2 = r - m;
        int[] L = new int[n1];
        int[] R = new int[n2];
        System.arraycopy(a, l, L, 0, n1);
        System.arraycopy(a, m + 1, R, 0, n2);

        int i = 0, j = 0, k = l;
        while (i < n1 && j < n2) {
            if (L[i] <= R[j]) a[k++] = L[i++];
            else a[k++] = R[j++];
        }
        while (i < n1) a[k++] = L[i++];
        while (j < n2) a[k++] = R[j++];
    }

    static class MergeSortTask extends RecursiveAction {
        private final int[] a;
        private final int l, r;
        private static final int THRESHOLD = 1000;

        public MergeSortTask(int[] a, int l, int r) {
            this.a = a;
            this.l = l;
            this.r = r;
        }

        @Override
        protected void compute() {
            if (r - l < THRESHOLD) {
                mergeSortSerial(a, l, r);
            } else {
                int m = l + (r - l) / 2;
                MergeSortTask leftTask = new MergeSortTask(a, l, m);
                MergeSortTask rightTask = new MergeSortTask(a, m + 1, r);
                invokeAll(leftTask, rightTask);
                merge(a, l, m, r);
            }
        }
    }

    // --- 2. QUICK SORT (Serial e Paralelo) ---
    public static void quickSortSerial(int[] a, int low, int high) {
        if (low < high) {
            int pi = partition(a, low, high);
            quickSortSerial(a, low, pi - 1);
            quickSortSerial(a, pi + 1, high);
        }
    }

    
    private static int partition(int[] a, int low, int high) {
        // Escolhe o elemento central como pivô.
        int middle = low + (high - low) / 2;

        int temp = a[middle];
        a[middle] = a[high];
        a[high] = temp;

        int pivot = a[high];
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (a[j] <= pivot) {
                i++;

                temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }

        temp = a[i + 1];
        a[i + 1] = a[high];
        a[high] = temp;

        return i + 1;
    }


    static class QuickSortTask extends RecursiveAction {
        private final int[] a;
        private final int low, high;
        private static final int THRESHOLD = 1000;

        public QuickSortTask(int[] a, int low, int high) {
            this.a = a;
            this.low = low;
            this.high = high;
        }

        @Override
        protected void compute() {
            if (high - low < THRESHOLD) {
                quickSortSerial(a, low, high);
            } else if (low < high) {
                int pi = partition(a, low, high);
                QuickSortTask leftTask = new QuickSortTask(a, low, pi - 1);
                QuickSortTask rightTask = new QuickSortTask(a, pi + 1, high);
                invokeAll(leftTask, rightTask);
            }
        }
    }

    // --- 3. INSERTION SORT (Serial e Paralelo por Blocos) ---
    public static void insertionSortSerial(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    

    public static void insertionSortParallel(int[] a, int numThreads)
            throws InterruptedException {

        if (numThreads <= 0) {
            throw new IllegalArgumentException(
                "O número de threads deve ser positivo."
            );
        }

        int n = a.length;

        if (n <= 1) {
            return;
        }

        // Evita criar mais threads do que elementos.
        int effectiveThreads = Math.min(numThreads, n);

        // Cada bloco terá, no máximo, esse tamanho.
        int chunk = (n + effectiveThreads - 1) / effectiveThreads;

        Thread[] threads = new Thread[effectiveThreads];
        int threadCount = 0;

        // 1. Ordenação paralela dos blocos.
        for (int i = 0; i < effectiveThreads; i++) {
            final int start = i * chunk;
            final int end = Math.min(start + chunk, n);

            if (start >= end) {
                continue;
            }

            threads[threadCount] = new Thread(() -> {
                insertionSortRange(a, start, end);
            });

            threads[threadCount].start();
            threadCount++;
        }

        // Espera todos os blocos terminarem.
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }

        // 2. Mesclagem dos blocos já ordenados.
        int[] temp = new int[n];

        for (int width = chunk; width < n; ) {
            for (int left = 0; left < n; left += 2 * width) {
                int mid = (int) Math.min((long) left + width, n);
                int right = (int) Math.min((long) left + 2L * width, n);

                if (mid < right) {
                    mergeRanges(a, temp, left, mid, right);
                }
            }

            if (width > n / 2) {
                break;
            }

            width *= 2;
        }
    }

    // Insertion Sort aplicado somente ao intervalo [start, end).
    private static void insertionSortRange(
            int[] a, int start, int end) {

        for (int i = start + 1; i < end; i++) {
            int key = a[i];
            int j = i - 1;

            while (j >= start && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }

            a[j + 1] = key;
        }
    }

    // Intercala dois intervalos ordenados:
    // [left, mid) e [mid, right).
    private static void mergeRanges(
            int[] a, int[] temp,
            int left, int mid, int right) {

        int i = left;
        int j = mid;
        int k = left;

        while (i < mid && j < right) {
            if (a[i] <= a[j]) {
                temp[k++] = a[i++];
            } else {
                temp[k++] = a[j++];
            }
        }

        while (i < mid) {
            temp[k++] = a[i++];
        }

        while (j < right) {
            temp[k++] = a[j++];
        }

        System.arraycopy(temp, left, a, left, right - left);
    }



    // --- 4. BUBBLE SORT (Serial e Paralelo com Odd-Even Transposition) ---
    public static void bubbleSortSerial(int[] a) {
        int n = a.length;
        boolean swapped = true;
        for (int i = 0; i < n - 1 && swapped; i++) {
            swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (a[j] > a[j + 1]) {
                    int temp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = temp;
                    swapped = true;
                }
            }
        }
    }

    
    public static void bubbleSortParallel(int[] a, int numThreads)
            throws InterruptedException {

        if (numThreads <= 0) {
            throw new IllegalArgumentException(
                "O número de threads deve ser positivo."
            );
        }

        int n = a.length;

        if (n <= 1) {
            return;
        }

        // Evita criar mais threads do que pares disponíveis.
        int maxPairs = (n + 1) / 2;
        int effectiveThreads = Math.min(numThreads, maxPairs);

        // Cada fase compara pares de elementos vizinhos.
        for (int phase = 0; phase < n; phase++) {

            final int parity = phase % 2;
            Thread[] threads = new Thread[effectiveThreads];

            for (int t = 0; t < effectiveThreads; t++) {
                final int threadId = t;

                threads[t] = new Thread(() -> {

                    // Cada thread recebe pares distintos.
                    for (int j = parity + 2 * threadId;
                        j < n - 1;
                        j += 2 * effectiveThreads) {

                        if (a[j] > a[j + 1]) {
                            int temp = a[j];
                            a[j] = a[j + 1];
                            a[j + 1] = temp;
                        }
                    }
                });

                threads[t].start();
            }

            // Todas as threads devem terminar esta fase
            // antes de a próxima fase começar.
            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    // --- MAIN DE TESTE E GERAÇÃO DE CSV ---

    public static void main(String[] args) {
        // Tamanhos menores para permitir testar os quatro
        // algoritmos, incluindo Bubble Sort.
        int[] tamanhos = {100, 200, 500, 1000};
        int[] threadList = {1, 2, 4, 8};
        String[] algoritmos = {
            "MergeSort", "QuickSort",
            "InsertionSort", "BubbleSort"
        };
        String[] tiposEntrada = {
            "Aleatorio", "Ordenado", "Inverso"
        };

        int amostras = 5;

        try (FileWriter writer =
                new FileWriter("resultados_desempenho.csv")) {

            // Separador ; e decimal com vírgula para Excel pt-BR.
            writer.write(
                "Algoritmo;Tipo;Entrada;Tamanho;Threads;" +
                "Amostra;Tempo_ms\n"
            );

            for (int tamanho : tamanhos) {
                for (String tipoEntrada : tiposEntrada) {
                    for (int amostra = 1;
                        amostra <= amostras; amostra++) {

                        // Todos os algoritmos recebem os mesmos
                        // dados para esta amostra.
                        int[] original = gerarEntrada(
                            tamanho,
                            tipoEntrada,
                            42L + tamanho * 31L + amostra
                        );

                        // Referência independente para validar
                        // o resultado, fora do tempo medido.
                        int[] esperado = original.clone();
                        Arrays.sort(esperado);

                        for (String algoritmo : algoritmos) {
                            for (int threads : threadList) {
                                System.out.printf(
                                    "Executando: %s | %s | N=%d | Threads=%d | Amostra=%d%n",
                                    algoritmo, tipoEntrada, tamanho, threads, amostra
                                );
                                int[] arr = original.clone();

                                long inicio = System.nanoTime();

                                executarOrdenacao(
                                    algoritmo, arr, threads
                                );

                                long fim = System.nanoTime();

                                // A validação não entra no tempo.
                                if (!estaOrdenado(arr)
                                        || !Arrays.equals(arr, esperado)) {
                                    throw new IllegalStateException(
                                        "Resultado incorreto: " +
                                        algoritmo + ", entrada=" +
                                        tipoEntrada + ", tamanho=" +
                                        tamanho + ", threads=" +
                                        threads + ", amostra=" +
                                        amostra
                                    );
                                }

                                double tempoMs =
                                    (fim - inicio) / 1_000_000.0;

                                String linha = String.format(
                                    Locale.forLanguageTag("pt-BR"),
                                    "%s;%s;%s;%d;%d;%d;%.4f%n",
                                    algoritmo,
                                    threads == 1
                                        ? "Serial" : "Paralelo",
                                    tipoEntrada,
                                    tamanho,
                                    threads,
                                    amostra,
                                    tempoMs
                                );

                                writer.write(linha);
                            }
                        }
                    }
                }
            }

            System.out.println(
                "Testes concluídos! CSV gerado e resultados validados."
            );

        } catch (IOException e) {
            System.err.println(
                "Erro ao escrever o CSV: " + e.getMessage()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Execução interrompida.");
        } catch (IllegalStateException e) {
            System.err.println(
                "Falha na validação: " + e.getMessage()
            );
        }
    }
}