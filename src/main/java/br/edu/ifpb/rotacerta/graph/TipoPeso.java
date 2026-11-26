package br.edu.ifpb.rotacerta.graph;

import java.util.Locale;

public enum TipoPeso {
    DISTANCIA("menor distância", "km"),
    TEMPO("menor tempo", "min");

    private final String descricao;
    private final String unidade;

    TipoPeso(String descricao, String unidade) {
        this.descricao = descricao;
        this.unidade = unidade;
    }

    public String descricao() {
        return descricao;
    }

    public String unidade() {
        return unidade;
    }

    public static TipoPeso deTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O critério de peso não pode ser vazio.");
        }
        return switch (texto.trim().toLowerCase(Locale.ROOT)) {
            case "distancia", "distância", "km", "d" -> DISTANCIA;
            case "tempo", "min", "minuto", "minutos", "t" -> TEMPO;
            default -> throw new IllegalArgumentException(
                    "Critério inválido: '" + texto + "'. Use 'distancia' ou 'tempo'.");
        };
    }
}
