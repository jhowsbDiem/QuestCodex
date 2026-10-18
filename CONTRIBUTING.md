# Contribuindo com o QuestCodex

Este guia cobre como preparar o ambiente, o caminho de uma mudança até a
`main` e as convenções de commit e de comentário. A visão geral do jogo, os
comandos e como subir o servidor estão no [README](README.md).

---

## ⚙️ Preparando o Ambiente

* **JDK 25:** o build usa o `javac`, então só o Java de execução não basta. No Windows: `winget install --id EclipseAdoptium.Temurin.25.JDK -e`; confira com `javac -version`.
* **Compilar sem subir o servidor:** `.\compilar-plugins.ps1` compila o TagsPlugin e o PingPlugin e copia os `.jar` para `plugins/` e `plugins/update/`. Ele precisa da pasta `libraries/`, criada pela primeira subida do servidor.
* **Testar:** não há testes automatizados — toda mudança é verificada no servidor local (`.\iniciar.bat`, entrando em `localhost`).

---

## 🔀 Fluxo de Trabalho

1. Toda mudança começa por uma issue.
2. Uma funcionalidade maior ganha uma branch própria a partir da `main`: `feature/<nome>`.
3. Cada issue dela ganha uma branch a partir da branch da funcionalidade: `feature/<N>-<assunto>` (ou `fix/`, `docs/`).
4. Um commit por issue, feito depois que a mudança foi verificada no servidor.
5. A branch da issue entra na da funcionalidade por fast-forward; a da funcionalidade entra na `main` por pull request. A `main` não aceita push direto nem force push.

```
main ─────────────────────────────●──── (pull request)
  └─ feature/<nome> ──●──●──●────┘
         └─ feature/<N>-<assunto> (um commit cada)
```

---

## 📝 Padrão de Commit

[Conventional Commits](https://www.conventionalcommits.org/pt-br/), em português:

```
<tipo>: <descrição no imperativo, sem ponto final>

Closes #<issue>
```

* **Tipos:** `feat`, `fix`, `refactor`, `docs`, `perf`, `test`, `style`, `chore`, sem escopo entre parênteses.
* **Corpo:** só quando o motivo não é óbvio pela issue.

Exemplo:

```
feat: adicionar kit Ninja ao seletor de kits

Closes #12
```

---

## 💬 Padrão de Comentários

* Um comentário explica **por quê**, nunca o quê — o código já diz o quê.
* Sem referência a issues, tarefas ou conversas: isso fica no commit.
* Sem código comentado: o histórico fica no git.

```java
// Ruim: repete o código
// Percorre o inventário do jogador
private void scanPlayerInventoryForQuests(Player player) {

// Bom: diz o que o código não diz
// Itens tirados de baús de estruturas não passam pela coleta no chão e também precisam contar.
private void scanPlayerInventoryForQuests(Player player) {
```

---

## 📚 Onde Fica Cada Documento

* **[README.md](README.md):** visão geral, funcionalidades, comandos, instalação e estado do projeto.
* **CONTRIBUTING.md:** este guia.
* **[LICENSE](LICENSE):** licença MIT.
