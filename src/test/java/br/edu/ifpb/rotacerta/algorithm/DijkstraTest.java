package br.edu.ifpb.rotacerta.algorithm;

import br.edu.ifpb.rotacerta.graph.Aresta;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.TipoPeso;
import br.edu.ifpb.rotacerta.graph.Vertice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DijkstraTest {

    private Grafo grafoDeExemplo() {
        Grafo grafo = new Grafo();
        grafo.adicionarVertice(new Vertice("A", -7.0, -35.0));
        grafo.adicionarVertice(new Vertice("B", -7.1, -35.1));
        grafo.adicionarVertice(new Vertice("C", -7.2, -35.2));
        grafo.adicionarVertice(new Vertice("D", -7.3, -35.3));
        grafo.adicionarVertice(new Vertice("E", -7.4, -35.4));

        grafo.adicionarAresta(new Aresta("A", "B", 10, 5));
        grafo.adicionarAresta(new Aresta("B", "D", 10, 5));
        grafo.adicionarAresta(new Aresta("A", "C", 15, 2));
        grafo.adicionarAresta(new Aresta("C", "D", 15, 2));
        return grafo;
    }

    @Test
    void encontraCaminhoMinimoPorDistancia() {
        ResultadoRota resultado = new Dijkstra(grafoDeExemplo())
                .calcular("A", "D", TipoPeso.DISTANCIA);

        assertTrue(resultado.encontrouRota());
        assertEquals(List.of("A", "B", "D"), resultado.caminho());
        assertEquals(20.0, resultado.distanciaTotalKm(), 1e-9);
        assertEquals(20.0, resultado.custo(), 1e-9);
        assertTrue(resultado.verticesExplorados() >= 2);
    }

    @Test
    void criterioDeTempoPodeEscolherOutroCaminho() {
        ResultadoRota resultado = new Dijkstra(grafoDeExemplo())
                .calcular("A", "D", TipoPeso.TEMPO);

        assertTrue(resultado.encontrouRota());
        assertEquals(List.of("A", "C", "D"), resultado.caminho());
        assertEquals(4.0, resultado.tempoTotalMin(), 1e-9);
        assertEquals(4.0, resultado.custo(), 1e-9);
    }

    @Test
    void origemIgualAoDestinoTemCustoZero() {
        ResultadoRota resultado = new Dijkstra(grafoDeExemplo())
                .calcular("B", "B", TipoPeso.DISTANCIA);

        assertTrue(resultado.encontrouRota());
        assertEquals(List.of("B"), resultado.caminho());
        assertEquals(0.0, resultado.custo(), 1e-9);
        assertEquals(1, resultado.verticesExplorados());
    }

    @Test
    void destinoInalcancavelRetornaCaminhoVazio() {
        ResultadoRota resultado = new Dijkstra(grafoDeExemplo())
                .calcular("A", "E", TipoPeso.DISTANCIA);

        assertFalse(resultado.encontrouRota());
        assertTrue(resultado.caminho().isEmpty());
        assertEquals(Double.POSITIVE_INFINITY, resultado.custo());
    }

    @Test
    void aceitaNomesSemDiferenciarMaiusculas() {
        ResultadoRota resultado = new Dijkstra(grafoDeExemplo())
                .calcular("a", "d", TipoPeso.DISTANCIA);

        assertTrue(resultado.encontrouRota());
        assertEquals(List.of("A", "B", "D"), resultado.caminho());
    }
}
