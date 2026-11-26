package br.edu.ifpb.rotacerta.cli;

import br.edu.ifpb.rotacerta.algorithm.Dijkstra;
import br.edu.ifpb.rotacerta.algorithm.ResultadoRota;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.TipoPeso;
import br.edu.ifpb.rotacerta.graph.Vertice;
import br.edu.ifpb.rotacerta.io.CarregadorDados;
import br.edu.ifpb.rotacerta.io.DadosInvalidosException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CliApp {

    private static final String PASTA_PADRAO = "data/pequeno";
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    public int executar(String[] args) {
        Set<String> flags = new HashSet<>();
        Map<String, String> opcoes = new HashMap<>();
        List<String> pastasDados = new ArrayList<>();

        try {
            interpretar(args, flags, opcoes, pastasDados);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro nos argumentos: " + e.getMessage());
            System.err.println("Use --ajuda para ver as opções.");
            return 2;
        }

        if (flags.contains("ajuda") || flags.contains("help")) {
            imprimirAjuda();
            return 0;
        }

        if (flags.contains("benchmark")) {
            return executarBenchmark(opcoes, pastasDados);
        }

        if (pastasDados.size() > 1) {
            System.err.println("Erro: informe apenas uma pasta com --dados "
                    + "(fora do modo --benchmark).");
            return 2;
        }

        Path pasta = pastasDados.isEmpty()
                ? Path.of(opcoes.getOrDefault("dados", PASTA_PADRAO))
                : Path.of(pastasDados.get(0));
        Grafo grafo;

        try {
            grafo = new CarregadorDados().carregarPasta(pasta);
        } catch (NoSuchFileException e) {
            System.err.println("Arquivo não encontrado: " + e.getFile());
            return 1;
        } catch (IOException e) {
            System.err.println("Erro ao ler os dados: " + e.getMessage());
            return 1;
        } catch (DadosInvalidosException e) {
            System.err.println("Dados inválidos: " + e.getMessage());
            return 1;
        }

        if (flags.contains("listar-cidades")) {
            listarCidades(grafo);
            return 0;
        }

        String origem = opcoes.get("origem");
        String destino = opcoes.get("destino");
        if (origem == null || destino == null) {
            System.err.println("Informe --origem e --destino. Use --ajuda para ver as opções.");
            return 2;
        }

        TipoPeso criterio;
        try {
            criterio = TipoPeso.deTexto(opcoes.getOrDefault("criterio", "distancia"));
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            return 2;
        }

        ResultadoRota resultado;
        try {
            resultado = new Dijkstra(grafo).calcular(origem, destino, criterio);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            return 2;
        }

        imprimirResultado(grafo, resultado, pasta);
        return resultado.encontrouRota() ? 0 : 3;
    }

    private int executarBenchmark(Map<String, String> opcoes, List<String> pastasDados) {
        int aquecimento;
        int repeticoes;
        try {
            aquecimento = inteiro(opcoes, "aquecimento", Benchmark.AQUECIMENTO_PADRAO, 0);
            repeticoes = inteiro(opcoes, "repeticoes", Benchmark.REPETICOES_PADRAO, 1);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro nos argumentos: " + e.getMessage());
            return 2;
        }

        List<Path> pastas = new ArrayList<>();
        if (pastasDados.isEmpty()) {
            for (String candidata : Benchmark.PASTAS_PADRAO) {
                Path pasta = Path.of(candidata);
                if (Files.isDirectory(pasta)) {
                    pastas.add(pasta);
                }
            }
            if (pastas.isEmpty()) {
                System.err.println("Nenhuma pasta de dados padrão encontrada "
                        + "(data/pequeno, data/medio, data/grande).");
                return 1;
            }
        } else {
            for (String candidata : pastasDados) {
                Path pasta = Path.of(candidata);
                if (!Files.isDirectory(pasta)) {
                    System.err.println("Pasta de dados não encontrada: " + pasta);
                    return 1;
                }
                pastas.add(pasta);
            }
        }

        Path saida = opcoes.containsKey("saida") ? Path.of(opcoes.get("saida")) : null;
        return new Benchmark().executar(pastas, opcoes.get("origem"), opcoes.get("destino"),
                aquecimento, repeticoes, saida);
    }

    private void interpretar(String[] args, Set<String> flags, Map<String, String> opcoes,
                             List<String> pastasDados) {
        Set<String> opcoesComValor = Set.of("dados", "origem", "destino", "criterio",
                "repeticoes", "aquecimento", "saida");
        Set<String> flagsConhecidas = Set.of("cli", "ajuda", "help", "listar-cidades",
                "benchmark");

        for (int i = 0; i < args.length; i++) {
            String argumento = args[i];
            if (!argumento.startsWith("--")) {
                throw new IllegalArgumentException("argumento inesperado: " + argumento);
            }

            String chave = argumento.substring(2);
            if (flagsConhecidas.contains(chave)) {
                flags.add(chave);
                continue;
            }
            if (!opcoesComValor.contains(chave)) {
                throw new IllegalArgumentException("opção desconhecida: --" + chave);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("faltou o valor de --" + chave);
            }
            String valor = args[++i];
            opcoes.put(chave, valor);
            if (chave.equals("dados")) {
                pastasDados.add(valor);
            }
        }
    }

    private static int inteiro(Map<String, String> opcoes, String chave,
                               int padrao, int minimo) {
        String valor = opcoes.get(chave);
        if (valor == null) {
            return padrao;
        }
        try {
            int numero = Integer.parseInt(valor);
            if (numero < minimo) {
                throw new IllegalArgumentException(
                        "--" + chave + " deve ser maior ou igual a " + minimo);
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "--" + chave + " deve ser um número inteiro");
        }
    }

    private void listarCidades(Grafo grafo) {
        List<String> nomes = new ArrayList<>();
        for (Vertice vertice : grafo.vertices()) {
            nomes.add(vertice.nome());
        }
        Collator collator = Collator.getInstance(BR);
        nomes.sort(collator);

        System.out.println("Cidades disponíveis (" + nomes.size() + "):");
        nomes.forEach(nome -> System.out.println("  - " + nome));
    }

    private void imprimirResultado(Grafo grafo, ResultadoRota resultado, Path pasta) {
        System.out.println();
        System.out.println("RotaCerta — planejador de rotas para pipeiros");
        System.out.println("Conjunto de dados: " + pasta
                + " (" + grafo.numeroVertices() + " " + plural(grafo.numeroVertices(), "cidade", "cidades")
                + ", " + grafo.numeroArestas() + " " + plural(grafo.numeroArestas(), "estrada", "estradas")
                + ")");
        System.out.println("Critério: " + resultado.criterio().descricao()
                + " (" + resultado.criterio().unidade() + ")");

        if (!resultado.encontrouRota()) {
            System.out.println();
            System.out.println("Não existe rota entre as cidades informadas neste conjunto de dados.");
            System.out.println("Vértices explorados: " + resultado.verticesExplorados()
                    + " de " + grafo.numeroVertices());
            System.out.printf(BR, "Tempo de execução: %.3f ms%n",
                    resultado.tempoExecucaoMillis());
            return;
        }

        System.out.println();
        System.out.println("Rota encontrada (" + resultado.caminho().size() + " "
                + plural(resultado.caminho().size(), "cidade", "cidades") + "):");
        System.out.println("  " + String.join(" -> ", resultado.caminho()));
        System.out.println();
        System.out.printf(BR, "Distância total: %.1f km%n", resultado.distanciaTotalKm());
        System.out.printf(BR, "Tempo estimado: %.1f min%n", resultado.tempoTotalMin());
        System.out.printf(BR, "Custo otimizado: %.1f %s%n",
                resultado.custo(), resultado.criterio().unidade());
        System.out.println("Vértices explorados: " + resultado.verticesExplorados()
                + " de " + grafo.numeroVertices());
        System.out.printf(BR, "Tempo de execução: %.3f ms%n",
                resultado.tempoExecucaoMillis());
    }

    private static String plural(int quantidade, String singular, String plural) {
        return quantidade == 1 ? singular : plural;
    }

    private void imprimirAjuda() {
        System.out.println("""
                RotaCerta — planejador de rotas para pipeiros (Dijkstra)

                Uso:
                  java -jar target/rotacerta-1.0.0.jar --cli \\
                      --dados data/pequeno \\
                      --origem "Patos" \\
                      --destino "João Pessoa" \\
                      --criterio distancia

                Opções:
                  --dados <pasta>       pasta com cidades.csv e estradas.csv
                                        (padrão: data/pequeno; pode repetir no benchmark)
                  --origem <cidade>     cidade de origem
                  --destino <cidade>    cidade de destino
                  --criterio <tipo>     distancia (padrão) ou tempo
                  --listar-cidades      lista as cidades do conjunto de dados
                  --benchmark           mede consultas de distância e tempo
                  --repeticoes <n>      repetições medidas por critério (padrão: 30)
                  --aquecimento <n>     execuções descartadas por critério (padrão: 30)
                  --saida <arquivo>     grava o resumo em .md ou .csv
                  --ajuda               mostra esta ajuda

                Exemplo de rota com critérios diferentes:
                  --origem "Patos" --destino "João Pessoa" --criterio distancia
                  --origem "Patos" --destino "João Pessoa" --criterio tempo

                Benchmark das três instâncias (gera tabela Markdown pronta para o relatório):
                  --benchmark --repeticoes 30 --saida /tmp/benchmark.md

                Sem --dados, o benchmark usa data/pequeno, data/medio e data/grande
                que existirem. Com --dados, use uma ou mais pastas explícitas.
                Sem --origem/--destino, a consulta usa a primeira e a última cidade
                da ordem de leitura dos CSVs.

                Códigos de saída:
                  0  sucesso
                  1  erro ao ler/validar/gravar os dados
                  2  argumentos inválidos
                  3  não existe rota entre as cidades
                """);
    }
}
