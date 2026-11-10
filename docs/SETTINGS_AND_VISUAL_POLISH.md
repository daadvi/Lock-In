# Configurações e acabamento visual

## Escopo

Mantém a organização das telas, a tipografia geométrica e os ícones pixelados, com preto/cinza e laranja. Cartões compartilhados ganharam bordas e títulos mais legíveis; fundos usam a paleta do tema. A navegação tem seleção laranja discreta, sem expansão lateral dos ícones. As telas de bloqueio e proteção contra desinstalação passam a acompanhar o tema salvo.

Os três desenhos no topo das configurações agora são atalhos identificados: alternar tema, ativar/desativar vibração e ativar/desativar lembrete. O seletor de tema mostra sempre os rótulos Claro/Sistema/Escuro. Os interruptores são controles acessíveis únicos, acionáveis em toda a linha.

A vibração dos cartões, controles de foco, navegação e ilha respeita a preferência do app e do Android. Desligar a opção não provoca vibração extra. A opção não controla notificações de outros apps nem as configurações de vibração do teclado.

O lembrete de foco usa um único alarme inexato por volta das 20h locais. O Android pode atrasá-lo para economizar bateria. Ele é reagendado após reinício, atualização e mudança de horário/fuso; desativar cancela o alarme e a notificação. Não é mostrado durante Focus Time nem sem acesso às notificações. A tela oferece acesso à central de permissões quando esse acesso está bloqueado. O texto descreve uma revisão diária de sequência e metas; não afirma detectar um minuto de sessão, pois o histórico existente não mede esse evento.

O cartão Info abre os créditos existentes. Não foi criado um plano Premium: essa função aparece na imagem de referência, mas não existe no aplicativo.

Esta alteração trata dos controles mostrados na referência. As opções adicionais de alerta pré-limite, pausa consciente e modo estrito ainda precisam de implementação própria; não estão incluídas na validação funcional descrita aqui.

## Validação

Testes JVM com Robolectric/Compose verificam cliques, estados selecionados, preferências persistidas, abertura dos créditos, recuperação de permissão, navegação, agendamento/cancelamento e notificação, bloqueio durante foco e controle de vibração. Imagens são renderizadas por Roborazzi. Resultados finais são registrados após executar a suíte e o lint.

Não houve teste em celular físico. A vibração e a entrega efetiva de alarmes sob as restrições de bateria de fabricantes precisam de validação no aparelho. O APK de teste usa a mesma chave de desenvolvimento dos artefatos anteriores; não é uma versão publicada na Play Store.

Resultado executado: 10 testes novos passaram na rodada direcionada. Na primeira suíte completa, o teste de ciclo de vida contava o novo observador de registro do agendador como se fosse um observador da tela. O teste passou a observar o tema (fluxo exclusivamente da interface), preservando a exigência de zero assinantes em segundo plano. A repetição completa passou: **116 testes, zero falhas, zero erros, zero ignorados**. Log: `/workspace/.setup/settings/full-tests-final.log`.

As imagens de configurações claras e escuras foram inspecionadas em resolução de 360 dp. Os testes de tema também renderizam Focus Time, estatísticas e amigos. A suíte não comprova ausência de todos os bugs nem substitui teste de longa duração em celulares.

Build final: `assembleRelease`, `assembleDebug` e `lintDebug` concluídos com sucesso. Lint: zero erros, 249 avisos e uma sugestão (mesmas contagens da rodada anterior). APK: `/workspace/artifacts/Lock-In-configuracoes.apk`. Assinatura verificada com `apksigner`; certificado de desenvolvimento preservado. Log: `/workspace/.setup/settings/build-lint.log`.
