# Configuração de permissões

O login/cadastro e a tela de configurações agora compartilham `PermissionSetup`.
Antes, o login apresentava cinco permissões em uma lista extensa, a acessibilidade
ficava sempre pendente no login e sempre autorizada nas configurações, e esta
última tela não verificava novamente os acessos ao retornar do Android. O guia de
configurações restritas estava atrás de um diálogo sem ação de abertura.

## Fluxo atual

- Cabeçalho do cadastro com a etapa atual, progresso e botão Voltar; o botão de
  continuar informa corretamente que a próxima etapa é escolher o nível de bloqueio.
- Contagem das autorizações concedidas e dos três acessos essenciais; atalho para
  configurar a próxima permissão pendente. Os demais itens podem ser expandidos
  individualmente para ler orientações e abrir as configurações do Android.
- Leitura inicial e ao retornar à tela (`ON_RESUME`), executada fora da thread da
  interface. Não há consultas periódicas nem um novo serviço em segundo plano.
- Abrir uma configuração não marca a permissão como concedida. Se a consulta falhar,
  o item exibe que não foi possível verificar; se a navegação falhar, há orientação
  para acessar as configurações manualmente.
- Permissões essenciais pendentes não impedem o usuário de continuar: uma confirmação
  explica as funções limitadas. Recursos adicionais são opcionais.
- O guia de configurações restritas fica visível no primeiro acesso quando aplicável
  e a acessibilidade está desativada. Ele abre as informações do Lock In e oferece
  um caminho de volta à ativação da acessibilidade.
- Instruções específicas do fabricante ficam em “Ajuda para o meu aparelho”.

| Acesso | Uso | Como é verificado |
| --- | --- | --- |
| Acessibilidade | Bloqueios por app e leitura de elementos de Reels/Shorts/instalações | Serviço exato do Lock In na lista habilitada, com acessibilidade ativada |
| Dados de uso | Tempo de uso dos aplicativos | AppOps de `PACKAGE_USAGE_STATS` |
| Sobreposição | Cronômetro e avisos sobre outros apps | `Settings.canDrawOverlays` |
| Notificações | Lembretes e status de monitoramento | Permissão de runtime no Android 13+ e notificações habilitadas |
| Bateria | Continuidade do monitoramento em segundo plano | Exceção de otimização informada pelo PowerManager |
| Administrador | Etapa adicional para desinstalação | DevicePolicyManager |
| Tela de bloqueio | Avisos em tela cheia | NotificationManager no Android 14+; item omitido nas versões anteriores |

Notificações e bateria já estavam declaradas no manifesto e passam a ter ações
explícitas nesse fluxo. VPN continua sendo solicitada pelo recurso de proteção de
rede, e alarmes exatos continuam usando o tratamento existente. Não foi adicionada
uma permissão genérica para alterar configurações ou executar scripts.

O botão de bateria abre a lista padrão de otimização do Android. O guia orienta
a selecionar Todos os apps → Lock In → Não otimizar; os nomes podem variar conforme
o fabricante. O estado só é atualizado após consultar novamente o PowerManager.

“Permitir configurações restritas” é uma ação do próprio Android. O aplicativo
não pode concedê-la automaticamente nem consultar seu estado de forma universal.
Por isso ela é apresentada como orientação, não como uma autorização contada no
progresso. O menu pode não aparecer conforme versão do Android, fabricante e origem
da instalação; nesse caso o usuário pode tentar ativar a acessibilidade diretamente.

## Continuidade do login

Restaurar um backup pode atualizar `isRegistered` antes do fim do cadastro. O
MainActivity mantém o fluxo inicial aberto até a conclusão explícita, evitando
pular as permissões nesse caso. Um ViewModel mantém etapa e dados de autenticação
em memória durante recriações da Activity; a senha não é escrita em um Bundle de
estado salvo e é removida do ViewModel ao concluir o cadastro.

## Validação

Os testes cobrem detecção do serviço correto, notificações negadas, alternativas
quando uma tela do Android não existe, atualização ao retornar, ausência de
polling, configurações restritas, continuação com funções limitadas, restauração
de backup e recriação da Activity. Também verificam a navegação numa tela de
320 × 640 dp, com fonte ampliada e tema escuro. Capturas de interface ficam em
`app/build/outputs/permission-setup*.png` e `onboarding-permissions.png` quando os
testes de gravação do Roborazzi são executados.

Os testes usam Android simulado pelo Robolectric. As telas de configurações de
fabricantes e os diálogos reais de consentimento ainda dependem de validação em
aparelhos físicos; cada autorização continua sendo uma decisão do usuário no Android.

### Resultado final — 3 de novembro de 2026

Comando executado com o ambiente local e a assinatura de desenvolvimento:

```sh
source /workspace/.setup/activate.sh
gradle --no-daemon --continue \
  -I /workspace/.setup/optimization/local-signing.gradle \
  :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- Compilação concluída em 4 min 54 s; APKs debug e release gerados.
- 79 testes executados, sem falhas, erros ou testes ignorados. Destes, 13 cobrem
  o novo fluxo de permissões e a continuidade do cadastro, incluindo o atalho
  de bateria para a lista de otimização do sistema.
- Android Lint: zero erros, 249 avisos e uma sugestão no projeto. Nenhum diagnóstico
  nos arquivos do novo fluxo de permissões e cadastro. Os avisos restantes não
  equivalem a uma certificação de prontidão para a Play Store.
- Assinatura do APK validada com `apksigner`; mesmo certificado de desenvolvimento
  do APK anterior de proteção do teclado. A configuração de assinatura de produção
  do repositório foi preservada.
- APK release: 1.993.082 bytes; exportado para
  `/workspace/artifacts/Lock-In-permissoes.apk`.
- SHA-256: `679580ac9404eb91806473561513705c0a3e4454e4ffab42d11f065c064be20f`.

O log completo desta execução está em
`/workspace/.setup/permissions/final-settings-navigation.log`; resultados individuais
em `app/build/test-results/testDebugUnitTest` e relatório de análise estática em
`app/build/reports/lint-results-debug.html`.
