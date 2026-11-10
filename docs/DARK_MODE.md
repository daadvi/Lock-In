# Correção do modo escuro

As telas usavam o tema escuro no fundo principal, mas vários elementos internos
continuavam usando cores claras fixas (`CmfWhite`, `CmfLightGray` e valores RGB).
Os textos também mantinham cores escuras, causando baixo contraste em cartões
que já acompanhavam o tema.

## Alterações

- **Focus Time:** aviso de bloqueio, duração, presets, ajustes de tempo, nota sobre
  telefone, sugestões e lista de apps liberados usam superfícies, bordas e textos
  do tema. Os controles de rotinas, seletor de apps e diálogos relacionados também
  foram revisados.
- **Estatísticas:** gráfico, filtros Antes/Depois, navegação de datas, períodos,
  centro e trilha do indicador circular, resumos, lista de apps e detalhes de
  sequência respeitam as cores do tema.
- **Amigos:** o botão de entrar no grupo passa a usar uma superfície escura com
  texto claro. O título das instruções tem contraste maior no modo escuro.
- **Componentes compartilhados:** títulos de `NothingCard` e textos auxiliares do
  seletor de horário usam `onSurfaceVariant` para acompanhar a superfície.

As cores vêm do `MaterialTheme.colorScheme` existente. Texto branco sobre botões
coloridos, ícones e cores próprias dos aplicativos continuam sendo usados conforme
sua função. As regras de bloqueio, permissões e dados do aplicativo não mudaram.

## Validação visual

`ThemeScreensUiTest` abre as três telas nos modos claro e escuro, com apps e dados
de uso simulados. Ele verifica a renderização dos painéis, a navegação até o
seletor de horário e a preservação do código digitado ao alternar o tema.

Os seis casos passaram na execução específica. As capturas foram inspecionadas
visualmente em `app/build/outputs/theme`. O teste também analisa os pixels dos
painéis: regiões que antes eram brancas devem conter menos de 16% de pixels claros
no modo escuro, permitindo textos e ícones sem aceitar grandes fundos claros.
As regiões verificadas ficaram entre 0,2% e 2,1% de pixels claros nessa execução.

As capturas usam o renderizador nativo do Robolectric/Roborazzi. São imagens da
interface Android executada em teste; os apps e os dados exibidos são exemplos.
Não houve validação em aparelho físico.

Para repetir a verificação das telas:

```sh
source /workspace/.setup/activate.sh
gradle --no-daemon :app:testDebugUnitTest --tests '*ThemeScreensUiTest*'
```

Os testes gravam imagens novas explicitamente antes de analisar os pixels, para
não reutilizar uma captura de execução anterior. As imagens geradas ficam nos
arquivos `light-*.png`, `dark-*.png` e na pasta `panels` de
`app/build/outputs/theme`.

### Ambiente de testes

A primeira execução conjunta com a compilação aprovou 102 de 106 testes e teve
quatro timeouts de sincronização do Compose. Esses quatro passaram na repetição
sem alterações no código do aplicativo. Nessa repetição, 105 testes passaram e o
teste legado de captura no SDK 36 falhou ao inicializar o renderizador nativo
(`FileSystemAlreadyExistsException` ao carregar fontes), antes de abrir o app.

A validação final usa processos separados por classe de teste para evitar que os
renderizadores dos SDKs 34 e 36 compartilhem recursos nativos. A configuração fica
fora do aplicativo, em `/workspace/.setup/dark-theme/isolated-tests.gradle`:

```groovy
allprojects {
    tasks.withType(org.gradle.api.tasks.testing.Test).configureEach {
        forkEvery = 1
        maxParallelForks = 1
    }
}
```

Comando da suíte isolada:

```sh
source /workspace/.setup/activate.sh
gradle --no-daemon --no-parallel --max-workers=2 \
  -I /workspace/.setup/dark-theme/isolated-tests.gradle \
  :app:testDebugUnitTest -Proborazzi.test.record=true
```

### Resultado final — 3 de novembro de 2026

- **106 testes aprovados**, sem falhas, erros ou testes ignorados, na execução
  isolada de 2 min 57 s. Inclui os seis casos dos temas e o teste legado no SDK 36.
- APKs debug e release gerados. Android Lint: zero erros, 249 avisos e uma sugestão,
  mesma contagem anterior às alterações.
- APK release de 1.993.082 bytes, com assinatura verificada pelo `apksigner` e o
  mesmo certificado de desenvolvimento dos APKs anteriores.
- Arquivo para instalar: `/workspace/artifacts/Lock-In-modo-escuro.apk`.
- SHA-256: `5bea0c7d6e6a6b6db7ad095853dc84c199be18c19415ba37d09b24a47cf4df7f`.
- Prévias exportadas: `/workspace/artifacts/Lock-In-modo-escuro-foco.png`,
  `/workspace/artifacts/Lock-In-modo-escuro-estatisticas.png` e
  `/workspace/artifacts/Lock-In-modo-escuro-amigos.png`.

O log da validação final está em
`/workspace/.setup/dark-theme/isolated-verification.log`; os resultados individuais
estão em `app/build/test-results/testDebugUnitTest`. O log de compilação e análise
estática, que também registra os timeouts da primeira execução conjunta, está em
`/workspace/.setup/dark-theme/final.log`.
