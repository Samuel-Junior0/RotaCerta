package br.edu.ifpb.rotacerta.graph;

public record Vertice(String nome, double latitude, double longitude) {

    public Vertice {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do vértice não pode ser vazio.");
        }
        nome = nome.trim();
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude inválida para " + nome + ": " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude inválida para " + nome + ": " + longitude);
        }
    }
}
