# Confirmação de desinstalação e verificação do bloqueio

O interceptador de acessibilidade enviava GLOBAL_ACTION_HOME antes de iniciar a confirmação, e a Activity compartilhava a tarefa principal do Lock In. Isso permitia que a navegação para Home disputasse a exibição da Activity, compatível com o relato de a confirmação aparecer somente ao abrir o app. A confirmação agora usa tarefa própria, recebe o contexto do serviço de acessibilidade e não dispara HOME antes da abertura. Intents novos com outro modo atualizam a confirmação.

A tela do Android com “desativar e desinstalar” agora solicita o fluxo completo de remoção, em vez de apenas desativar o administrador. Após o desafio, o app aguarda até três segundos pela desativação do administrador, sem bloquear a thread principal, e solicita ACTION_UNINSTALL_PACKAGE para seu próprio pacote, com resultado de retorno. A Activity permanece até o retorno. Cancelamento ou erro encerra a autorização. O bypass antigo permanente foi substituído por autorização com duração máxima de cinco minutos, preservada apenas para concluir a tentativa; valores antigos não liberam a proteção.

A confirmação final do Android permanece obrigatória. Um aplicativo comum não pode desinstalar a si mesmo silenciosamente. A remoção é realizada pelo sistema; este fluxo não apaga backups em nuvem. Se o usuário cancela depois de desativar o administrador, o Android exige concessão explícita para reativá-lo.

O conteúdo da tela de limite esgotado não foi alterado. Os testes verificam que o enforcer envia o Intent para BlockedActivity com o pacote correto e a indicação de limite esgotado; um teste Compose separado verifica a mensagem visível e a ação existente de desbloqueio.

Limites: Robolectric simula Android e Compose; não demonstra o comportamento de abertura em segundo plano de todos os fabricantes. Acessibilidade precisa estar ativa e, em dispositivos que exigem isso, a permissão de janelas em segundo plano precisa estar concedida. Nenhuma confirmação do sistema é acionada automaticamente pelo serviço de acessibilidade.

## Resultados executados

Suíte completa: **122 testes, zero falhas, zero erros, zero ignorados**. Inclui seis testes novos para detecção do alvo correto, ausência da navegação HOME antes do desafio, tarefa própria, expiração da autorização, Intent de remoção e acionamento/renderização da tela existente de limite esgotado. Os testes de repetição de senha e frase dos níveis de Lock também passaram.

Logs: `/workspace/.setup/uninstall/full-tests.log` e `/workspace/.setup/uninstall/targeted.log`. Não houve desinstalação nem teste em celular físico nesta execução.

Build `assembleRelease` e `lintDebug`: concluídos com sucesso. APK otimizado em `/workspace/artifacts/Lock-In-desinstalacao.apk`, com assinatura de desenvolvimento verificada por `apksigner`. Log de build: `/workspace/.setup/uninstall/build.log`.
