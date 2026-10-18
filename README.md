# ⚔️ QuestCodex — Survival Competitivo & Caça-Pontos (v4.0)

Servidor e plugin customizado para **Minecraft Java Edition 26.3 (PaperMC)** focado em partidas competitivas de sobrevivência de **45 minutos**, combinando corridas de conquistas (**QuestCodex**), **21 Kits de Habilidades estilo Hardcore Games (HG)**, **Bússola Caça-Pontos**, **Espólios em Baú de Morte** e **Cerimônia Épica de Pódio** com reset automático de mapa.

---

## 🎮 Dinâmica da Partida

1. **Lobby no Coliseu Celestial (`Y = 165`):**
   - Ao entrar no servidor, os jogadores nascem no **Coliseu nas Alturas** com os portões trancados.
   - Enquanto a partida não inicia, apenas staffs com tag **`dono`**, **`admin`**, **`mod`** ou **`dev`** (ou no modo Criativo/OP) podem atravessar os portões.
   - Todos recebem no slot 4 o **Baú Seletor de Kits (`🎁 Seletor de Kits`)** para escolher sua habilidade antes do início.
2. **Início da Partida (`/partida iniciar`):**
   - O mundo é ajustado para **Dia (`1000L`)**, dificuldade **Difícil (`HARD`)** e borda centralizada de **350 blocos de raio (`700x700`)**.
   - Os **4 portões do Coliseu se abrem**, o Baú Seletor de Kits desaparece do inventário e cada jogador recebe o **Item Ativador da Habilidade** do seu Kit, além do **Livro QuestCodex** e da **Bússola Caça-Pontos**.
   - Todos os jogadores recebem **2 minutos de Invencibilidade Global** + **Velocidade I (Speed I)** para saltar do Coliseu (queda na piscina central de pouso) e se espalharem pelo mapa.
3. **Morte Justa & Baú de Espólios:**
   - **70% da mochila + 100% das armaduras equipadas e itens essenciais** permanecem com o jogador.
   - Os equipamentos sofrem **20% de desgaste na durabilidade máxima**.
   - Os **30% restantes** da mochila não caem espalhados no chão: um **Baú de Morte (`💀 Espólios de <Jogador>`)** é criado automaticamente no local do abate com efeito sonoro e partículas.
4. **Respawn Protegido no Coliseu:**
   - Ao renascer no Coliseu, o jogador recebe **30 segundos de Invencibilidade** (não recebe dano e **não pode atacar ninguém** nesse período) + **15 segundos de Velocidade II (Speed II)** e um aviso na tela para descer rapidamente de volta para a arena.
5. **Fim de Jogo, Pódio com Fogos e Reset do Mundo:**
   - Quando o cronômetro de **45 minutos** zera (ou via `/partida finalizar`), o PvP é encerrado e um **Pódio Olímpico (Ouro, Prata e Bronze)** é erguido no centro do Coliseu.
   - O **Top 3** é teleportado para os seus respectivos degraus e todos os demais jogadores são posicionados na plateia para assistir a **15 segundos de queima de fogos de artifício**.
   - Após a cerimônia, o servidor grava a flag de reset, encerra automaticamente e o `iniciar.bat` apaga a pasta `world/` para gerar uma seed 100% nova na próxima partida.

---

## ⚡ Os 21 Kits de Habilidades (Estilo HG)

Nenhum kit entrega armas, ferramentas ou comidas grátis. Cada kit fornece **exclusivamente 1 Item Ativador de Habilidade** (indestrutível, não-dropável e fixo na partida) com controle de **Cooldown na ActionBar** e passivas balanceadas:

| # | Kit | Item Ativador | Habilidade Ativa / Passiva | Cooldown |
| :---: | :--- | :--- | :--- | :---: |
| 1 | **Kangaroo** | Foguete (`Firework`) | **Pulo Duplo** no ar (ou *Dash* horizontal ao usar agachado) + redução de dano de queda. | `7s` |
| 2 | **Thor** | Machado de Madeira | Invoca um **Raio** no bloco mirado (até 25m) causando dano em área. *(Não causa dano corpo a corpo)*. | `18s` |
| 3 | **Phantom** | Pena (`Feather`) | Concede **5 segundos de Voo livre** com contagem regressiva + imunidade ao dano de queda ao pousar. | `35s` |
| 4 | **Ninja** | Estrela do Nether | Teleporta **nas costas** do inimigo mais próximo (até 25m) e ganha *Speed I* por 3s. | `16s` |
| 5 | **Scout** | Açúcar (`Sugar`) | Explosão de velocidade com **Speed II por 7 segundos** para rush ou fuga. | `22s` |
| 6 | **Stomper** | Botas de Ferro (Ícone) | Lança você **16 blocos para o alto** e esmaga o chão ao cair, causando dano em área (rivais no *Shift* recebem metade). | `28s` |
| 7 | **Fisherman** | Vara de Pesca | Fisga qualquer jogador ou criatura e **puxa diretamente para a sua posição**. | `8s` |
| 8 | **Grappler** | Laço (`Lead`) | Lança um gancho que **impulsiona seu corpo rapidamente** na direção mirada + proteção de queda. | `9s` |
| 9 | **Monk** | Vara de Blaze | Clique direito em um inimigo para **embaralhar o item da mão principal dele** com um slot aleatório da mochila. | `18s` |
| 10 | **Viper** | Olho de Aranha | **Ativa:** Nuvem tóxica (6m) com Veneno. **Passiva:** 25% de chance de envenenar ao acertar golpes. | `20s` |
| 11 | **Snail** | Bola de Slime | **Ativa:** Onda congelante (6m) com *Slowness II*. **Passiva:** 25% de chance de aplicar lentidão ao bater. | `20s` |
| 12 | **Flash** | Tocha de Redstone | **Teleporte instantâneo** com partículas e som de Enderman para o bloco mirado (até 22m). | `24s` |
| 13 | **Endermage** | Olho do Fim | Puxa jogadores que estejam **acima ou abaixo de você** (coluna de 5 blocos) para o seu andar + 3s de proteção. | `26s` |
| 14 | **Poseidon** | Coração do Mar | **Ativa:** Onda de água que empurra rivais + *Força I* (6s). **Passiva:** *Speed II* e Respiração na água/chuva. | `22s` |
| 15 | **Fireman** | Creme de Magma | **Passiva:** **100% imune a fogo e lava**. **Ativa:** Cria um anel de chamas (5m) incendiando inimigos próximos. | `22s` |
| 16 | **Boxer** | Tijolo (`Brick`) | **Passiva:** Punho causa dano de espada de pedra e recebe -15% de dano físico. **Ativa:** *Força I* + *Resistência I* (5s). | `25s` |
| 17 | **Lumberjack** | Farinha de Osso | **Passiva:** Quebra a árvore inteira de uma só vez (*TreeCapitator*). **Ativa:** *Pressa II* (*Haste II*) por 8s. | `15s` |
| 18 | **Miner** | Sílex (`Flint`) | **Passiva:** Quebra até 4 minérios conectados (*VeinMiner*). **Ativa:** *Pressa II* + *Visão Noturna* por 12s. | `18s` |
| 19 | **Ghost** | Quartzo | Fica **100% Invisível + Speed II** por 6 segundos para emboscadas ou fugas táticas. | `30s` |
| 20 | **Gladiator** | Barras de Ferro | Puxa o rival mais próximo (até 10m) para a sua frente em duelo e ganha **Corações de Absorção** por 8s. | `32s` |
| 21 | **Anchor** | Bigorna (`Anvil`) | **Passiva:** **Zero Knockback** (não sofre nem aplica repulsão). **Ativa:** Impacto sísmico com *Slowness II* em área. | `20s` |

---

## 🏆 Sistema de Pontos (`QuestCodex`) & Caça-Pontos

### 📖 Livro de Missões (`/codex`)
- São **28 conquistas** divididas em 4 níveis de dificuldade:
  - **Fáceis (`15 a 25 pts`):** Madeira, Bancada, Picareta, Fornalha, Ferro, Escudo, Comida Assada, Tochas, Cama, Carvão.
  - **Médias (`35 a 50 pts`):** Ouro, Armadura de Ferro, Arco, Balde de Lava, Redstone, Lápis-Lazúli, Agricultura, Caçador de Monstros, Pescaria.
  - **Difíceis (`65 a 100 pts`):** Diamante, Picareta de Diamante, Obsidiana, Portal do Nether, Mesa de Encantamentos, Alquimia, **Caçador Implacável (Abate PvP)**.
  - **Épicas / Lendárias (`110 a 250 pts`):** Fortaleza do Nether, Vara de Blaze, Olho do Fim, Armadura de Diamante, Crânio/Estrela de Wither, Maçã Dourada, Totem da Imortalidade, Matar o Dragão (`250 pts`).
- **🔥 Bônus First Blood (`+50%`):** O primeiro jogador da partida a concluir qualquer conquista recebe **+50% de pontos extras** e um anúncio global no chat.
- **🔍 Varredura Automática de Inventário:** Itens coletados diretamente de baús de estruturas (Vilas, Portais em Ruínas, Bastiões) ou de Baús de Morte contam automaticamente para as missões.

### 🧭 Bússola Caça-Pontos & Roubo PvP
- **Bússola Rastreadora:** Clique com o botão direito na `🧭 Bússola Caça-Pontos` para rastrear automaticamente o **Líder de Pontos (Top 1)** ou o rival mais próximo no mapa, exibindo nome, pontuação e distância em blocos.
- **Roubo de Pontos em Combate:** Abater um rival transfere **15% dos pontos da vítima** para o assassino (mínimo de `5 pts` se a vítima tiver pontos, teto máximo de `120 pts` por abate e *cooldown anti-farm* de `60s` contra o mesmo jogador).
- **👑 Recompensa pelo Líder (`+15 pts`):** Abater o jogador que está em **#1 no Ranking** concede um bônus extra imediato de **+15 pontos**!

---

## 🛠️ Comandos Principais

| Comando | Permissão | Descrição |
| :--- | :--- | :--- |
| `/codex` ou `/missoes` | Todos | Abre o menu gráfico interativo com as 28 conquistas da partida. |
| `/kit [nome]` | Todos | Abre o seletor de Kits (durante o pré-jogo no Coliseu). |
| `/bussola` | Todos | Atualiza o rastreamento da Bússola Caça-Pontos para o alvo mais valioso. |
| `/pontos [nick]` | Todos | Consulta sua pontuação atual ou a de outro jogador. |
| `/ranking` ou `/top` | Todos | Exibe o Top 10 da partida no chat. |
| `/partida iniciar` | OP / Staff | Abre os portões do Coliseu, ativa os 2 min de invencibilidade e inicia os 45 min. |
| `/partida finalizar` | OP / Staff | Encerra a partida imediatamente e inicia a Cerimônia de Pódio + Reset. |
| `/partida coliseu` | OP / Staff | Reconstrói a estrutura do Coliseu Celestial e teleporta para o centro. |
| `/partida tempo <min>` | OP / Staff | Ajusta o tempo restante da partida em andamento. |
| `/partida resetpontos` | OP / Staff | Zera todas as pontuações e conquistas da partida atual. |
| `/settag <nick> <tag>` | `tags.admin` | Define o cargo/tag de um jogador (`dono`, `admin`, `mod`, `dev`, `vip`, `membro`). |
| `/tags` | Todos | Lista todas as tags disponíveis no servidor. |

---

## 🚀 Como Clonar, Compilar e Rodar (Guia para Desenvolvedores)

### Pré-requisitos
- **Windows** com PowerShell
- **Java JDK 25** (ex.: [Eclipse Adoptium JDK 25](https://adoptium.net/))
- **PaperMC 26.3** (`paper-26.3-205.jar` na raiz do projeto)

### Passo a Passo
1. **Clonar o repositório e acessar a branch `epica`:**
   ```powershell
   git clone git@github.com:jhowsbDiem/QuestCodex.git
   cd QuestCodex
   git checkout epica
   ```
2. **Compilar o plugin (`TagsPlugin.jar`):**
   ```powershell
   .\compilar-plugins.ps1
   ```
   *(O script compila o código-fonte em `src-tags/` e gera o `.jar` em `plugins/TagsPlugin.jar` e `plugins/update/TagsPlugin.jar`).*
3. **Iniciar o servidor:**
   ```powershell
   .\iniciar.bat
   ```
   *(Na primeira execução, certifique-se de ter o `paper-26.3-205.jar` na raiz e `eula=true` no `eula.txt`).*
