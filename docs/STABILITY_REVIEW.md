# Revisão de estabilidade e ANRs

## Escopo e evidência

Revisão motivada por ANRs recorrentes durante monitoramento prolongado em segundo plano. A análise estática percorre o módulo Android; a revisão manual concentrou-se em Application, acessibilidade, VPN, ilha flutuante, timers, banco, preferências, carregamento de aplicativos, estatísticas e widgets. Isso não comprova ausência de todos os bugs nem substitui testes prolongados em aparelhos.

## Correções

- Restaura o tratamento padrão do Android para exceções não capturadas. Engolir a exceção da thread principal não recupera seu Looper.
- Esconder uma ilha inexistente deixa de iniciar um foreground service. Chamadas frequentes de monitoramento agora atualizam a instância existente diretamente.
- Cancela todas as coroutines da ilha no encerramento, incluindo a demonstração.
- Acessibilidade: orçamento compartilhado por evento de 120 consultas a filhos e 60 ms entre consultas. Conta também nós vazios e filhos nulos; a reciclagem permanece em finally. Esse limite não interrompe uma chamada Binder individual já bloqueada. Árvores muito grandes podem precisar de eventos posteriores para serem inspecionadas.
- A varredura completa de nomes dos aplicativos instalados passa a IO, com cache de até 60 segundos. A consulta deixa de retornar outro aplicativo apenas porque ele também está na lista de bloqueio. Aplicativos recém-instalados com nomes personalizados podem levar até uma atualização do cache para aparecer nessa consulta.
- VPN: estabelecimento e enumeração de pacotes fora da thread principal, fila de até 64 pacotes e quatro trabalhadores DNS. Leitura vazia aguarda antes de repetir; EOF/erro fatal encerra o túnel. Cancelamento fecha o descritor e encerra o escopo do serviço.
- Validação de limites de cabeçalhos IPv4/UDP/DNS, rejeição de fragmentos e nomes comprimidos inválidos. Resposta NXDOMAIN para bloqueios evita retornar registros A a consultas AAAA. Sockets upstream conectados ao resolvedor e verificação do resultado de protect().
- Declaração do tipo de foreground service da VPN para versões recentes do Android.
- O timer salva uma cópia da sessão encerrada, em vez de ler campos já limpos. Reativar tempo de tela reinicia seu monitoramento; limites desativados deixam de manter um ticker ocioso.
- Banco Room verifica novamente a instância dentro do lock, evitando duas instâncias em inicialização concorrente.
- Preferências de grupos usam gravação assíncrona. Atualizações frequentes do contador deixam de disparar novas consultas completas de estatísticas.
- Resultados de estatísticas são aplicados após retornar à coroutine da tela: consultas canceladas não sobrescrevem a seleção atual.
- Cache de ícones limitado por bytes (8 MiB), chave por pacote/tamanho e rasterização direta na dimensão final. Trocar o pacote no mesmo composable recarrega o ícone.
- Seleção de aplicativos apresenta carregamento, erro com tentativa novamente e lista vazia. Consulta inicial de backup local sai da thread principal.
- Protege requestDismissKeyguard por API 26; remove tentativa não permitida de matar processos de terceiros; substitui View por ImageView nos RemoteViews dos widgets.

## Como validar neste ambiente

```bash
source /workspace/.setup/activate.sh
cd /workspace/Lock-In
gradle --no-daemon --continue :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Executar os testes outra vez sem reutilizar o resultado da tarefa:
gradle --no-daemon :app:testDebugUnitTest --rerun
```

Artefatos: `app/build/outputs/apk/debug/app-debug.apk`, `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html`.

Regressões adicionadas: tráfego DNS válido/truncado/malformado (10 mil entradas), orçamento de varredura, árvore com mil filhos, 10 mil pedidos para esconder a ilha, cancelamento de tarefas, persistência do timer após limpar sessão, reativação do tempo de tela, correspondência de pacote instalado, tamanho do bitmap, estado vazio Compose e abertura/recriação da MainActivity.

## Resultados executados em 1 de novembro de 2026

- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: BUILD SUCCESSFUL; 54 testes, zero falhas, erros ou ignorados. A suíte inicial tinha 39 testes; foram adicionados 15.
- `:app:testDebugUnitTest --rerun`: BUILD SUCCESSFUL; os mesmos 54 testes executados novamente, zero falhas, erros ou ignorados. Os timestamps dos XMLs confirmam execuções diferentes (21:25–21:26 UTC e 21:27–21:28 UTC).
- Lint: zero erros, 255 avisos e uma sugestão. Os 11 erros encontrados na primeira análise foram corrigidos sem baseline ou supressão. Avisos ainda incluem localização, APIs antigas, estilo, dependências e acessibilidade visual.
- Assinatura do APK de debug verificada com apksigner. `git diff --check` passou.
- Houve falhas intermediárias nos testes novos (sincronização/fixture) e no lint; foram investigadas e corrigidas, sem desabilitar testes.

## Limites e validação para distribuição

- Robolectric executa código Android/Compose na JVM; não é um aparelho físico. Não foi realizado ensaio de horas com VPN/acessibilidade em firmware Samsung, Xiaomi ou outros. O ambiente não possui `/dev/kvm` nem aparelho conectado.
- Antes de distribuir, medir uma sessão prolongada com VPN ligada/desligada, ilha ligada/desligada, tela apagada/desbloqueada, troca de rede e muitas trocas de aplicativos. Comparar ANRs, CPU, memória e bateria antes/depois. Capturar o stack trace do ANR com `adb bugreport` ou Android vitals; esses arquivos podem conter dados pessoais e devem ser tratados com cuidado.
- Não há medição de ganho percentual de velocidade nem garantia de que os ANRs relatados desapareceram. Os problemas corrigidos são observáveis no código e cobertos pelos testes descritos.
- A função chamada CloudSyncManager usa arquivos locais/internos e externos do próprio aplicativo. Isso não implementa backup remoto autenticado. A troca de mensagens de grupos usa um serviço externo e precisa de uma revisão própria de autenticação e autorização antes de divulgação como recurso seguro.
- A migração Room ainda usa fallbackToDestructiveMigration: é necessário um plano de migrações preservando dados antes de mudanças futuras no schema.
- Aprovação Play Store, formulários de uso de acessibilidade/VPN, política de privacidade, assinatura de release e testes em aparelhos continuam sendo etapas de lançamento; este patch não publica o aplicativo.
