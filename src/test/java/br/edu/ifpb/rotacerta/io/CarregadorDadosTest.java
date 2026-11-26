package br.edu.ifpb.rotacerta.io;

import br.edu.ifpb.rotacerta.graph.Grafo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarregadorDadosTest {

    @TempDir
    Path pasta;

    @Test
    void carregaArquivosValidos() throws IOException {
        Files.writeString(pasta.resolve("cidades.csv"), """
                nome,latitude,longitude
                João Pessoa,-7.1195,-34.8450
                Campina Grande,-7.2306,-35.8811
                Patos,-7.0244,-37.2800
                """);
        Files.writeString(pasta.resolve("estradas.csv"), """
                origem,destino,distancia_km,tempo_min
                João Pessoa,Campina Grande,125,100
                Campina Grande,Patos,185,170
                """);

        Grafo grafo = new CarregadorDados().carregarPasta(pasta);

        assertEquals(3, grafo.numeroVertices());
        assertEquals(2, grafo.numeroArestas());
        assertNotNull(grafo.vertice("joão pessoa"));
        assertEquals("João Pessoa", grafo.vertice("JOÃO PESSOA").nome());
        assertNotNull(grafo.arestaEntre("Patos", "Campina Grande"));
    }

    @Test
    void aceitaColunasEmOrdemDiferenteEComentarios() throws IOException {
        Files.writeString(pasta.resolve("cidades.csv"), """
                # cidades do exemplo
                longitude,nome,latitude
                -34.8450,João Pessoa,-7.1195
                -35.8811,Campina Grande,-7.2306
                """);
        Files.writeString(pasta.resolve("estradas.csv"), """
                tempo_min,distancia_km,destino,origem
                100,125,Campina Grande,João Pessoa
                """);

        Grafo grafo = new CarregadorDados().carregarPasta(pasta);

        assertEquals(2, grafo.numeroVertices());
        assertEquals(1, grafo.numeroArestas());
    }

    @Test
    void rejeitaCidadeInexistenteNaEstrada() throws IOException {
        Files.writeString(pasta.resolve("cidades.csv"), """
                nome,latitude,longitude
                A,-7.0,-35.0
                B,-7.1,-35.1
                """);
        Files.writeString(pasta.resolve("estradas.csv"), """
                origem,destino,distancia_km,tempo_min
                A,C,10,10
                """);

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new CarregadorDados().carregarPasta(pasta));
        assertTrue(erro.getMessage().contains("C"));
    }

    @Test
    void rejeitaPesoNaoPositivo() throws IOException {
        Files.writeString(pasta.resolve("cidades.csv"), """
                nome,latitude,longitude
                A,-7.0,-35.0
                B,-7.1,-35.1
                """);
        Files.writeString(pasta.resolve("estradas.csv"), """
                origem,destino,distancia_km,tempo_min
                A,B,0,10
                """);

        assertThrows(DadosInvalidosException.class,
                () -> new CarregadorDados().carregarPasta(pasta));
    }

    @Test
    void rejeitaColunaObrigatoriaAusente() throws IOException {
        Files.writeString(pasta.resolve("cidades.csv"), """
                nome,latitude
                A,-7.0
                """);
        Files.writeString(pasta.resolve("estradas.csv"), """
                origem,destino,distancia_km,tempo_min
                A,A,10,10
                """);

        assertThrows(DadosInvalidosException.class,
                () -> new CarregadorDados().carregarPasta(pasta));
    }
}
