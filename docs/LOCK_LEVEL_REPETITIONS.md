# Repetições de senha por nível de Lock

A regra confirmada é nível 1 (BASIC): **1** repetição; nível 2 (DETERMINED):
**3** repetições; nível 3 (DEV_DREAM): **5** repetições. O modelo `LockLevel`
e os identificadores persistidos continuam com esses valores; não é necessário
refazer o cadastro nem migrar as preferências.

## Causa e correção

O desbloqueio comum de apps, a remoção de apps da lista de instalação e a redução
do nível já consultavam `LockLevel.passwordRepeatCount`. Outros caminhos exigiam
cinco senhas independentemente da escolha:

- Desbloqueio de emergência do dispositivo (`EmergencyUnlockScreen`).
- Desinstalação e desativação do administrador (`HighFrictionSecurityChallengeFlow`).
- Desativação do bloqueio absoluto de instalações (`AbsoluteInstallBlockDialog`).

Esses caminhos passam a consultar o mesmo valor do nível salvo. As condições de
avanço, os indicadores de progresso, os botões e as instruções exibem a mesma
contagem. O cadastro explica que a escolha também vale para desbloquear o
dispositivo, desinstalar o app e desativar as proteções. O aviso de desbloqueio
do painel também apresenta a contagem e o limite diário do nível escolhido.

Uma senha incorreta continua zerando as repetições consecutivas. A verificação
criptográfica das senhas e a tolerância a pequenos erros nas frases não foram
alteradas. As etapas adicionais de cada desafio continuam sendo exigidas.

## Verificação

`LockLevelUnlockUiTest` executa a mesma matriz para os três níveis, usando campos
Compose, digitação caractere por caractere e submissão pelo teclado ou botão.
Verifica a persistência do nível, desbloqueio comum e de emergência, remoção de
bloqueio de instalação, desinstalação, desativação do administrador e bloqueio
absoluto de instalações. Confere que o avanço ocorre na repetição correta e que
uma senha incorreta após progresso parcial obriga a repetir toda a sequência.

Também confere que as frases permanecem obrigatórias, que erros nas frases do
desafio de desinstalação/administrador reiniciam o processo e que a tolerância
a pequenos erros de digitação das frases de desbloqueio continua funcionando.

Os testes são executados em Android simulado pelo Robolectric; não substituem
a validação dos fluxos do sistema em aparelhos físicos.

### Comparação com as contagens anteriores — 3 de novembro de 2026

Com as regras anteriores de contagem temporariamente restauradas nos três
componentes responsáveis, os 21 testes da matriz produziram oito falhas:
emergência, desinstalação, administrador e bloqueio absoluto nos níveis 1 e 2.
Os sete casos do nível 3 passaram. Persistência, desbloqueio comum de apps e
remoção de um app da lista de instalação passaram nos três níveis.

Essa comparação preservou os valores 1/3/5 de `LockLevel`. Os resultados estão
em `/workspace/.setup/lock-level/original-counts.xml` e o log correspondente em
`/workspace/.setup/lock-level/original-counts.log`.

### Resultado com a correção

| Nível | Repetições | Casos aprovados antes | Casos aprovados depois |
| --- | --- | --- | --- |
| 1 — Básico | 1 | 3 de 7 | 7 de 7 |
| 2 — Determinado | 3 | 3 de 7 | 7 de 7 |
| 3 — Sonho do Dev | 5 | 7 de 7 | 7 de 7 |

Comando da validação final:

```sh
source /workspace/.setup/activate.sh
gradle --no-daemon --continue \
  -I /workspace/.setup/optimization/local-signing.gradle \
  :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- Compilação concluída em 5 min 17 s, com APKs debug e release gerados.
- **100 testes passaram**, incluindo os 21 da matriz dos níveis; zero falhas,
  erros ou testes ignorados. A suíte inclui as verificações anteriores de entrada
  manual, tolerância de digitação, permissões, ciclo de vida e serviços.
- Android Lint: zero erros, 249 avisos e uma sugestão, mesma contagem anterior.
- Assinatura do APK validada com `apksigner`: mesmo certificado de desenvolvimento
  do APK anterior; a configuração de assinatura de produção continua preservada.
- APK release: 1.993.082 bytes, exportado para
  `/workspace/artifacts/Lock-In-niveis-corrigidos.apk`.
- SHA-256: `cd4f93cd735bc029b12e228410e827b7c41a132f34749d270d16e29f3b17b2fb`.

Log final: `/workspace/.setup/lock-level/final.log`. Resultados individuais:
`app/build/test-results/testDebugUnitTest`. Análise estática:
`app/build/reports/lint-results-debug.html`.
