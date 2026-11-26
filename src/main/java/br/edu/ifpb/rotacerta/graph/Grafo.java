package br.edu.ifpb.rotacerta.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Grafo {

    private final Map<String, Vertice> vertices = new LinkedHashMap<>();
    private final Map<String, List<Aresta>> adjacencias = new HashMap<>();
    private int totalArestas;

    public void adicionarVertice(Vertice vertice) {
        String chave = normalizar(vertice.nome());
        if (vertices.putIfAbsent(chave, vertice) != null) {
            throw new IllegalArgumentException("Vértice duplicado: " + vertice.nome());
        }
        adjacencias.put(chave, new ArrayList<>());
    }

    public void adicionarAresta(Aresta aresta) {
        Vertice a = vertice(aresta.origem());
        Vertice b = vertice(aresta.destino());
        if (a == null) {
            throw new IllegalArgumentException("Origem inexistente na aresta: " + aresta.origem());
        }
        if (b == null) {
            throw new IllegalArgumentException("Destino inexistente na aresta: " + aresta.destino());
        }
        if (existeAresta(a.nome(), b.nome())) {
            throw new IllegalArgumentException(
                    "Aresta duplicada entre " + a.nome() + " e " + b.nome());
        }

        Aresta canonica = new Aresta(a.nome(), b.nome(), aresta.distanciaKm(), aresta.tempoMin());
        adjacencias.get(normalizar(a.nome())).add(canonica);
        adjacencias.get(normalizar(b.nome())).add(canonica);
        totalArestas++;
    }

    public Vertice vertice(String nome) {
        return nome == null ? null : vertices.get(normalizar(nome));
    }

    public boolean contemVertice(String nome) {
        return vertice(nome) != null;
    }

    public List<Aresta> vizinhos(String nome) {
        Vertice v = vertice(nome);
        if (v == null) {
            throw new IllegalArgumentException("Vértice inexistente: " + nome);
        }
        return Collections.unmodifiableList(adjacencias.get(normalizar(v.nome())));
    }

    public Aresta arestaEntre(String origem, String destino) {
        if (!contemVertice(origem) || !contemVertice(destino)) {
            return null;
        }
        for (Aresta aresta : vizinhos(origem)) {
            if (aresta.outroExtremo(origem).equalsIgnoreCase(destino)) {
                return aresta;
            }
        }
        return null;
    }

    public Collection<Vertice> vertices() {
        return Collections.unmodifiableCollection(vertices.values());
    }

    public int numeroVertices() {
        return vertices.size();
    }

    public int numeroArestas() {
        return totalArestas;
    }

    private boolean existeAresta(String origem, String destino) {
        return vizinhos(origem).stream()
                .anyMatch(aresta -> aresta.outroExtremo(origem).equalsIgnoreCase(destino));
    }

    private static String normalizar(String nome) {
        return nome.trim().toLowerCase(Locale.ROOT);
    }
}
