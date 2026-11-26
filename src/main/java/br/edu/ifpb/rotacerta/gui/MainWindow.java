package br.edu.ifpb.rotacerta.gui;

import br.edu.ifpb.rotacerta.algorithm.Dijkstra;
import br.edu.ifpb.rotacerta.algorithm.ResultadoRota;
import br.edu.ifpb.rotacerta.graph.Grafo;
import br.edu.ifpb.rotacerta.graph.TipoPeso;
import br.edu.ifpb.rotacerta.graph.Vertice;
import br.edu.ifpb.rotacerta.io.CarregadorDados;
import br.edu.ifpb.rotacerta.io.DadosInvalidosException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainWindow extends JFrame {

    private static final Color AZUL = new Color(0x1D4E89);
    private static final Color AZUL_ESCURO = new Color(0x163A66);
    private static final Color FUNDO = new Color(0xF1F5F9);
    private static final Color TEXTO = new Color(0x1E293B);
    private static final Color TEXTO_SUAVE = new Color(0x64748B);
    private static final Color BORDA = new Color(0xE2E8F0);
    private static final Color VERDE = new Color(0x15803D);
    private static final Color VERMELHO = new Color(0xB91C1C);
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final GrafoPanel painelGrafo = new GrafoPanel();
    private final JComboBox<String> origemCombo = new JComboBox<>();
    private final JComboBox<String> destinoCombo = new JComboBox<>();
    private final JRadioButton distanciaRadio = new JRadioButton("Menor distância (km)", true);
    private final JRadioButton tempoRadio = new JRadioButton("Menor tempo (min)");
    private final JButton calcularButton = new BotaoPrimario("Calcular rota");
    private final JButton carregarButton = new JButton("Carregar pasta...");
    private final JButton trocarButton = new JButton(
            "<html><u>Trocar origem e destino</u></html>");
    private final JCheckBox pesosCheck = new JCheckBox("Mostrar todos os pesos");
    private final JButton zoomOutButton = new JButton("\u2212");
    private final JButton zoomInButton = new JButton("+");
    private final JButton ajustarButton = new JButton("Ajustar");
    private final JLabel zoomLabel = new JLabel("100%");
    private final JLabel statusDados = new JLabel("Nenhum conjunto carregado");
    private final JLabel statusRodape = new JLabel("Pronto.");
    private final JLabel resultadoTitulo = new JLabel("Nenhuma rota calculada");
    private final JTextArea resultadoArea = new JTextArea();

    private Grafo grafo;
    private Dijkstra dijkstra;
    private Path pastaDados;
    private boolean atualizandoCombos;

    public MainWindow() {
        super("RotaCerta — Rotas para pipeiros");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1220, 820);
        setLocationRelativeTo(null);

        setContentPane(construirConteudo());
        configurarEventos();
        carregarDadosIniciais();
    }

    private JPanel construirConteudo() {
        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBackground(FUNDO);
        raiz.setBorder(new EmptyBorder(20, 24, 16, 24));
        raiz.add(cabecalho(), BorderLayout.NORTH);
        raiz.add(corpo(), BorderLayout.CENTER);
        raiz.add(rodape(), BorderLayout.SOUTH);
        return raiz;
    }

    private JPanel cabecalho() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("RotaCerta");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 31));
        titulo.setForeground(AZUL);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel(
                "Rotas de menor custo para o transporte de água • Paraíba");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(2));
        textos.add(subtitulo);

        JPanel badge = new JPanel();
        badge.setBackground(new Color(0xE0F2FE));
        badge.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel badgeTexto = new JLabel("TEMA 5 • MENOR CAMINHO");
        badgeTexto.setFont(new Font("SansSerif", Font.BOLD, 10));
        badgeTexto.setForeground(AZUL);
        badge.add(badgeTexto);

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        painel.add(textos, BorderLayout.WEST);
        painel.add(badgeWrap, BorderLayout.EAST);
        return painel;
    }

    private JPanel corpo() {
        JPanel corpo = new JPanel(new BorderLayout(18, 0));
        corpo.setOpaque(false);
        corpo.add(cartaoGrafo(), BorderLayout.CENTER);
        corpo.add(barraLateral(), BorderLayout.EAST);
        return corpo;
    }

    private JPanel cartaoGrafo() {
        JPanel cartao = new JPanel(new BorderLayout());
        cartao.setBackground(Color.WHITE);
        cartao.setBorder(BorderFactory.createLineBorder(BORDA));

        JPanel topo = new JPanel(new BorderLayout());
        topo.setBackground(Color.WHITE);
        topo.setBorder(new EmptyBorder(12, 16, 10, 16));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Mapa da malha rodoviária");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        titulo.setForeground(TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel(
                "Coordenadas aproximadas, usadas apenas para visualização.");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(1));
        textos.add(subtitulo);

        pesosCheck.setOpaque(false);
        pesosCheck.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pesosCheck.setForeground(TEXTO_SUAVE);
        pesosCheck.setFocusPainted(false);

        configurarBotaoZoom(zoomOutButton, 34);
        configurarBotaoZoom(zoomInButton, 34);
        configurarBotaoZoom(ajustarButton, 72);

        zoomLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        zoomLabel.setForeground(TEXTO_SUAVE);
        zoomLabel.setHorizontalAlignment(JLabel.CENTER);
        zoomLabel.setPreferredSize(new Dimension(48, 26));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controles.setOpaque(false);
        controles.add(pesosCheck);
        controles.add(Box.createHorizontalStrut(6));
        controles.add(zoomOutButton);
        controles.add(zoomLabel);
        controles.add(zoomInButton);
        controles.add(ajustarButton);

        JLabel dica = new JLabel("Roda: zoom • arraste: mover • duplo clique: ajustar");
        dica.setFont(new Font("SansSerif", Font.PLAIN, 10));
        dica.setForeground(TEXTO_SUAVE);

        JPanel rodapeMapa = new JPanel(new BorderLayout());
        rodapeMapa.setBackground(Color.WHITE);
        rodapeMapa.setBorder(new EmptyBorder(6, 16, 9, 16));
        rodapeMapa.add(dica, BorderLayout.WEST);
        rodapeMapa.add(controles, BorderLayout.EAST);

        topo.add(textos, BorderLayout.WEST);

        cartao.add(topo, BorderLayout.NORTH);
        cartao.add(painelGrafo, BorderLayout.CENTER);
        cartao.add(rodapeMapa, BorderLayout.SOUTH);
        return cartao;
    }

    private JComponent barraLateral() {
        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(0, 0, 0, 0));

        conteudo.add(cartaoDados());
        conteudo.add(Box.createVerticalStrut(10));
        conteudo.add(cartaoConsulta());
        conteudo.add(Box.createVerticalStrut(10));
        conteudo.add(cartaoResultado());
        conteudo.add(Box.createVerticalGlue());

        JScrollPane rolagem = new JScrollPane(conteudo);
        rolagem.setBorder(null);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rolagem.setPreferredSize(new Dimension(374, 0));
        return rolagem;
    }

    private JPanel cartaoDados() {
        JPanel painel = cartaoBase();
        painel.add(tituloSecao("CONJUNTO DE DADOS"));
        painel.add(Box.createVerticalStrut(6));

        statusDados.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusDados.setForeground(TEXTO);
        statusDados.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(statusDados);
        painel.add(Box.createVerticalStrut(10));

        configurarBotaoSecundario(carregarButton);
        carregarButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        carregarButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        painel.add(carregarButton);

        return finalizarCartao(painel);
    }

    private JPanel cartaoConsulta() {
        JPanel painel = cartaoBase();
        painel.add(tituloSecao("CONSULTA DE ROTA"));
        painel.add(Box.createVerticalStrut(8));

        painel.add(rotulo("Origem"));
        painel.add(Box.createVerticalStrut(4));
        configurarCombo(origemCombo);
        painel.add(origemCombo);
        painel.add(Box.createVerticalStrut(10));

        painel.add(rotulo("Destino"));
        painel.add(Box.createVerticalStrut(4));
        configurarCombo(destinoCombo);
        painel.add(destinoCombo);
        painel.add(Box.createVerticalStrut(4));

        trocarButton.setBorder(null);
        trocarButton.setContentAreaFilled(false);
        trocarButton.setFocusPainted(false);
        trocarButton.setForeground(AZUL);
        trocarButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
        trocarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        trocarButton.setToolTipText("Inverter as cidades de origem e destino");

        JPanel linhaTroca = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaTroca.setOpaque(false);
        linhaTroca.setAlignmentX(Component.LEFT_ALIGNMENT);
        linhaTroca.add(trocarButton);
        painel.add(linhaTroca);
        painel.add(Box.createVerticalStrut(8));

        painel.add(rotulo("Critério"));
        painel.add(Box.createVerticalStrut(4));

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(distanciaRadio);
        grupo.add(tempoRadio);

        JPanel criterios = new JPanel(new GridLayout(1, 2, 8, 0));
        criterios.setOpaque(false);
        criterios.setAlignmentX(Component.LEFT_ALIGNMENT);
        criterios.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        for (JRadioButton radio : List.of(distanciaRadio, tempoRadio)) {
            radio.setOpaque(false);
            radio.setFont(new Font("SansSerif", Font.PLAIN, 11));
            radio.setForeground(TEXTO);
            radio.setFocusPainted(false);
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            criterios.add(radio);
        }
        painel.add(criterios);

        painel.add(Box.createVerticalStrut(10));
        calcularButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        calcularButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        calcularButton.setPreferredSize(new Dimension(300, 42));
        painel.add(calcularButton);

        return finalizarCartao(painel);
    }

    private JPanel cartaoResultado() {
        JPanel painel = cartaoBase();
        painel.add(tituloSecao("RESULTADO"));
        painel.add(Box.createVerticalStrut(6));

        resultadoTitulo.setFont(new Font("SansSerif", Font.BOLD, 13));
        resultadoTitulo.setForeground(TEXTO_SUAVE);
        resultadoTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(resultadoTitulo);
        painel.add(Box.createVerticalStrut(8));

        resultadoArea.setEditable(false);
        resultadoArea.setLineWrap(true);
        resultadoArea.setWrapStyleWord(true);
        resultadoArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resultadoArea.setRows(6);
        resultadoArea.setForeground(TEXTO);
        resultadoArea.setBackground(new Color(0xF8FAFC));
        resultadoArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        resultadoArea.setText("Informe a origem, o destino e o critério,\n"
                + "depois clique em \"Calcular rota\".");

        JScrollPane rolagem = new JScrollPane(resultadoArea);
        rolagem.setBorder(BorderFactory.createLineBorder(BORDA));
        rolagem.setAlignmentX(Component.LEFT_ALIGNMENT);
        rolagem.setPreferredSize(new Dimension(320, 130));
        rolagem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        rolagem.getViewport().setBackground(new Color(0xF8FAFC));
        painel.add(rolagem);

        return finalizarCartao(painel);
    }

    private JPanel cartaoBase() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(Color.WHITE);
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA),
                new EmptyBorder(12, 14, 12, 14)));
        painel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return painel;
    }

    private JPanel finalizarCartao(JPanel painel) {
        painel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                painel.getPreferredSize().height));
        return painel;
    }

    private JLabel tituloSecao(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(TEXTO_SUAVE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel rotulo(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(TEXTO_SUAVE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void configurarCombo(JComboBox<String> combo) {
        combo.setEditable(true);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        combo.setPreferredSize(new Dimension(300, 32));
        combo.setPrototypeDisplayValue("Campina Grande");
    }

    private void configurarBotaoSecundario(JButton botao) {
        botao.setFont(new Font("SansSerif", Font.PLAIN, 12));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void configurarBotaoZoom(JButton botao, int largura) {
        botao.setFont(new Font("SansSerif", Font.BOLD, 13));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setMargin(new java.awt.Insets(2, 6, 2, 6));
        botao.setPreferredSize(new Dimension(largura, 26));
    }

    private void atualizarRotuloZoom() {
        zoomLabel.setText(String.format(BR, "%.0f%%", painelGrafo.getZoom() * 100));
    }

    private JPanel rodape() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setOpaque(false);
        painel.setBorder(new EmptyBorder(0, 4, 0, 4));

        statusRodape.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusRodape.setForeground(TEXTO_SUAVE);

        JLabel info = new JLabel("Dijkstra • O((V+E) log V)");
        info.setFont(new Font("SansSerif", Font.PLAIN, 11));
        info.setForeground(TEXTO_SUAVE);

        painel.add(statusRodape, BorderLayout.WEST);
        painel.add(info, BorderLayout.EAST);
        return painel;
    }

    private void configurarEventos() {
        calcularButton.addActionListener(e -> calcularRota());
        carregarButton.addActionListener(e -> escolherPasta());
        trocarButton.addActionListener(e -> trocarCidades());
        pesosCheck.addActionListener(e -> painelGrafo.setMostrarPesos(pesosCheck.isSelected()));
        zoomInButton.addActionListener(e -> painelGrafo.zoomIn());
        zoomOutButton.addActionListener(e -> painelGrafo.zoomOut());
        ajustarButton.addActionListener(e -> painelGrafo.ajustarVisao());
        painelGrafo.addPropertyChangeListener("zoom", e -> atualizarRotuloZoom());

        origemCombo.addActionListener(e -> {
            if (!atualizandoCombos) {
                limparResultado("Consulta alterada. Clique em \"Calcular rota\".");
            }
        });
        destinoCombo.addActionListener(e -> {
            if (!atualizandoCombos) {
                limparResultado("Consulta alterada. Clique em \"Calcular rota\".");
            }
        });
        distanciaRadio.addActionListener(e -> {
            painelGrafo.setCriterio(TipoPeso.DISTANCIA);
            if (!atualizandoCombos) {
                limparResultado("Critério alterado. Clique em \"Calcular rota\".");
            }
        });
        tempoRadio.addActionListener(e -> {
            painelGrafo.setCriterio(TipoPeso.TEMPO);
            if (!atualizandoCombos) {
                limparResultado("Critério alterado. Clique em \"Calcular rota\".");
            }
        });

        getRootPane().setDefaultButton(calcularButton);
        configurarEnter(origemCombo);
        configurarEnter(destinoCombo);
    }

    private void configurarEnter(JComboBox<String> combo) {
        JComponent editor = (JComponent) combo.getEditor().getEditorComponent();
        editor.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "calcularRota");
        editor.getActionMap().put("calcularRota", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcularRota();
            }
        });
    }

    private void carregarDadosIniciais() {
        Path padrao = localizarDadosPadrao();
        if (padrao != null) {
            carregarDados(padrao);
            return;
        }

        statusDados.setText("Nenhum conjunto carregado");
        statusRodape.setText("Selecione uma pasta com cidades.csv e estradas.csv.");
        limparResultado("Clique em \"Carregar pasta...\" para começar.");
        definirControlesHabilitados(false);
    }

    private Path localizarDadosPadrao() {
        List<Path> candidatos = List.of(
                Path.of("data", "pequeno"),
                Path.of("codigo", "data", "pequeno"),
                Path.of("..", "data", "pequeno"),
                Path.of("..", "codigo", "data", "pequeno")
        );

        for (Path candidato : candidatos) {
            if (Files.isRegularFile(candidato.resolve(CarregadorDados.ARQUIVO_CIDADES))
                    && Files.isRegularFile(candidato.resolve(CarregadorDados.ARQUIVO_ESTRADAS))) {
                return candidato.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private void escolherPasta() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Selecione a pasta que contém cidades.csv e estradas.csv");
        seletor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        seletor.setAcceptAllFileFilterUsed(false);

        Path inicio = pastaDados != null ? pastaDados.getParent() : Path.of(".").toAbsolutePath();
        if (inicio != null) {
            seletor.setCurrentDirectory(inicio.toFile());
        }

        if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            carregarDados(seletor.getSelectedFile().toPath());
        }
    }

    private void carregarDados(Path pasta) {
        try {
            Grafo novoGrafo = new CarregadorDados().carregarPasta(pasta);
            this.grafo = novoGrafo;
            this.dijkstra = new Dijkstra(novoGrafo);
            this.pastaDados = pasta.toAbsolutePath().normalize();

            painelGrafo.setGrafo(novoGrafo);
            painelGrafo.setCriterio(criterioSelecionado());
            atualizarCidades();

            String nomePasta = pasta.getFileName() == null
                    ? pasta.toString() : pasta.getFileName().toString();
            statusDados.setText("<html><b>" + escaparHtml(nomePasta) + "</b><br>"
                    + novoGrafo.numeroVertices() + " cidades • "
                    + novoGrafo.numeroArestas() + " estradas</html>");
            statusDados.setToolTipText(this.pastaDados.toString());
            statusRodape.setText("Conjunto de dados carregado.");
            limparResultado("Selecione origem e destino ou pressione Calcular rota.");
            definirControlesHabilitados(true);
        } catch (IOException e) {
            mostrarErro("Não foi possível ler a pasta selecionada:\n" + e.getMessage());
            statusRodape.setText("Erro ao carregar o conjunto de dados.");
        } catch (DadosInvalidosException e) {
            mostrarErro("Os dados são inválidos:\n" + e.getMessage());
            statusRodape.setText("Erro de validação dos dados.");
        }
    }

    private void atualizarCidades() {
        if (grafo == null) {
            return;
        }

        List<String> nomes = new ArrayList<>();
        for (Vertice vertice : grafo.vertices()) {
            nomes.add(vertice.nome());
        }
        Collator collator = Collator.getInstance(BR);
        nomes.sort(collator);

        atualizandoCombos = true;
        try {
            origemCombo.removeAllItems();
            destinoCombo.removeAllItems();
            for (String nome : nomes) {
                origemCombo.addItem(nome);
                destinoCombo.addItem(nome);
            }

            String origemPadrao = nomes.contains("Patos") ? "Patos" : nomes.get(0);
            String destinoPadrao = nomes.contains("João Pessoa")
                    ? "João Pessoa" : nomes.get(nomes.size() - 1);

            origemCombo.setSelectedItem(origemPadrao);
            destinoCombo.setSelectedItem(destinoPadrao);
        } finally {
            atualizandoCombos = false;
        }
    }

    private void trocarCidades() {
        String origem = textoCombo(origemCombo);
        String destino = textoCombo(destinoCombo);

        atualizandoCombos = true;
        try {
            origemCombo.setSelectedItem(destino);
            destinoCombo.setSelectedItem(origem);
        } finally {
            atualizandoCombos = false;
        }
        limparResultado("Consulta alterada. Clique em \"Calcular rota\".");
    }

    private void calcularRota() {
        if (grafo == null || dijkstra == null) {
            mostrarErro("Nenhum conjunto de dados está carregado.");
            return;
        }

        String origem = textoCombo(origemCombo);
        String destino = textoCombo(destinoCombo);
        TipoPeso criterio = criterioSelecionado();

        if (origem.isBlank() || destino.isBlank()) {
            mostrarErro("Informe a cidade de origem e a cidade de destino.");
            return;
        }

        try {
            ResultadoRota resultado = dijkstra.calcular(origem, destino, criterio);
            painelGrafo.setCriterio(criterio);

            if (!resultado.encontrouRota()) {
                painelGrafo.setRota(List.of());
                resultadoTitulo.setText("Não existe rota");
                resultadoTitulo.setForeground(VERMELHO);
                resultadoArea.setText("Não há caminho entre \"" + origem + "\" e \""
                        + destino + "\" neste conjunto de dados.\n\n"
                        + "Vértices explorados: " + resultado.verticesExplorados()
                        + " de " + grafo.numeroVertices() + "\n"
                        + String.format(BR, "Tempo de execução: %.3f ms",
                        resultado.tempoExecucaoMillis()));
                resultadoArea.setCaretPosition(0);
                statusRodape.setText("Sem rota entre as cidades informadas.");
                return;
            }

            painelGrafo.setRota(resultado.caminho());
            resultadoTitulo.setText("Rota encontrada");
            resultadoTitulo.setForeground(VERDE);
            resultadoArea.setText(formatarResultado(resultado));
            resultadoArea.setCaretPosition(0);
            statusRodape.setText("Rota com " + resultado.caminho().size()
                    + " cidades • " + String.format(BR, "%.0f km", resultado.distanciaTotalKm()));
        } catch (IllegalArgumentException e) {
            painelGrafo.setRota(List.of());
            resultadoTitulo.setText("Consulta inválida");
            resultadoTitulo.setForeground(VERMELHO);
            resultadoArea.setText(e.getMessage()
                    + "\n\nEscolha uma cidade existente nas listas de origem e destino.");
            resultadoArea.setCaretPosition(0);
            mostrarErro(e.getMessage());
        }
    }

    private String formatarResultado(ResultadoRota resultado) {
        StringBuilder texto = new StringBuilder();
        texto.append(String.join(" → ", resultado.caminho()));
        texto.append("\n\n");
        texto.append(String.format(BR, "%.1f km • %.1f min%n",
                resultado.distanciaTotalKm(), resultado.tempoTotalMin()));
        texto.append(resultado.criterio().descricao())
                .append(" (").append(resultado.criterio().unidade()).append(")\n");
        texto.append("Explorados: ").append(resultado.verticesExplorados())
                .append(" de ").append(grafo.numeroVertices())
                .append(" • ").append(String.format(BR, "%.3f ms",
                        resultado.tempoExecucaoMillis()));
        return texto.toString();
    }

    private void limparResultado(String dica) {
        painelGrafo.setRota(List.of());
        resultadoTitulo.setText("Nenhuma rota calculada");
        resultadoTitulo.setForeground(TEXTO_SUAVE);
        resultadoArea.setText(dica);
        resultadoArea.setCaretPosition(0);
    }

    private void definirControlesHabilitados(boolean habilitado) {
        origemCombo.setEnabled(habilitado);
        destinoCombo.setEnabled(habilitado);
        distanciaRadio.setEnabled(habilitado);
        tempoRadio.setEnabled(habilitado);
        calcularButton.setEnabled(habilitado);
        trocarButton.setEnabled(habilitado);
        pesosCheck.setEnabled(habilitado);
        zoomInButton.setEnabled(habilitado);
        zoomOutButton.setEnabled(habilitado);
        ajustarButton.setEnabled(habilitado);
    }

    private TipoPeso criterioSelecionado() {
        return tempoRadio.isSelected() ? TipoPeso.TEMPO : TipoPeso.DISTANCIA;
    }

    private String textoCombo(JComboBox<String> combo) {
        Object valor = combo.isEditable()
                ? combo.getEditor().getItem()
                : combo.getSelectedItem();
        return valor == null ? "" : valor.toString().trim();
    }

    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "RotaCerta",
                JOptionPane.ERROR_MESSAGE);
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static final class BotaoPrimario extends JButton {

        private BotaoPrimario(String texto) {
            super(texto);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color cor = AZUL;
                if (getModel().isPressed()) {
                    cor = AZUL_ESCURO;
                } else if (getModel().isRollover()) {
                    cor = new Color(0x2563EB);
                }
                g2.setColor(cor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
