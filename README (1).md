# Benchmark de Algoritmos de Ordenação: Serial vs. Paralelo em Java

Projeto da disciplina de computação concorrente e paralela (UNIFOR). Implementa quatro algoritmos de ordenação em versão **serial** e **paralela**, mede o tempo de execução variando o tamanho e a natureza dos dados e o número de threads, grava os resultados em **CSV** e exibe um **gráfico interativo** (Java Swing).

## Algoritmos

| Algoritmo | Versão serial | Versão paralela |
| --- | --- | --- |
| Merge Sort | Recursivo clássico | Fork/Join (`ForkJoinPool` + `RecursiveAction`); tarefas com menos de 1.000 elementos rodam em série |
| Quick Sort | Recursivo, pivô central | Fork/Join (`ForkJoinPool` + `RecursiveAction`); tarefas com menos de 1.000 elementos rodam em série |
| Insertion Sort | Clássico | O vetor é dividido em blocos, cada thread ordena um bloco e os blocos são intercalados no final |
| Bubble Sort | Clássico, com parada antecipada | *Odd-even transposition*: a cada fase, as threads comparam pares vizinhos distintos; threads novas a cada fase |

## Arquivos

| Arquivo | Função |
| --- | --- |
| `BenchmarkOrdenacao.java` | Algoritmos, geração dos dados, execução dos testes e gravação do CSV |
| `GraficoBenchmark.java` | Janela Swing que lê o CSV e desenha o gráfico com filtros |
| `resultados_desempenho.csv` | Resultados da execução (960 medições) |

## Requisitos

- JDK 19 ou superior (o código usa `try-with-resources` com `ForkJoinPool`, disponível a partir do Java 19)
- Nenhuma biblioteca externa

## Como executar

Abra um terminal na pasta do projeto.

```bash
# 1. Compilar
javac BenchmarkOrdenacao.java GraficoBenchmark.java

# 2. Rodar o benchmark (gera resultados_desempenho.csv na pasta atual)
java BenchmarkOrdenacao

# 3. Abrir o gráfico (lê resultados_desempenho.csv da pasta atual)
java GraficoBenchmark
```

Também é possível executar as duas classes direto pelo IntelliJ IDEA. O `GraficoBenchmark` precisa ser executado na mesma pasta do CSV.

## Parâmetros do teste

| Parâmetro | Valores |
| --- | --- |
| Tamanhos do vetor | 100, 200, 500, 1.000 |
| Tipos de entrada | Aleatório, Ordenado, Inverso |
| Threads | 1 (serial), 2, 4, 8 (paralelo) |
| Amostras | 5 por combinação |
| Total de medições | 4 algoritmos × 4 tamanhos × 3 entradas × 4 configurações de threads × 5 amostras = 960 |

- Todos os algoritmos recebem a mesma entrada em cada amostra (mesma semente).
- O tempo é medido com `System.nanoTime()` apenas em torno da ordenação.
- Cada resultado é validado contra `Arrays.sort`; se algum estiver incorreto, a execução é interrompida.

## Formato do CSV

Separador `;` e decimal com vírgula (abre direto no Excel em português):

```
Algoritmo;Tipo;Entrada;Tamanho;Threads;Amostra;Tempo_ms
MergeSort;Serial;Aleatorio;100;1;1;0,0896
```

## Gráfico interativo

A janela `GraficoBenchmark` mostra o **tempo médio (ms) por número de threads**, calculado sobre as 5 amostras. Os filtros disponíveis são:

- **Algoritmo:** Merge Sort, Quick Sort, Insertion Sort ou Bubble Sort
- **Execução:** Todos, Serial ou Paralelo
- **Entrada:** Aleatório, Ordenado ou Inverso
- **Tamanho:** um dos tamanhos presentes no CSV

O gráfico se atualiza ao mudar qualquer filtro.

## Observações

- Com vetores de até 1.000 elementos, as versões paralelas de Merge Sort e Quick Sort não chegam a dividir o trabalho em tarefas (o limite é de 1.000 elementos por tarefa), e o custo de criar threads pesa mais que o tempo de ordenação. Por isso, nesses tamanhos o paralelo tende a ficar mais lento que o serial. Para ver ganhos reais, use vetores maiores e ajuste a lista `tamanhos` em `BenchmarkOrdenacao.java`.
- Os tempos dependem da máquina, do número de núcleos e do estado da JVM no momento do teste.

## Autores

- Allan Justo de Freitas Guimarães (matrícula 2413100)
