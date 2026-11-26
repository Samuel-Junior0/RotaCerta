package br.edu.ifpb.rotacerta.cli;

import br.edu.ifpb.rotacerta.algorithm.Dijkstra;
import br.edu.ifpb.rotacerta.algorithm.ResultadoRota;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.TipoPeso;
import br.edu.ifpb.rotacerta.graph.Vertice;
import br.edu.ifpb.rotacerta.io.CarregadorDados;
import br.edu.ifpb.rotacerta.io.DadosInvalidosException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Benchmark {

    public static final int AQUECIMENTO_PADRAO = 30;
    public static final int REPETICOES_PADRAO = 30;
    public static final List<String> PASTAS_PADRAO = List.of(
            "data/pequeno", "data/medio", "data/grande");

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    public int executar(List<Path> pastas, String origem, String destino,
                        int aquecimento, int repeticoes, Path saida) {
        List<ResultadoInstancia> resultados = new ArrayList<>();

        for (Path pasta : pastas) {
            ResultadoInstancia resultado;
            try {
                resultado = medirInstancia(pasta, origem, destino,
                        aquecimento, repeticoes);
            } catch (IOException e) {
                System.err.println("Erro ao ler os dados de " + pasta + ": " + e.getMessage());
                return 1;
            } catch (DadosInvalidosException e) {
                System.err.println("Dados inválidos em " + pasta + ": " + e.getMessage());
                return 1;
            } catch (IllegalArgumentException e) {
                System.err.println("Erro em " + pasta + ": " + e.getMessage());
                return 1;
            }
            resultados.add(resultado);
        }

        imprimirConsole(resultados, aquecimento, repeticoes);

        if (saida != null) {
            try {
                escreverArquivo(saida, resultados, aquecimento, repeticoes);
                System.out.println();
                System.out.println("Resultados gravados em " + saida);
            } catch (IOException e) {
                System.err.println("Erro ao gravar " + saida + ": " + e.getMessage());
                return 1;
            }
        }
        return 0;
    }

    private ResultadoInstancia medirInstancia(Path pasta, String origem, String destino,
                                              int aquecimento, int repeticoes)
            throws IOException {
        descartarReferencias();
        long memoriaAntes = memoriaUsadaBytes();

        Grafo grafo = new CarregadorDados().carregarPasta(pasta);
        long memoriaDepois = memoriaUsadaBytes();
        double memoriaMb = Math.max(0.0, (memoriaDepois - memoriaAntes) / 1_000_000.0);

        String origemEfetiva = origem != null ? origem : primeiraCidade(grafo);
        String destinoEfetivo = destino != null ? destino : ultimaCidade(grafo);

        if (!grafo.contemVertice(origemEfetiva)) {
            throw new IllegalArgumentException("cidade de origem não encontrada: " + origemEfetiva);
        }
        if (!grafo.contemVertice(destinoEfetivo)) {
            throw new IllegalArgumentException("cidade de destino não encontrada: " + destinoEfetivo);
        }

        Dijkstra dijkstra = new Dijkstra(grafo);

        for (int i = 0; i < aquecimento; i++) {
            dijkstra.calcular(origemEfetiva, destinoEfetivo, TipoPeso.DISTANCIA);
            dijkstra.calcular(origemEfetiva, destinoEfetivo, TipoPeso.TEMPO);
        }

        List<Medida> medidas = new ArrayList<>();
        for (TipoPeso criterio : TipoPeso.values()) {
            medidas.add(medirCriterio(dijkstra, origemEfetiva, destinoEfetivo,
                    criterio, repeticoes));
        }

        return new ResultadoInstancia(pasta, grafo.numeroVertices(), grafo.numeroArestas(),
                memoriaMb, origemEfetiva, destinoEfetivo, List.copyOf(medidas));
    }

    private Medida medirCriterio(Dijkstra dijkstra, String origem, String destino,
                                 TipoPeso criterio, int repeticoes) {
        double[] tempos = new double[repeticoes];
        int explorados = 0;
        int tamanhoCaminho = 0;
        double custo = 0.0;

        for (int i = 0; i < repeticoes; i++) {
            ResultadoRota resultado = dijkstra.calcular(origem, destino, criterio);
            tempos[i] = resultado.tempoExecucaoMillis();
            explorados = resultado.verticesExplorados();
            tamanhoCaminho = resultado.caminho().size();
            custo = resultado.custo();
        }

        double media = 0.0;
        double minimo = tempos[0];
        double maximo = tempos[0];
        for (double tempo : tempos) {
            media += tempo;
            minimo = Math.min(minimo, tempo);
            maximo = Math.max(maximo, tempo);
        }
        media /= tempos.length;

        double somaQuadrados = 0.0;
        for (double tempo : tempos) {
            double diferenca = tempo - media;
            somaQuadrados += diferenca * diferenca;
        }
        double desvio = tempos.length > 1
                ? Math.sqrt(somaQuadrados / tempos.length)
                : 0.0;

        return new Medida(criterio, tamanhoCaminho, custo, explorados,
                media, desvio, minimo, maximo);
    }

    private void imprimirConsole(List<ResultadoInstancia> resultados,
                                 int aquecimento, int repeticoes) {
        System.out.println();
        System.out.println("RotaCerta — benchmark do Dijkstra");
        System.out.printf(BR, "Aquecimento: %d execuções por critério | Repetições: %d%n",
                aquecimento, repeticoes);
        System.out.println("Tempos medidos internamente por Dijkstra.calcular (ms).");

        for (ResultadoInstancia instancia : resultados) {
            System.out.println();
            System.out.printf(BR, "Instância: %s (%d %s, %d %s)%n",
                    instancia.pasta(),
                    instancia.vertices(), plural(instancia.vertices(), "cidade", "cidades"),
                    instancia.arestas(), plural(instancia.arestas(), "estrada", "estradas"));
            System.out.printf(BR, "Consulta: %s -> %s%n",
                    instancia.origem(), instancia.destino());
            System.out.printf(BR, "Memória adicional de heap: %.2f MB (estimativa)%n",
                    instancia.memoriaMb());
            System.out.println();
            System.out.printf(BR, "%-12s %8s %14s %11s %11s %11s %11s%n",
                    "Critério", "Rota", "Custo", "Explorados",
                    "Média (ms)", "Desvio", "Mín / Máx");
            for (Medida medida : instancia.medidas()) {
                System.out.printf(BR, "%-12s %8d %14s %11d %11.4f %11.4f %6.4f/%.4f%n",
                        medida.criterio().name().toLowerCase(Locale.ROOT),
                        medida.tamanhoCaminho(),
                        String.format(BR, "%.1f %s", medida.custo(),
                                medida.criterio().unidade()),
                        medida.explorados(),
                        medida.mediaMs(), medida.desvioMs(),
                        medida.minimoMs(), medida.maximoMs());
            }
        }

        System.out.println();
        System.out.println("Resumo (Markdown):");
        System.out.println(tabelaMarkdown(resultados, repeticoes));
    }

    private void escreverArquivo(Path saida, List<ResultadoInstancia> resultados,
                                 int aquecimento, int repeticoes) throws IOException {
        Path pai = saida.toAbsolutePath().getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }

        String nome = saida.getFileName().toString().toLowerCase(Locale.ROOT);
        String conteudo = nome.endsWith(".md") || nome.endsWith(".markdown")
                ? tabelaMarkdown(resultados, repeticoes)
                : tabelaCsv(resultados, aquecimento, repeticoes);

        Files.writeString(saida, conteudo, StandardCharsets.UTF_8);
    }

    private String tabelaMarkdown(List<ResultadoInstancia> resultados, int repeticoes) {
        StringBuilder texto = new StringBuilder();
        texto.append("| Instância | Vértices | Arestas | Critério | Consultas ")
                .append("| Tempo médio (ms) | Desvio (ms) | Mín (ms) | Máx (ms) ")
                .append("| Explorados | Memória (MB) |\n");
        texto.append("|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|\n");

        for (ResultadoInstancia instancia : resultados) {
            for (Medida medida : instancia.medidas()) {
                texto.append(String.format(BR,
                        "| %s | %d | %d | %s | %d | %.4f | %.4f | %.4f | %.4f | %d | %.2f |%n",
                        nomeInstancia(instancia.pasta()),
                        instancia.vertices(), instancia.arestas(),
                        medida.criterio().name().toLowerCase(Locale.ROOT),
                        repeticoes, medida.mediaMs(), medida.desvioMs(),
                        medida.minimoMs(), medida.maximoMs(),
                        medida.explorados(), instancia.memoriaMb()));
            }
        }
        return texto.toString();
    }

    private String tabelaCsv(List<ResultadoInstancia> resultados,
                             int aquecimento, int repeticoes) {
        StringBuilder texto = new StringBuilder();
        texto.append("instancia,vertices,arestas,criterio,origem,destino,")
                .append("repeticoes,aquecimento,tempo_medio_ms,desvio_padrao_ms,")
                .append("tempo_minimo_ms,tempo_maximo_ms,vertices_explorados,memoria_mb\n");

        for (ResultadoInstancia instancia : resultados) {
            for (Medida medida : instancia.medidas()) {
                texto.append(String.format(Locale.ROOT,
                        "%s,%d,%d,%s,%s,%s,%d,%d,%.6f,%.6f,%.6f,%.6f,%d,%.2f%n",
                        nomeInstancia(instancia.pasta()),
                        instancia.vertices(), instancia.arestas(),
                        medida.criterio().name().toLowerCase(Locale.ROOT),
                        instancia.origem(), instancia.destino(),
                        repeticoes, aquecimento,
                        medida.mediaMs(), medida.desvioMs(),
                        medida.minimoMs(), medida.maximoMs(),
                        medida.explorados(), instancia.memoriaMb()));
            }
        }
        return texto.toString();
    }

    private static String nomeInstancia(Path pasta) {
        Path nome = pasta.normalize().getFileName();
        return nome == null ? pasta.toString() : nome.toString();
    }

    private static String plural(int quantidade, String singular, String plural) {
        return quantidade == 1 ? singular : plural;
    }

    private static String primeiraCidade(Grafo grafo) {
        return grafo.vertices().iterator().next().nome();
    }

    private static String ultimaCidade(Grafo grafo) {
        String ultima = null;
        for (Vertice vertice : grafo.vertices()) {
            ultima = vertice.nome();
        }
        if (ultima == null) {
            throw new IllegalStateException("grafo sem vértices");
        }
        return ultima;
    }

    private static void descartarReferencias() {
        System.gc();
    }

    private static long memoriaUsadaBytes() {
        Runtime runtime = Runtime.getRuntime();
        long menor = Long.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            System.gc();
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            menor = Math.min(menor, runtime.totalMemory() - runtime.freeMemory());
        }
        return menor;
    }

    private record Medida(
            TipoPeso criterio,
            int tamanhoCaminho,
            double custo,
            int explorados,
            double mediaMs,
            double desvioMs,
            double minimoMs,
            double maximoMs
    ) {
    }

    private record ResultadoInstancia(
            Path pasta,
            int vertices,
            int arestas,
            double memoriaMb,
            String origem,
            String destino,
            List<Medida> medidas
    ) {
    }
}
