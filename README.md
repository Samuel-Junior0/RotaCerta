# RotaCerta — código-fonte

Projeto Maven da aplicação. A documentação completa do trabalho está na pasta
[`../docs`](../docs) e o plano em [`../PLANO.md`](../PLANO.md).

## Requisitos

- JDK 17 ou superior.
- IntelliJ IDEA (Maven embutido) ou Maven 3.9+.

## Executar no IntelliJ

1. Abra **esta pasta** (`codigo/`) no IntelliJ e selecione o `pom.xml`.
2. Aguarde a sincronização do Maven.
3. Execute `br.edu.ifpb.rotacerta.Main` sem argumentos para abrir a interface.
4. Para o modo CLI, use o diretório de trabalho `codigo/` e os argumentos:

   ```
   --cli --dados data/pequeno --origem "Patos" --destino "João Pessoa" --criterio distancia
   ```

## Interface

Ao abrir a janela, o conjunto `data/pequeno` é carregado automaticamente. Escolha origem,
destino e critério, depois clique em **Calcular rota**. A rota é destacada no mapa e o
painel lateral mostra custo, vértices explorados e tempo de execução. Use **Carregar
pasta...** para testar outro conjunto de dados e **Mostrar todos os pesos** para exibir
as distâncias/tempos das arestas.

O mapa pode ser navegado: a roda do mouse aproxima/afasta, arrastar move a visão, duplo
clique ou **Ajustar** volta ao enquadramento inicial, e os botões **−**/**+** alteram o
zoom pelo centro. Nas instâncias média e grande, os nomes das cidades próximas aparecem
a partir de 150% de zoom; origem e destino continuam sempre rotulados.

## Executar pelo terminal

```bash
mvn clean package
java -jar target/rotacerta-1.0.0.jar --cli \
    --dados data/pequeno \
    --origem "Patos" \
    --destino "João Pessoa" \
    --criterio distancia
```

Testes:

```bash
mvn test
```

Benchmark reproduzível:

```bash
java -jar target/rotacerta-1.0.0.jar --benchmark \
    --repeticoes 30 --aquecimento 30 --saida ../docs/benchmark.md
```

Sem `--dados`, o benchmark usa as pastas `data/pequeno`, `data/medio` e `data/grande`
que existirem. Para medir uma instância específica, informe `--dados data/grande`. O
parâmetro pode ser repetido para comparar mais de uma pasta. A saída `.md` gera uma
tabela pronta para o relatório; `.csv` gera uma linha por instância/critério.

## Dados

Cada pasta em `data/` contém `cidades.csv` e `estradas.csv`:

- `data/pequeno` — instância didática com 24 cidades reais da Paraíba e 31 estradas;
- `data/medio` — 300 cidades e 900 estradas;
- `data/grande` — 5.000 cidades e 20.000 estradas.

```csv
nome,latitude,longitude
João Pessoa,-7.1195,-34.8450
```

```csv
origem,destino,distancia_km,tempo_min
João Pessoa,Campina Grande,125,100
```

Para usar outro conjunto, aponte `--dados` para outra pasta. Nenhuma alteração no código
é necessária.

## Organização

```
codigo/
├── pom.xml
├── data/                    # pequeno, medio e grande
├── src/main/java/...        # aplicação (graph, algorithm, io, cli, gui)
└── src/test/java/...        # testes JUnit
```
