
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class GraficoBenchmark extends JFrame {

    private final List<Resultado> resultados = new ArrayList<>();

    private JComboBox<String> algoritmoBox;
    private JComboBox<String> tipoBox;
    private JComboBox<String> entradaBox;
    private JComboBox<String> tamanhoBox;

    private final PainelGrafico painelGrafico = new PainelGrafico();

    public GraficoBenchmark() {
        setTitle("Análise de Desempenho - Benchmark");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        carregarCSV("resultados_desempenho.csv");

        montarInterface();
        atualizarGrafico();
    }

    private void carregarCSV(String arquivo) {
        try (BufferedReader br =
                new BufferedReader(new FileReader(arquivo))) {

            String linha;
            br.readLine(); // Ignora o cabeçalho.

            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(";");

                if (campos.length < 7) {
                    continue;
                }

                String algoritmo = campos[0];
                String tipo = campos[1];
                String entrada = campos[2];

                int tamanho = Integer.parseInt(campos[3]);
                int threads = Integer.parseInt(campos[4]);
                int amostra = Integer.parseInt(campos[5]);

                // Aceita tanto decimal com vírgula quanto ponto.
                double tempo = Double.parseDouble(
                    campos[6].trim().replace(",", ".")
                );

                resultados.add(new Resultado(
                    algoritmo, tipo, entrada,
                    tamanho, threads, amostra, tempo
                ));
            }

        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar o CSV: " + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void montarInterface() {
        JPanel filtros = new JPanel(new FlowLayout());

        algoritmoBox = new JComboBox<>(new String[]{
            "MergeSort", "QuickSort",
            "InsertionSort", "BubbleSort"
        });

        tipoBox = new JComboBox<>(new String[]{
            "Todos", "Serial", "Paralelo"
        });

        entradaBox = new JComboBox<>(new String[]{
            "Aleatorio", "Ordenado", "Inverso"
        });

        Set<Integer> tamanhos = new TreeSet<>();

        for (Resultado r : resultados) {
            tamanhos.add(r.tamanho);
        }

        tamanhoBox = new JComboBox<>();

        for (Integer tamanho : tamanhos) {
            tamanhoBox.addItem(String.valueOf(tamanho));
        }

        filtros.add(new JLabel("Algoritmo:"));
        filtros.add(algoritmoBox);

        filtros.add(new JLabel("Execução:"));
        filtros.add(tipoBox);

        filtros.add(new JLabel("Entrada:"));
        filtros.add(entradaBox);

        filtros.add(new JLabel("Tamanho:"));
        filtros.add(tamanhoBox);

        JButton atualizar = new JButton("Atualizar gráfico");
        atualizar.addActionListener(e -> atualizarGrafico());

        filtros.add(atualizar);

        algoritmoBox.addActionListener(e -> atualizarGrafico());
        tipoBox.addActionListener(e -> atualizarGrafico());
        entradaBox.addActionListener(e -> atualizarGrafico());
        tamanhoBox.addActionListener(e -> atualizarGrafico());

        add(filtros, BorderLayout.NORTH);
        add(painelGrafico, BorderLayout.CENTER);
    }

    private void atualizarGrafico() {
        if (algoritmoBox == null || tamanhoBox.getSelectedItem() == null) {
            return;
        }

        String algoritmo = (String) algoritmoBox.getSelectedItem();
        String tipo = (String) tipoBox.getSelectedItem();
        String entrada = (String) entradaBox.getSelectedItem();

        int tamanho = Integer.parseInt(
            (String) tamanhoBox.getSelectedItem()
        );

        Map<Integer, List<Double>> tempos = new TreeMap<>();

        for (Resultado r : resultados) {
            if (r.algoritmo.equals(algoritmo)
                    && (tipo.equals("Todos") || r.tipo.equals(tipo))
                    && r.entrada.equals(entrada)
                    && r.tamanho == tamanho) {

                tempos.computeIfAbsent(
                    r.threads, k -> new ArrayList<>()
                ).add(r.tempo);
            }
        }

        Map<Integer, Double> medias = new TreeMap<>();

        for (Map.Entry<Integer, List<Double>> item
                : tempos.entrySet()) {

            double media = item.getValue()
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

            medias.put(item.getKey(), media);
        }

        painelGrafico.setDados(
            medias,
            algoritmo,
            tipo,
            entrada,
            tamanho
        );
    }

    private static class Resultado {
        String algoritmo;
        String tipo;
        String entrada;

        int tamanho;
        int threads;
        int amostra;

        double tempo;

        Resultado(
                String algoritmo,
                String tipo,
                String entrada,
                int tamanho,
                int threads,
                int amostra,
                double tempo) {

            this.algoritmo = algoritmo;
            this.tipo = tipo;
            this.entrada = entrada;
            this.tamanho = tamanho;
            this.threads = threads;
            this.amostra = amostra;
            this.tempo = tempo;
        }
    }

    private static class PainelGrafico extends JPanel {

        private Map<Integer, Double> dados = new TreeMap<>();

        private String algoritmo = "";
        private String tipo = "";
        private String entrada = "";
        private int tamanho;

        public void setDados(
                Map<Integer, Double> dados,
                String algoritmo,
                String tipo,
                String entrada,
                int tamanho) {

            this.dados = new TreeMap<>(dados);
            this.algoritmo = algoritmo;
            this.tipo = tipo;
            this.entrada = entrada;
            this.tamanho = tamanho;

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int largura = getWidth();
            int altura = getHeight();

            int margemEsquerda = 90;
            int margemDireita = 40;
            int margemSuperior = 80;
            int margemInferior = 80;

            g2.setColor(Color.BLACK);

            g2.drawString(
                "Desempenho: " + algoritmo
                    + " | " + tipo
                    + " | " + entrada
                    + " | N = " + tamanho,
                margemEsquerda,
                30
            );

            if (dados.isEmpty()) {
                g2.drawString(
                    "Nenhum resultado encontrado.",
                    largura / 2 - 100,
                    altura / 2
                );
                return;
            }

            double maxTempo = dados.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1);

            if (maxTempo <= 0) {
                maxTempo = 1;
            }

            int xInicio = margemEsquerda;
            int xFim = largura - margemDireita;
            int yInicio = altura - margemInferior;
            int yFim = margemSuperior;

            // Eixos.
            g2.drawLine(xInicio, yInicio, xFim, yInicio);
            g2.drawLine(xInicio, yInicio, xInicio, yFim);

            g2.drawString(
                "Threads",
                largura / 2,
                altura - 25
            );

            g2.drawString(
                "Tempo (ms)",
                15,
                55
            );

            int quantidade = dados.size();
            int indice = 0;

            int xAnterior = 0;
            int yAnterior = 0;

            for (Map.Entry<Integer, Double> item
                    : dados.entrySet()) {

                int x = xInicio;

                if (quantidade > 1) {
                    x += indice * (xFim - xInicio)
                        / (quantidade - 1);
                }

                int y = yInicio - (int) (
                    (item.getValue() / maxTempo)
                    * (yInicio - yFim)
                );

                g2.setColor(Color.BLUE);
                g2.fillOval(x - 5, y - 5, 10, 10);

                g2.drawString(
                    String.format(
                        Locale.US, "%.4f ms", item.getValue()
                    ),
                    x - 30,
                    y - 12
                );

                g2.drawString(
                    String.valueOf(item.getKey()),
                    x - 5,
                    yInicio + 20
                );

                if (indice > 0) {
                    g2.drawLine(
                        xAnterior,
                        yAnterior,
                        x,
                        y
                    );
                }

                xAnterior = x;
                yAnterior = y;

                indice++;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GraficoBenchmark().setVisible(true);
        });
    }
}
