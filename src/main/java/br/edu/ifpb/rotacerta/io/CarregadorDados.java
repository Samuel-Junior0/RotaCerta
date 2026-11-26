package br.edu.ifpb.rotacerta.io;

import br.edu.ifpb.rotacerta.graph.Aresta;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.Vertice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CarregadorDados {

    public static final String ARQUIVO_CIDADES = "cidades.csv";
    public static final String ARQUIVO_ESTRADAS = "estradas.csv";

    private static final List<String> COLUNAS_CIDADES = List.of("nome", "latitude", "longitude");
    private static final List<String> COLUNAS_ESTRADAS =
            List.of("origem", "destino", "distancia_km", "tempo_min");

    public Grafo carregarPasta(Path pasta) throws IOException {
        if (pasta == null) {
            throw new IllegalArgumentException("A pasta de dados não pode ser nula.");
        }
        return carregar(pasta.resolve(ARQUIVO_CIDADES), pasta.resolve(ARQUIVO_ESTRADAS));
    }

    public Grafo carregar(Path arquivoCidades, Path arquivoEstradas) throws IOException {
        List<Linha> cidades = lerLinhas(arquivoCidades);
        List<Linha> estradas = lerLinhas(arquivoEstradas);

        if (cidades.isEmpty()) {
            throw new DadosInvalidosException("Arquivo de cidades vazio: " + arquivoCidades);
        }
        if (estradas.isEmpty()) {
            throw new DadosInvalidosException("Arquivo de estradas vazio: " + arquivoEstradas);
        }

        Grafo grafo = new Grafo();
        carregarCidades(grafo, cidades, arquivoCidades);

        if (grafo.numeroVertices() == 0) {
            throw new DadosInvalidosException("Nenhuma cidade encontrada em " + arquivoCidades);
        }

        carregarEstradas(grafo, estradas, arquivoEstradas);
        return grafo;
    }

    private void carregarCidades(Grafo grafo, List<Linha> linhas, Path arquivo) {
        Map<String, Integer> colunas = mapearColunas(linhas.get(0), arquivo, COLUNAS_CIDADES);

        for (int i = 1; i < linhas.size(); i++) {
            Linha linha = linhas.get(i);
            String[] partes = linha.texto().split(",", -1);

            String nome = valor(partes, colunas.get("nome"), arquivo, linha.numero(), "nome");
            double latitude = numero(partes, colunas.get("latitude"), arquivo, linha.numero(), "latitude");
            double longitude = numero(partes, colunas.get("longitude"), arquivo, linha.numero(), "longitude");

            try {
                grafo.adicionarVertice(new Vertice(nome, latitude, longitude));
            } catch (IllegalArgumentException e) {
                throw new DadosInvalidosException(
                        erroLocal(arquivo, linha.numero(), e.getMessage()), e);
            }
        }
    }

    private void carregarEstradas(Grafo grafo, List<Linha> linhas, Path arquivo) {
        Map<String, Integer> colunas = mapearColunas(linhas.get(0), arquivo, COLUNAS_ESTRADAS);

        for (int i = 1; i < linhas.size(); i++) {
            Linha linha = linhas.get(i);
            String[] partes = linha.texto().split(",", -1);

            String nomeOrigem = valor(partes, colunas.get("origem"), arquivo, linha.numero(), "origem");
            String nomeDestino = valor(partes, colunas.get("destino"), arquivo, linha.numero(), "destino");
            double distancia = numero(partes, colunas.get("distancia_km"), arquivo,
                    linha.numero(), "distancia_km");
            double tempo = numero(partes, colunas.get("tempo_min"), arquivo,
                    linha.numero(), "tempo_min");

            Vertice origem = grafo.vertice(nomeOrigem);
            Vertice destino = grafo.vertice(nomeDestino);

            if (origem == null) {
                throw new DadosInvalidosException(
                        erroLocal(arquivo, linha.numero(),
                                "cidade de origem inexistente: " + nomeOrigem));
            }
            if (destino == null) {
                throw new DadosInvalidosException(
                        erroLocal(arquivo, linha.numero(),
                                "cidade de destino inexistente: " + nomeDestino));
            }

            try {
                grafo.adicionarAresta(new Aresta(origem.nome(), destino.nome(), distancia, tempo));
            } catch (IllegalArgumentException e) {
                throw new DadosInvalidosException(
                        erroLocal(arquivo, linha.numero(), e.getMessage()), e);
            }
        }
    }

    private Map<String, Integer> mapearColunas(Linha cabecalho, Path arquivo,
                                               List<String> obrigatorias) {
        String[] partes = cabecalho.texto().split(",", -1);
        Map<String, Integer> colunas = new HashMap<>();

        for (int i = 0; i < partes.length; i++) {
            String nome = partes[i].trim().toLowerCase(Locale.ROOT);
            if (!nome.isEmpty()) {
                colunas.putIfAbsent(nome, i);
            }
        }

        for (String obrigatoria : obrigatorias) {
            if (!colunas.containsKey(obrigatoria)) {
                throw new DadosInvalidosException(
                        erroLocal(arquivo, cabecalho.numero(),
                                "coluna obrigatória ausente: " + obrigatoria));
            }
        }
        return colunas;
    }

    private static List<Linha> lerLinhas(Path arquivo) throws IOException {
        if (arquivo == null) {
            throw new IllegalArgumentException("O caminho do arquivo não pode ser nulo.");
        }

        List<String> conteudo = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        List<Linha> linhas = new ArrayList<>();

        for (int i = 0; i < conteudo.size(); i++) {
            String texto = conteudo.get(i);
            if (i == 0 && texto.startsWith("\uFEFF")) {
                texto = texto.substring(1);
            }
            texto = texto.trim();
            if (texto.isEmpty() || texto.startsWith("#")) {
                continue;
            }
            linhas.add(new Linha(i + 1, texto));
        }
        return linhas;
    }

    private static String valor(String[] partes, int indice, Path arquivo, int linha,
                                String coluna) {
        if (indice >= partes.length) {
            throw new DadosInvalidosException(
                    erroLocal(arquivo, linha, "valor ausente para a coluna " + coluna));
        }
        String valor = partes[indice].trim();
        if (valor.isEmpty()) {
            throw new DadosInvalidosException(
                    erroLocal(arquivo, linha, "valor vazio para a coluna " + coluna));
        }
        return valor;
    }

    private static double numero(String[] partes, int indice, Path arquivo, int linha,
                                 String coluna) {
        String valor = valor(partes, indice, arquivo, linha, coluna);
        try {
            return Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new DadosInvalidosException(
                    erroLocal(arquivo, linha, "número inválido em " + coluna + ": " + valor), e);
        }
    }

    private static String erroLocal(Path arquivo, int linha, String mensagem) {
        return arquivo.getFileName() + ", linha " + linha + ": " + mensagem;
    }

    private record Linha(int numero, String texto) {
    }
}
