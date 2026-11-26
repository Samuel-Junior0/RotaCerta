package br.edu.ifpb.rotacerta.gui;

import br.edu.ifpb.rotacerta.graph.Aresta;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.TipoPeso;
import br.edu.ifpb.rotacerta.graph.Vertice;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GrafoPanel extends JPanel {

    private static final Color FUNDO = new Color(0xF8FAFC);
    private static final Color ESTRADA = new Color(0xCBD5E1);
    private static final Color ROTA_CLARA = new Color(0xBFDBFE);
    private static final Color ROTA = new Color(0x2563EB);
    private static final Color TEXTO = new Color(0x334155);
    private static final Color TEXTO_SUAVE = new Color(0x64748B);
    private static final Color ORIGEM = new Color(0x15803D);
    private static final Color DESTINO = new Color(0xC2410C);
    private static final Color NO = new Color(0xFFFFFF);
    private static final Color BORDA_NO = new Color(0x64748B);
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private static final double ZOOM_MINIMO = 1.0;
    private static final double ZOOM_MAXIMO = 64.0;
    private static final double ZOOM_PASSO = 1.25;
    private static final double ZOOM_MINIMO_ROTULOS = 1.5;
    private static final int LIMITE_ROTULOS_EXTRAS = 180;
    private static final int MARGEM_PROJECAO = 54;

    private Grafo grafo;
    private List<String> rota = List.of();
    private Set<String> verticesRota = Set.of();
    private Set<String> chavesRota = Set.of();
    private TipoPeso criterio = TipoPeso.DISTANCIA;
    private boolean mostrarPesos;
    private final List<Rectangle2D> rotulosPesos = new ArrayList<>();

    private final Map<String, Point2D.Double> posicoes = new HashMap<>();
    private List<Aresta> arestas = List.of();

    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    private boolean limitesValidos;
    private double centroLat;
    private double centroLon;
    private double centroBaseLat;
    private double centroBaseLon;
    private double zoom = ZOOM_MINIMO;

    private boolean arrastando;
    private Point ultimoArraste;

    public GrafoPanel() {
        setOpaque(true);
        setBackground(FUNDO);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        ToolTipManager.sharedInstance().registerComponent(this);

        MouseAdapter navegacao = new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (grafo == null) {
                    return;
                }
                double fator = Math.pow(1.15, -e.getPreciseWheelRotation());
                aplicarZoom(fator, e.getPoint());
                e.consume();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (grafo != null && SwingUtilities.isLeftMouseButton(e)) {
                    arrastando = true;
                    ultimoArraste = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!arrastando || ultimoArraste == null) {
                    return;
                }
                Point atual = e.getPoint();
                deslocarVisao(atual.x - ultimoArraste.x, atual.y - ultimoArraste.y);
                ultimoArraste = atual;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (arrastando) {
                    arrastando = false;
                    ultimoArraste = null;
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    ajustarVisao();
                }
            }
        };

        addMouseListener(navegacao);
        addMouseMotionListener(navegacao);
        addMouseWheelListener(navegacao);
    }

    public void setGrafo(Grafo grafo) {
        this.grafo = grafo;
        this.rota = List.of();
        this.verticesRota = Set.of();
        this.chavesRota = Set.of();
        recalcularGeometria();
        ajustarVisao();
        repaint();
    }

    public double getZoom() {
        return zoom;
    }

    public void zoomIn() {
        aplicarZoom(ZOOM_PASSO, centroDoPainel());
    }

    public void zoomOut() {
        aplicarZoom(1.0 / ZOOM_PASSO, centroDoPainel());
    }

    public void ajustarVisao() {
        double zoomAnterior = zoom;
        zoom = ZOOM_MINIMO;
        centroLat = centroBaseLat;
        centroLon = centroBaseLon;
        if (Double.compare(zoomAnterior, zoom) != 0) {
            firePropertyChange("zoom", Double.valueOf(zoomAnterior), Double.valueOf(zoom));
        }
        repaint();
    }

    public void setRota(List<String> caminho) {
        this.rota = caminho == null ? List.of() : List.copyOf(caminho);
        Set<String> vertices = new HashSet<>();
        Set<String> chaves = new HashSet<>();
        for (int i = 0; i < rota.size(); i++) {
            vertices.add(rota.get(i));
            if (i + 1 < rota.size()) {
                chaves.add(chave(rota.get(i), rota.get(i + 1)));
            }
        }
        this.verticesRota = Set.copyOf(vertices);
        this.chavesRota = Set.copyOf(chaves);
        repaint();
    }

    public void setCriterio(TipoPeso criterio) {
        if (criterio != null && criterio != this.criterio) {
            this.criterio = criterio;
            repaint();
        }
    }

    public void setMostrarPesos(boolean mostrarPesos) {
        this.mostrarPesos = mostrarPesos;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (grafo == null || grafo.numeroVertices() == 0) {
                desenharEstadoVazio(g2);
                return;
            }

            atualizarProjecao();
            desenharEstradas(g2);
            desenharVertices(g2);
            desenharRotulos(g2);
            desenharLegenda(g2);
        } finally {
            g2.dispose();
        }
    }

    private void desenharEstadoVazio(Graphics2D g2) {
        String mensagem = "Carregue um conjunto de dados para visualizar o grafo.";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(TEXTO_SUAVE);
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(mensagem)) / 2;
        int y = getHeight() / 2;
        g2.drawString(mensagem, Math.max(16, x), y);
    }

    private void recalcularGeometria() {
        posicoes.clear();
        arestas = List.of();
        limitesValidos = false;
        if (grafo == null || grafo.numeroVertices() == 0) {
            return;
        }

        minLat = Double.POSITIVE_INFINITY;
        maxLat = Double.NEGATIVE_INFINITY;
        minLon = Double.POSITIVE_INFINITY;
        maxLon = Double.NEGATIVE_INFINITY;

        for (Vertice vertice : grafo.vertices()) {
            minLat = Math.min(minLat, vertice.latitude());
            maxLat = Math.max(maxLat, vertice.latitude());
            minLon = Math.min(minLon, vertice.longitude());
            maxLon = Math.max(maxLon, vertice.longitude());
        }

        limitesValidos = true;
        centroBaseLat = (minLat + maxLat) / 2.0;
        centroBaseLon = (minLon + maxLon) / 2.0;
        centroLat = centroBaseLat;
        centroLon = centroBaseLon;
        arestas = montarArestasUnicas();
    }

    private double escalaBase() {
        if (!limitesValidos) {
            return 1.0;
        }
        double faixaLat = Math.max(maxLat - minLat, 1e-6);
        double faixaLon = Math.max(maxLon - minLon, 1e-6);
        double largura = Math.max(getWidth() - 2.0 * MARGEM_PROJECAO, 1);
        double altura = Math.max(getHeight() - 2.0 * MARGEM_PROJECAO, 1);
        return Math.min(largura / faixaLon, altura / faixaLat);
    }

    private double escalaAtual() {
        return escalaBase() * zoom;
    }

    private void atualizarProjecao() {
        posicoes.clear();
        if (!limitesValidos) {
            return;
        }

        double escala = escalaAtual();
        double meioX = getWidth() / 2.0;
        double meioY = getHeight() / 2.0;

        for (Vertice vertice : grafo.vertices()) {
            double x = meioX + (vertice.longitude() - centroLon) * escala;
            double y = meioY - (vertice.latitude() - centroLat) * escala;
            posicoes.put(vertice.nome(), new Point2D.Double(x, y));
        }
    }

    private void aplicarZoom(double fator, Point ancora) {
        if (!limitesValidos || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        double novoZoom = Math.max(ZOOM_MINIMO,
                Math.min(ZOOM_MAXIMO, zoom * fator));
        if (Math.abs(novoZoom - zoom) < 1e-6) {
            return;
        }

        double escalaAntes = escalaAtual();
        double meioX = getWidth() / 2.0;
        double meioY = getHeight() / 2.0;
        double ancoraX = ancora == null ? meioX : ancora.x;
        double ancoraY = ancora == null ? meioY : ancora.y;

        double lonAncora = centroLon + (ancoraX - meioX) / escalaAntes;
        double latAncora = centroLat - (ancoraY - meioY) / escalaAntes;

        double zoomAnterior = zoom;
        zoom = novoZoom;
        double escalaDepois = escalaAtual();
        centroLon = lonAncora - (ancoraX - meioX) / escalaDepois;
        centroLat = latAncora + (ancoraY - meioY) / escalaDepois;
        limitarCentro();

        firePropertyChange("zoom", Double.valueOf(zoomAnterior), Double.valueOf(zoom));
        repaint();
    }

    private void deslocarVisao(int deltaX, int deltaY) {
        if (!limitesValidos) {
            return;
        }
        double escala = escalaAtual();
        if (escala <= 0) {
            return;
        }
        centroLon -= deltaX / escala;
        centroLat += deltaY / escala;
        limitarCentro();
        repaint();
    }

    private void limitarCentro() {
        if (!limitesValidos) {
            return;
        }
        centroLat = Math.max(minLat, Math.min(maxLat, centroLat));
        centroLon = Math.max(minLon, Math.min(maxLon, centroLon));
    }

    private Point centroDoPainel() {
        return new Point(getWidth() / 2, getHeight() / 2);
    }

    private boolean visivel(Point2D ponto) {
        return ponto.getX() >= -MARGEM_PROJECAO
                && ponto.getX() <= getWidth() + MARGEM_PROJECAO
                && ponto.getY() >= -MARGEM_PROJECAO
                && ponto.getY() <= getHeight() + MARGEM_PROJECAO;
    }

    private boolean segmentoVisivel(Point2D a, Point2D b) {
        double minX = Math.min(a.getX(), b.getX());
        double maxX = Math.max(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double maxY = Math.max(a.getY(), b.getY());
        return maxX >= -MARGEM_PROJECAO && minX <= getWidth() + MARGEM_PROJECAO
                && maxY >= -MARGEM_PROJECAO && minY <= getHeight() + MARGEM_PROJECAO;
    }

    private double distanciaAoCentro(Point2D ponto) {
        double dx = ponto.getX() - getWidth() / 2.0;
        double dy = ponto.getY() - getHeight() / 2.0;
        return dx * dx + dy * dy;
    }

    private void desenharEstradas(Graphics2D g2) {
        rotulosPesos.clear();
        Path2D.Float estradas = new Path2D.Float();
        boolean temEstradas = false;

        for (Aresta aresta : arestas) {
            Point2D.Double a = posicoes.get(aresta.origem());
            Point2D.Double b = posicoes.get(aresta.destino());
            if (a == null || b == null) {
                continue;
            }
            if (pertenceARota(aresta) || !segmentoVisivel(a, b)) {
                continue;
            }
            estradas.moveTo(a.x, a.y);
            estradas.lineTo(b.x, b.y);
            temEstradas = true;
        }

        if (temEstradas) {
            g2.setColor(ESTRADA);
            g2.setStroke(new BasicStroke(1.25f));
            g2.draw(estradas);
        }

        if (!verticesRota.isEmpty()) {
            for (Aresta aresta : arestas) {
                if (!pertenceARota(aresta)) {
                    continue;
                }
                Point2D.Double a = posicoes.get(aresta.origem());
                Point2D.Double b = posicoes.get(aresta.destino());
                if (a == null || b == null) {
                    continue;
                }

                g2.setColor(ROTA_CLARA);
                g2.setStroke(new BasicStroke(7.0f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);

                g2.setColor(ROTA);
                g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            }
        }

        if (mostrarPesos && arestas.size() <= 300) {
            for (Aresta aresta : arestas) {
                Point2D.Double a = posicoes.get(aresta.origem());
                Point2D.Double b = posicoes.get(aresta.destino());
                if (a == null || b == null) {
                    continue;
                }
                boolean pertenceRota = pertenceARota(aresta);
                if (!pertenceRota && !segmentoVisivel(a, b)) {
                    continue;
                }
                desenharPeso(g2, aresta, a, b, pertenceRota, rotulosPesos);
            }
        }
    }

    private boolean pertenceARota(Aresta aresta) {
        return !verticesRota.isEmpty()
                && verticesRota.contains(aresta.origem())
                && verticesRota.contains(aresta.destino())
                && chavesRota.contains(chave(aresta.origem(), aresta.destino()));
    }

    private void desenharPeso(Graphics2D g2, Aresta aresta, Point2D a, Point2D b,
                              boolean pertenceRota, List<Rectangle2D> ocupados) {
        double valor = aresta.peso(criterio);
        String texto = formatarPeso(valor) + (criterio == TipoPeso.DISTANCIA ? " km" : " min");

        int x = (int) Math.round((a.getX() + b.getX()) / 2.0);
        int y = (int) Math.round((a.getY() + b.getY()) / 2.0);

        Font fonte = new Font("SansSerif", pertenceRota ? Font.BOLD : Font.PLAIN, 9);
        g2.setFont(fonte);
        FontMetrics fm = g2.getFontMetrics();
        int largura = fm.stringWidth(texto) + 8;
        int altura = fm.getHeight() + 2;
        Rectangle2D retangulo = new Rectangle2D.Double(
                x - largura / 2.0, y - altura / 2.0, largura, altura);

        if (!pertenceRota) {
            for (Rectangle2D ocupado : ocupados) {
                if (ocupado.intersects(retangulo)) {
                    return;
                }
            }
        }
        ocupados.add(retangulo);

        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(new Color(248, 250, 252, 235));
        g2.fill(new RoundRectangle2D.Double(retangulo.getX(), retangulo.getY(),
                retangulo.getWidth(), retangulo.getHeight(), 6, 6));
        g2.setColor(pertenceRota ? ROTA : TEXTO_SUAVE);
        g2.drawString(texto, (int) (retangulo.getX() + 4),
                (int) (retangulo.getY() + fm.getAscent() + 1));
    }

    private void desenharVertices(Graphics2D g2) {
        boolean mostrarNosComuns = grafo.numeroVertices() <= 1500 || zoom >= 1.5;
        Path2D.Float comuns = new Path2D.Float();
        if (mostrarNosComuns) {
            int raioComum = grafo.numeroVertices() > 1500 ? (zoom >= 3.0 ? 5 : 3) : 5;
            for (Vertice vertice : grafo.vertices()) {
                Point2D.Double ponto = posicoes.get(vertice.nome());
                if (ponto == null || !visivel(ponto)) {
                    continue;
                }
                if (ehOrigem(vertice.nome()) || ehDestino(vertice.nome())
                        || naRota(vertice.nome())) {
                    continue;
                }
                comuns.append(new Ellipse2D.Double(
                        ponto.x - raioComum, ponto.y - raioComum,
                        raioComum * 2.0, raioComum * 2.0), false);
            }
        }

        if (comuns.getCurrentPoint() != null) {
            g2.setColor(NO);
            g2.fill(comuns);
            g2.setColor(BORDA_NO);
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(comuns);
        }

        if (verticesRota.isEmpty()) {
            return;
        }

        for (Vertice vertice : grafo.vertices()) {
            Point2D.Double ponto = posicoes.get(vertice.nome());
            if (ponto == null || !visivel(ponto)) {
                continue;
            }

            boolean ehOrigem = ehOrigem(vertice.nome());
            boolean ehDestino = ehDestino(vertice.nome());
            boolean naRota = naRota(vertice.nome());
            if (!ehOrigem && !ehDestino && !naRota) {
                continue;
            }
            int raio = (ehOrigem || ehDestino) ? 8 : 6;

            if (ehOrigem || ehDestino) {
                g2.setColor(ehOrigem
                        ? new Color(0x16A34A)
                        : new Color(0xEA580C));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
                g2.fillOval((int) ponto.x - 14, (int) ponto.y - 14, 28, 28);
                g2.setComposite(AlphaComposite.SrcOver);
            }

            g2.setColor(ehOrigem ? ORIGEM : ehDestino ? DESTINO : ROTA);
            g2.fillOval((int) ponto.x - raio, (int) ponto.y - raio, raio * 2, raio * 2);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval((int) ponto.x - raio, (int) ponto.y - raio, raio * 2, raio * 2);
        }
    }

    private void desenharRotulos(Graphics2D g2) {
        boolean malhaGrande = grafo.numeroVertices() > 80;
        boolean rotulosDetalhados = !malhaGrande || zoom >= ZOOM_MINIMO_ROTULOS;
        if (!rotulosDetalhados && verticesRota.isEmpty()) {
            return;
        }

        List<Vertice> ordenados;
        if (rotulosDetalhados) {
            ordenados = new ArrayList<>(grafo.vertices());
        } else {
            ordenados = new ArrayList<>(verticesRota.size());
            for (String nome : verticesRota) {
                Vertice vertice = grafo.vertice(nome);
                if (vertice != null) {
                    ordenados.add(vertice);
                }
            }
        }
        ordenados.sort(Comparator
                .comparingInt((Vertice vertice) -> prioridade(vertice.nome())).reversed()
                .thenComparingDouble(vertice -> {
                    Point2D.Double ponto = posicoes.get(vertice.nome());
                    return ponto == null ? Double.MAX_VALUE : distanciaAoCentro(ponto);
                }));

        List<Rectangle2D> ocupados = new ArrayList<>(rotulosPesos);
        int extrasDesenhados = 0;

        for (Vertice vertice : ordenados) {
            Point2D.Double ponto = posicoes.get(vertice.nome());
            if (ponto == null || !visivel(ponto)) {
                continue;
            }

            boolean extremo = ehOrigem(vertice.nome()) || ehDestino(vertice.nome());
            boolean importante = extremo || naRota(vertice.nome());

            if (!importante) {
                if (!rotulosDetalhados || extrasDesenhados >= LIMITE_ROTULOS_EXTRAS) {
                    continue;
                }
            } else if (!extremo && !rotulosDetalhados) {
                continue;
            }
            Font fonte = new Font("SansSerif", importante ? Font.BOLD : Font.PLAIN,
                    importante ? 11 : 10);
            g2.setFont(fonte);
            FontMetrics fm = g2.getFontMetrics();

            String texto = vertice.nome();
            int largura = fm.stringWidth(texto);
            int altura = fm.getHeight();
            int x = (int) Math.round(ponto.x);
            int y = (int) Math.round(ponto.y);

            int[][] deslocamentos = {
                    {12, -altura / 2},
                    {-largura - 12, -altura / 2},
                    {-largura / 2, -altura - 12},
                    {-largura / 2, 12},
                    {12, -altura - 10},
                    {12, 10},
                    {-largura - 12, -altura - 10},
                    {-largura - 12, 10},
                    {24, -altura - 22},
                    {-largura / 2, -altura - 26},
                    {24, 22},
                    {-largura / 2, 26}
            };

            Rectangle2D escolhido = null;
            for (int[] deslocamento : deslocamentos) {
                Rectangle2D candidato = new Rectangle2D.Double(
                        x + deslocamento[0] - 3,
                        y + deslocamento[1] - 2,
                        largura + 6,
                        altura + 4);
                if (candidato.getMinX() < 8 || candidato.getMaxX() > getWidth() - 8
                        || candidato.getMinY() < 8 || candidato.getMaxY() > getHeight() - 8) {
                    continue;
                }
                boolean colide = ocupados.stream().anyMatch(o -> o.intersects(candidato));
                if (!colide) {
                    escolhido = candidato;
                    break;
                }
            }

            if (escolhido == null) {
                if (!importante) {
                    continue;
                }
                double fx = Math.max(8, Math.min(x + 7.0, getWidth() - largura - 14.0));
                double fy = Math.max(8, Math.min(y - altura / 2.0, getHeight() - altura - 8.0));
                escolhido = new Rectangle2D.Double(fx, fy, largura + 6, altura + 4);
            }

            double alvoX = Math.max(escolhido.getMinX(), Math.min(x, escolhido.getMaxX()));
            double alvoY = Math.max(escolhido.getMinY(), Math.min(y, escolhido.getMaxY()));
            if (Point2D.distance(x, y, alvoX, alvoY) > 16) {
                g2.setColor(new Color(148, 163, 184, 170));
                g2.setStroke(new BasicStroke(0.8f));
                g2.draw(new Line2D.Double(x, y, alvoX, alvoY));
            }

            g2.setColor(new Color(248, 250, 252, 230));
            g2.fill(new RoundRectangle2D.Double(escolhido.getX(), escolhido.getY(),
                    escolhido.getWidth(), escolhido.getHeight(), 6, 6));

            g2.setColor(corDoRotulo(vertice.nome()));
            g2.drawString(texto, (float) (escolhido.getX() + 3),
                    (float) (escolhido.getY() + fm.getAscent() + 2));
            ocupados.add(escolhido);
            if (!importante) {
                extrasDesenhados++;
            }
        }
    }

    private void desenharLegenda(Graphics2D g2) {
        int largura = 318;
        int altura = 30;
        int x = 16;
        int y = getHeight() - altura - 14;

        g2.setColor(new Color(255, 255, 255, 230));
        g2.fill(new RoundRectangle2D.Double(x, y, largura, altura, 10, 10));
        g2.setColor(new Color(0xE2E8F0));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(x, y, largura, altura, 10, 10));

        int cursor = x + 12;
        cursor = desenharItemLegenda(g2, cursor, y + altura / 2, ORIGEM, "Origem");
        cursor = desenharItemLegenda(g2, cursor, y + altura / 2, DESTINO, "Destino");
        cursor = desenharItemLegenda(g2, cursor, y + altura / 2, ROTA, "Rota");
        desenharItemLegenda(g2, cursor, y + altura / 2, BORDA_NO, "Cidade");
    }

    private int desenharItemLegenda(Graphics2D g2, int x, int y, Color cor, String texto) {
        g2.setColor(cor);
        g2.fillOval(x, y - 4, 8, 8);
        g2.setColor(TEXTO_SUAVE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(texto, x + 12, y + 4);
        return x + 12 + g2.getFontMetrics().stringWidth(texto) + 18;
    }

    private List<Aresta> montarArestasUnicas() {
        Map<String, Aresta> unicas = new LinkedHashMap<>();
        for (Vertice vertice : grafo.vertices()) {
            for (Aresta aresta : grafo.vizinhos(vertice.nome())) {
                unicas.putIfAbsent(chave(aresta.origem(), aresta.destino()), aresta);
            }
        }
        return List.copyOf(unicas.values());
    }

    private static String chave(String a, String b) {
        String primeiro = a.compareToIgnoreCase(b) <= 0 ? a : b;
        String segundo = a.compareToIgnoreCase(b) <= 0 ? b : a;
        return primeiro.toLowerCase(Locale.ROOT) + "\u0000" + segundo.toLowerCase(Locale.ROOT);
    }

    private boolean ehOrigem(String nome) {
        return !rota.isEmpty() && rota.get(0).equalsIgnoreCase(nome);
    }

    private boolean ehDestino(String nome) {
        return !rota.isEmpty() && rota.get(rota.size() - 1).equalsIgnoreCase(nome);
    }

    private boolean naRota(String nome) {
        return verticesRota.contains(nome);
    }

    private int prioridade(String nome) {
        if (ehOrigem(nome) || ehDestino(nome)) {
            return 3;
        }
        return naRota(nome) ? 2 : 1;
    }

    private Color corDoRotulo(String nome) {
        if (ehOrigem(nome)) {
            return ORIGEM;
        }
        if (ehDestino(nome)) {
            return DESTINO;
        }
        return naRota(nome) ? ROTA : TEXTO;
    }

    private static String formatarPeso(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.05) {
            return String.format(BR, "%.0f", valor);
        }
        return String.format(BR, "%.1f", valor);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (grafo == null || posicoes.isEmpty()) {
            return null;
        }
        Point mouse = event.getPoint();
        String encontrado = null;
        double melhorDistancia = 14 * 14;

        for (Vertice vertice : grafo.vertices()) {
            Point2D.Double ponto = posicoes.get(vertice.nome());
            if (ponto != null) {
                double distancia = ponto.distanceSq(mouse);
                if (distancia < melhorDistancia) {
                    melhorDistancia = distancia;
                    encontrado = vertice.nome();
                }
            }
        }

        if (encontrado == null) {
            return null;
        }
        Vertice vertice = grafo.vertice(encontrado);
        return String.format(BR,
                "<html><b>%s</b><br>Latitude: %.4f<br>Longitude: %.4f</html>",
                vertice.nome(), vertice.latitude(), vertice.longitude());
    }
}
