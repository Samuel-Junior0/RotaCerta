package br.edu.ifpb.rotacerta.gui;

import br.edu.ifpb.rotacerta.graph.Aresta;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.Vertice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrafoPanelTest {

    private Grafo grafoDeExemplo() {
        Grafo grafo = new Grafo();
        grafo.adicionarVertice(new Vertice("A", -7.0, -35.0));
        grafo.adicionarVertice(new Vertice("B", -7.1, -35.1));
        grafo.adicionarVertice(new Vertice("C", -7.2, -35.2));
        grafo.adicionarAresta(new Aresta("A", "B", 10, 5));
        grafo.adicionarAresta(new Aresta("B", "C", 10, 5));
        return grafo;
    }

    private GrafoPanel painelComGrafo() {
        GrafoPanel painel = new GrafoPanel();
        painel.setSize(800, 600);
        painel.setGrafo(grafoDeExemplo());
        return painel;
    }

    @Test
    void iniciaAjustadoENaoPassaDoZoomMinimo() {
        GrafoPanel painel = painelComGrafo();

        assertEquals(1.0, painel.getZoom(), 1e-9);
        painel.zoomOut();
        assertEquals(1.0, painel.getZoom(), 1e-9);
    }

    @Test
    void ampliaEVoltaAoEnquadramentoInicial() {
        GrafoPanel painel = painelComGrafo();

        painel.zoomIn();
        painel.zoomIn();
        assertTrue(painel.getZoom() > 1.5);

        painel.ajustarVisao();
        assertEquals(1.0, painel.getZoom(), 1e-9);
    }

    @Test
    void notificaMudancaDeZoom() {
        GrafoPanel painel = painelComGrafo();
        AtomicReference<Double> novoZoom = new AtomicReference<>();

        painel.addPropertyChangeListener("zoom",
                evento -> novoZoom.set((Double) evento.getNewValue()));
        painel.zoomIn();

        assertEquals(1.25, novoZoom.get(), 1e-9);
    }

    @Test
    void carregarOutroGrafoRestauraOZoom() {
        GrafoPanel painel = painelComGrafo();
        painel.zoomIn();

        painel.setGrafo(grafoDeExemplo());
        assertEquals(1.0, painel.getZoom(), 1e-9);
    }

    @Test
    void aceitaRotaNulaSemQuebrar() {
        GrafoPanel painel = painelComGrafo();
        painel.setRota(null);

        painel.setRota(List.of("A", "B", "C"));
        painel.ajustarVisao();
        assertEquals(1.0, painel.getZoom(), 1e-9);
    }
}
