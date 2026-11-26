package br.edu.ifpb.rotacerta.algorithm;

import br.edu.ifpb.rotacerta.graph.TipoPeso;

import java.util.List;

public record ResultadoRota(
        List<String> caminho,
        double distanciaTotalKm,
        double tempoTotalMin,
        double custo,
        TipoPeso criterio,
        int verticesExplorados,
        long tempoExecucaoNanos
) {

    public ResultadoRota {
        caminho = List.copyOf(caminho);
    }

    public boolean encontrouRota() {
        return !caminho.isEmpty();
    }

    public double tempoExecucaoMillis() {
        return tempoExecucaoNanos / 1_000_000.0;
    }
}
