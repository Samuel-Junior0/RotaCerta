package br.edu.ifpb.rotacerta.graph;

public record Aresta(String origem, String destino, double distanciaKm, double tempoMin) {

    public Aresta {
        if (origem == null || origem.isBlank() || destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("Origem e destino da aresta são obrigatórios.");
        }
        origem = origem.trim();
        destino = destino.trim();
        if (origem.equalsIgnoreCase(destino)) {
            throw new IllegalArgumentException("Aresta com origem igual ao destino: " + origem);
        }
        if (distanciaKm <= 0) {
            throw new IllegalArgumentException("Distância deve ser positiva: " + distanciaKm);
        }
        if (tempoMin <= 0) {
            throw new IllegalArgumentException("Tempo deve ser positivo: " + tempoMin);
        }
    }

    public double peso(TipoPeso tipo) {
        return switch (tipo) {
            case DISTANCIA -> distanciaKm;
            case TEMPO -> tempoMin;
        };
    }

    public String outroExtremo(String vertice) {
        if (vertice.equalsIgnoreCase(origem)) {
            return destino;
        }
        if (vertice.equalsIgnoreCase(destino)) {
            return origem;
        }
        throw new IllegalArgumentException(
                "O vértice '" + vertice + "' não pertence à aresta " + origem + " - " + destino);
    }
}
