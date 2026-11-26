package br.edu.ifpb.rotacerta.algorithm;

import br.edu.ifpb.rotacerta.graph.Aresta;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.TipoPeso;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public final class Dijkstra {

    private static final double INFINITO = Double.POSITIVE_INFINITY;
    private static final double EPSILON = 1e-9;

    private final Grafo grafo;

    public Dijkstra(Grafo grafo) {
        this.grafo = Objects.requireNonNull(grafo, "O grafo não pode ser nulo.");
    }

    public ResultadoRota calcular(String origem, String destino, TipoPeso criterio) {
        if (origem == null || origem.isBlank()) {
            throw new IllegalArgumentException("A cidade de origem é obrigatória.");
        }
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("A cidade de destino é obrigatória.");
        }
        Objects.requireNonNull(criterio, "O critério de peso não pode ser nulo.");
        if (!grafo.contemVertice(origem)) {
            throw new IllegalArgumentException("Cidade de origem não encontrada: " + origem);
        }
        if (!grafo.contemVertice(destino)) {
            throw new IllegalArgumentException("Cidade de destino não encontrada: " + destino);
        }

        String nomeOrigem = grafo.vertice(origem).nome();
        String nomeDestino = grafo.vertice(destino).nome();
        long inicio = System.nanoTime();

        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> anteriores = new HashMap<>();
        Set<String> fechados = new HashSet<>();
        PriorityQueue<EntradaFila> fila =
                new PriorityQueue<>(Comparator.comparingDouble(EntradaFila::distancia));

        distancias.put(nomeOrigem, 0.0);
        fila.add(new EntradaFila(nomeOrigem, 0.0));

        int explorados = 0;
        boolean encontrou = nomeOrigem.equals(nomeDestino);

        if (encontrou) {
            explorados = 1;
        } else {
            while (!fila.isEmpty()) {
                EntradaFila atual = fila.poll();

                double melhorConhecida = distancias.getOrDefault(atual.vertice(), INFINITO);
                if (atual.distancia() > melhorConhecida + EPSILON) {
                    continue;
                }

                if (!fechados.add(atual.vertice())) {
                    continue;
                }

                explorados++;

                if (atual.vertice().equals(nomeDestino)) {
                    encontrou = true;
                    break;
                }

                for (Aresta aresta : grafo.vizinhos(atual.vertice())) {
                    String vizinho = aresta.outroExtremo(atual.vertice());
                    if (fechados.contains(vizinho)) {
                        continue;
                    }
                    double novaDistancia = melhorConhecida + aresta.peso(criterio);
                    double distanciaAtual = distancias.getOrDefault(vizinho, INFINITO);
                    if (novaDistancia + EPSILON < distanciaAtual) {
                        distancias.put(vizinho, novaDistancia);
                        anteriores.put(vizinho, atual.vertice());
                        fila.add(new EntradaFila(vizinho, novaDistancia));
                    }
                }
            }
        }

        long fim = System.nanoTime();

        if (!encontrou) {
            return new ResultadoRota(List.of(), INFINITO, INFINITO, INFINITO,
                    criterio, explorados, fim - inicio);
        }

        List<String> caminho = reconstruirCaminho(nomeOrigem, nomeDestino, anteriores);
        double distanciaTotal = somarMetrica(caminho, TipoPeso.DISTANCIA);
        double tempoTotal = somarMetrica(caminho, TipoPeso.TEMPO);
        double custo = criterio == TipoPeso.DISTANCIA ? distanciaTotal : tempoTotal;

        return new ResultadoRota(caminho, distanciaTotal, tempoTotal, custo,
                criterio, explorados, fim - inicio);
    }

    private List<String> reconstruirCaminho(String origem, String destino,
                                            Map<String, String> anteriores) {
        LinkedList<String> caminho = new LinkedList<>();
        String atual = destino;
        while (atual != null) {
            caminho.addFirst(atual);
            if (atual.equals(origem)) {
                break;
            }
            atual = anteriores.get(atual);
        }
        return caminho;
    }

    private double somarMetrica(List<String> caminho, TipoPeso tipo) {
        double total = 0.0;
        for (int i = 0; i + 1 < caminho.size(); i++) {
            Aresta aresta = grafo.arestaEntre(caminho.get(i), caminho.get(i + 1));
            if (aresta == null) {
                throw new IllegalStateException(
                        "Caminho inconsistente: aresta ausente entre "
                                + caminho.get(i) + " e " + caminho.get(i + 1));
            }
            total += aresta.peso(tipo);
        }
        return total;
    }

    private record EntradaFila(String vertice, double distancia) {
    }
}
