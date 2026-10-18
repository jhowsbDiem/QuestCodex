# QuestCodex — survival competitivo por conquistas ⚔️

[![Paper](https://img.shields.io/badge/Paper-26.3-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.1.0-brightgreen.svg)](https://github.com/jhowsbDiem/QuestCodex)

QuestCodex é um minigame de survival competitivo para servidores Minecraft
Paper 26.3. Cada partida dura 45 minutos: os jogadores escolhem um kit de
habilidade num coliseu nas alturas, saltam para um mapa de 700×700 blocos e
somam pontos cumprindo conquistas — do primeiro tronco de madeira ao Ender
Dragon — e abatendo rivais. No fim, o Top 3 sobe ao pódio e o mapa é
regerado para a próxima partida. Tudo roda no servidor, em dois plugins: os
jogadores entram com o cliente original (vanilla) do Minecraft Java Edition
26.3.

---

## 🚀 Funcionalidades

* **Coliseu nas alturas:** o lobby fica a Y = 165, sobre o spawn, com quatro portões trancados até o início. Dentro do coliseu ninguém leva dano, passa fome, constrói ou ataca. Antes do início, só OPs, jogadores no criativo ou espectador e as tags DONO, ADMIN, MOD e DEV atravessam os portões.
* **Início da partida:** `/partida iniciar` deixa o mundo de dia e no Difícil, abre os portões e dá a todos 2 minutos de invencibilidade com Speed I. Cada jogador recebe o item do seu kit, o Livro do QuestCodex e a Bússola Caça-Pontos.
* **Cronômetro de 45 minutos:** aparece na scoreboard, com avisos aos 30, 15, 10, 5 e 1 minuto. Ao zerar — ou com `/partida finalizar` — começa a cerimônia do pódio.
* **21 kits estilo HG:** escolhidos no baú seletor (ou com `/kit`) antes do início; sem escolha, o jogador fica com o Kangaroo. Cada kit dá só um item de habilidade, que não quebra, não pode ser dropado e tem cooldown — nenhum kit dá armas, ferramentas ou comida.
* **Conquistas automáticas:** 28 conquistas detectadas durante o jogo — quebrar blocos, craftar, fundir, pescar, matar monstros, domar animais, entrar no Nether e no The End, abater um rival. Itens pegos em baús também contam. O primeiro jogador a completar cada conquista ganha cerca de 50% a mais de pontos e é anunciado no chat.
* **Caça aos pontos:** abater um rival rouba 15% dos pontos dele (no mínimo 5 e no máximo 120 por abate, uma vez a cada 60 segundos contra o mesmo jogador). Abater o líder da partida, quando ele tem 50 pontos ou mais, dá +15 de bônus.
* **Bússola Caça-Pontos:** aponta para o rival com mais pontos fora do coliseu e mostra nome, pontos e distância; o clique direito alterna o alvo.
* **Morte justa:** o jogador mantém o inventário e o nível, mas os equipamentos perdem 20% da durabilidade, e 30% dos espaços da mochila (sorteados) vão para um baú "Espólios de <jogador>" no local da morte. A armadura vestida não vai para o baú.
* **Respawn no coliseu:** quem morre renasce no coliseu com 30 segundos sem levar nem causar dano e 15 segundos de Speed II para voltar ao mapa.
* **Altar do End:** um portal do End, sem os olhos, fica no chão a 30 blocos ao norte do coliseu.
* **Pódio e reset:** o Top 3 sobe nos degraus de ouro, prata e bronze, os demais assistem da plateia, e há 15 segundos de fogos. Depois o servidor fecha e, na próxima subida pelo `iniciar.bat`, o mapa é apagado e regerado na seed do projeto.
* **Livro do QuestCodex:** o clique direito abre um menu com as 28 conquistas (objetivo, pontos e quem completou primeiro), o tempo restante e o Top 3.
* **Scoreboard lateral:** tempo ou estado da partida, cargo, kit, pontos, conquistas feitas, líder, ping e jogadores online. `/sb` mostra ou oculta.
* **TAB e tags (cargos):** prefixo colorido no chat, no TAB e acima da cabeça. O servidor vem com DONO, DEV, ADMIN, MOD, VIP e MEMBRO; admins criam novas tags e as liberam para os jogadores.
* **Ping:** `/ping` mostra a latência com cor por faixa, de verde (abaixo de 60 ms) a vermelho (250 ms ou mais).

As conquistas se dividem em faixas de dificuldade. A lista completa, com o
objetivo de cada uma, aparece no Livro do QuestCodex, dentro do jogo.

| Faixa | Conquistas | Pontos | 1º a completar | Exemplos |
|:---:|:---|:---|:---|:---|
| Fácil | 7 | 15–25 | 25–40 | troncos de madeira, bancada, picareta de pedra, pão |
| Média | 7 | 35–50 | 55–75 | barra de ferro, escudo, balde de lava, 8 monstros |
| Difícil | 7 | 65–100 | 100–150 | diamante, obsidiana, entrar no Nether, vara de Blaze |
| Épica | 4 | 110–160 | 165–240 | olho de Ender, detrito ancestral, abater um rival, netherite |
| Lendária | 3 | 170–250 | 250–350 | entrar no The End, crânio de Wither, Ender Dragon |

Os kits, com o item que ativa cada habilidade:

| # | Kit | Item | Habilidade | Cooldown |
|:---:|:---|:---|:---|:---|
| 1 | Kangaroo | Foguete | salto duplo no ar; com Shift, avança para a frente; menos dano de queda | 7 s |
| 2 | Thor | Machado de madeira | raio no bloco mirado (até 25 m), com 5 de dano nos rivais próximos | 18 s |
| 3 | Phantom | Pena | 5 s de voo livre; imune à primeira queda ao pousar | 35 s |
| 4 | Ninja | Estrela do Nether | teleporta para as costas do rival mais próximo (até 25 blocos) e ganha Speed I por 3 s | 16 s |
| 5 | Scout | Açúcar | Speed II por 7 s | 22 s |
| 6 | Stomper | Botas de ferro | é lançado 16 blocos para cima e causa dano em área ao cair (metade em quem está agachado) | 28 s |
| 7 | Fisherman | Vara de pesca | puxa até você o jogador ou a criatura fisgada | 8 s |
| 8 | Grappler | Laço | impulsiona na direção mirada; menos dano de queda por 4 s | 9 s |
| 9 | Monk | Vara de Blaze | clique num rival para trocar o item da mão dele por outro do inventário dele | 18 s |
| 10 | Viper | Olho de aranha | Veneno I por 5 s nos rivais a até 6 blocos; passiva: 25% de chance de envenenar ao bater | 20 s |
| 11 | Snail | Bola de slime | Lentidão II por 5 s nos rivais a até 6 blocos; passiva: 25% de chance de dar lentidão ao bater | 20 s |
| 12 | Flash | Tocha de redstone | teleporta para o bloco mirado (até 22 m) | 24 s |
| 13 | Endermage | Olho de Ender | puxa para o seu nível quem está acima ou abaixo de você (5 blocos de raio); os dois ficam 3 s invulneráveis | 26 s |
| 14 | Poseidon | Coração do mar | Força I, Graça do Golfinho e Respiração Aquática por 6 s, com uma onda que empurra os rivais; passiva: Speed na água ou na chuva | 22 s |
| 15 | Fireman | Creme de magma | anel de fogo que incendeia rivais a até 5 blocos; passiva: imune a fogo e lava | 22 s |
| 16 | Boxer | Tijolo | Resistência I e Força I por 5 s; passiva: soco com dano de espada de pedra e -15% de dano corpo a corpo recebido | 25 s |
| 17 | Lumberjack | Farinha de osso | Pressa II por 8 s; passiva: quebrar um tronco derruba a árvore inteira | 15 s |
| 18 | Miner | Sílex | Pressa II e Visão Noturna por 12 s; passiva: quebrar um minério quebra a veia conectada | 18 s |
| 19 | Ghost | Quartzo | invisível e com Speed II por 6 s | 30 s |
| 20 | Gladiator | Barras de ferro | puxa o rival mais próximo (até 10 m) e ganha Absorção (+2 corações) por 8 s | 32 s |
| 21 | Anchor | Bigorna | tremor que dá Lentidão II ao redor; passiva: sem knockback para você e para quem você ataca | 20 s |

---

## 📁 Estrutura de Pastas

```
QuestCodex/
├── src-tags/                # TagsPlugin: partida, coliseu, kits, conquistas, pontos, tags, scoreboard e TAB
│   └── src/main/
│       ├── java/            # código do plugin
│       └── resources/       # plugin.yml e config.yml padrão
├── src-ping/                # PingPlugin: comando /ping
├── config/                  # configurações globais e de mundo do Paper
├── bukkit.yml               # configurações do servidor (Bukkit)
├── spigot.yml               # configurações do servidor (Spigot)
├── commands.yml             # aliases de comandos do servidor
├── server-icon.png          # ícone da lista de servidores
├── compilar-plugins.ps1     # compila os dois plugins e copia os .jar para plugins/
├── iniciar.bat              # baixa o Paper, prepara e reseta o mundo, compila e inicia o servidor
├── CONTRIBUTING.md          # fluxo de trabalho e convenções
└── LICENSE                  # licença MIT
```

O jar do Paper, o mundo, os logs, os plugins compilados, o `eula.txt` e o
`server.properties` são gerados na primeira execução e ficam fora do
repositório.

---

## 📚 Dependências de Software

* `Paper 26.3` (servidor e API dos plugins)
* `JDK 25` (compila os plugins e roda o servidor)
* `PowerShell` (scripts de build e de início, no Windows)

---

## 🎛️ Uso

Comandos de qualquer jogador:

| Comando | Atalhos | O que faz |
|:---:|:---|:---|
| `/kit` | `/kits` | abre o seletor de kits (só antes do início da partida) |
| `/codex` | `/quests`, `/missoes`, `/conquistas` | abre o menu de conquistas |
| `/codex livro` | — | devolve os itens do QuestCodex que faltarem no inventário |
| `/pontos` | `/ranking`, `/top` | mostra o Top 10 da partida no chat |
| `/tag` | `/tags`, `/cargo` | lista suas tags no chat; clique numa para equipar |
| `/tag <tag>` | — | equipa uma tag liberada |
| `/tag lista` | `/tag list` | lista todas as tags do servidor |
| `/tag help` | `/tag ajuda`, `/tag ?` | mostra a ajuda dos comandos |
| `/sb` | `/scoreboard` | mostra ou oculta a scoreboard lateral |
| `/ping [jogador]` | `/latencia`, `/ms` | mostra o seu ping ou o de outro jogador |

Comandos de admin (OP ou permissão `tags.admin`):

| Comando | Atalhos | O que faz |
|:---:|:---|:---|
| `/partida iniciar [minutos]` | `/partida start` | abre os portões e começa a partida (45 minutos se o tempo não for informado) |
| `/partida finalizar` | `/partida coroar`, `/partida vencedor` | encerra na hora: pódio, fogos, fechamento do servidor e reset do mapa |
| `/partida parar` | `/partida stop`, `/partida encerrar` | pausa a partida sem fechar o servidor, fecha os portões e mostra o ranking |
| `/partida resetar` | `/partida reset` | zera pontos e conquistas, fecha os portões e leva todos de volta ao coliseu |
| `/partida coliseu` | `/partida spawn` | teleporta você para o coliseu |
| `/partida darlivro` | — | entrega os itens do QuestCodex que faltarem a cada jogador online |
| `/partida top` | `/partida ranking` | mostra o ranking no chat |
| `/tag add <jogador> <tag>` | `/tag dar`, `/tag liberar` | libera uma tag para o jogador; `*` libera todas |
| `/tag set <jogador> <tag>` | — | libera e equipa a tag na hora |
| `/tag remover <jogador> [tag]` | `/tag remove`, `/tag del` | com tag, tira a liberação; sem tag, volta o jogador à tag padrão |
| `/tag criar <id> <COR> <nome>` | — | cria uma tag, ex.: `/tag criar builder GREEN BUILDER` |
| `/tag reload` | — | recarrega o `config.yml` do plugin |

As cores de `/tag criar` são os nomes de cor do Minecraft (`RED`, `GOLD`,
`AQUA`, `LIGHT_PURPLE`...); a lista está no topo do `config.yml` do
TagsPlugin. Além de `/tag add`, uma tag pode ser liberada por permissão:
`tags.tag.<id>` libera uma tag, e `tags.all` libera todas.

---

## ⚙️ Configuração e Instalação

Os scripts do projeto são para Windows (`.bat` e PowerShell).

1. **Instale o JDK 25** e confira:
   ```powershell
   winget install --id EclipseAdoptium.Temurin.25.JDK -e
   javac -version
   ```
   Sem o `javac`, o `compilar-plugins.ps1` não compila nada e não avisa.
2. **Clone o repositório:**
   ```powershell
   git clone https://github.com/jhowsbDiem/QuestCodex.git
   cd QuestCodex
   ```
3. **Primeira subida:** rode `.\iniciar.bat`. Ele baixa o Paper 26.3, grava o `server.properties` (seed do projeto, dificuldade Difícil) e o `eula.txt` com `eula=true` — rodar o script é aceitar a [EULA do Minecraft](https://www.minecraft.net/eula). Nessa subida o Paper cria a pasta `libraries/`, e os plugins ainda não são compilados. Quando o console mostrar `Done`, pare com `stop`.
4. **Segunda subida:** rode `.\iniciar.bat` de novo. Agora ele compila o TagsPlugin e o PingPlugin antes de subir, e o console mostra `[TagsPlugin] QuestCodex v0.1.0 ativado!`. O servidor reserva 6 GB de RAM (`-Xms6G -Xmx6G` no `iniciar.bat`); com menos memória livre, diminua os dois valores.
5. **Entre e vire admin:** conecte-se em `localhost` com o Minecraft Java Edition 26.3 e, no console do servidor, rode `op <seu nick>`.
6. **Comece a partida:** no jogo, rode `/partida iniciar`.

Para compilar sem subir o servidor, rode `.\compilar-plugins.ps1`.

---

## 🧠 Arquitetura

* **Conquistas e kits definidos no código:** as 28 conquistas e os 21 kits, com objetivos, pontos e cooldowns, ficam no TagsPlugin; criar ou mudar um deles exige recompilar o plugin.
* **Pontos salvos em arquivo, partida em memória:** pontos, conquistas feitas e quem completou cada uma primeiro ficam em `plugins/TagsPlugin/config.yml` e sobrevivem a um reinício. O cronômetro, os kits escolhidos, os cooldowns e o progresso parcial das conquistas de contagem (10 troncos, 8 monstros) ficam só em memória — reiniciar o servidor no meio de uma partida para o cronômetro.
* **Reset do mapa em duas etapas:** ao fim da partida, o plugin grava `reset-world.flag` e fecha o servidor; o `iniciar.bat` apaga o mundo antes da subida seguinte. Subindo o servidor por outro caminho, o mapa não é resetado.
* **Coliseu construído pelo plugin:** a cada subida, se o coliseu não existe sobre o spawn, o plugin o constrói; a altura e o raio ficam em `coliseu.altura-y` e `coliseu.raio` do `config.yml`.

---

## 🤝 Contribuindo

Fluxo de trabalho, padrão de commit e de comentários estão no [`CONTRIBUTING.md`](CONTRIBUTING.md).

---

## 📊 Estado Atual do Projeto

### ✅ Funcionando
* **Partida completa:** coliseu, início com invencibilidade, cronômetro de 45 minutos, pódio com fogos e reset do mapa.
* **21 kits estilo HG:** seletor antes do início e habilidades com cooldown.
* **28 conquistas automáticas:** em cinco faixas, com bônus e anúncio do 1º lugar.
* **Caça aos pontos:** roubo de pontos por abate, bônus pelo líder e Bússola Caça-Pontos.
* **Morte justa:** baú de espólios e desgaste de equipamentos.
* **Tags, scoreboard e TAB:** cargos coloridos, placar lateral e líder da partida.
* **Ping:** `/ping` com cor por faixa de latência.
* **Build por script:** `compilar-plugins.ps1`, chamado pelo `iniciar.bat`.

---

## ✒️ Autores e Licença

* **Desenvolvedor:** Jonathan dos Santos Bezerra.
* **Colaboradores:** Gabriel Cleto.
* **Licença:** MIT — veja [LICENSE](LICENSE).
