# Proteção de entrada de texto

Os campos de senha, frase e código dos desafios usam `ManualEntryTextField`:

- `TrustUnlockScreen`
- `HighFrictionSecurityChallengeFlow`
- `EmergencyUnlockScreen`
- `ChangeLockLevelDialog`
- `AbsoluteInstallBlockDialog`
- Confirmação de senha em `WebsiteBlacklistDialog`
- Senha atual, código e frase de recuperação em `SettingsScreen`

A proteção esconde o menu de seleção, fornece uma área de transferência vazia
somente ao campo e ignora operações de copiar/recortar/colar. Não limpa nem lê a
área de transferência real do usuário. Intercepta Ctrl/Cmd+C/X/V, Ctrl+Insert,
Shift+Insert/Delete e as teclas dedicadas, inclusive quando enviadas pelo IME.
Comandos `InputConnection.performContextMenuAction` de copiar, recortar e colar,
completamentos e correções automáticas também são recusados.

A validação compara o trecho alterado, permitindo inserir um ponto de código por
vez e apagar texto. A regra anterior comparava apenas o tamanho total: substituir
uma seleção por um texto colado de tamanho igual ou menor passava. A nova regra
também permite composição de acentos portugueses e substituição manual de um
caractere. Sugestões e autocorreção são desativadas nesses campos.

Os campos comuns de cadastro, busca e compartilhamento mantêm suas operações
normais de texto. O bloqueio de copiar e colar se destina aos desafios em que a
digitação manual é obrigatória.

## Ditado e limites do Android

`LockInTheme` aplica a todas as sessões Compose, inclusive diálogos, as opções
privadas `nm`, `com.google.android.inputmethod.latin.noMicrophoneKey` e
`com.android.inputmethod.latin.noMicrophoneKey`, além de
`IME_FLAG_NO_PERSONALIZED_LEARNING` no Android 8 ou superior. A proteção manual também instala essas opções
para telas que não usam o tema principal. Tipo do campo, máscara da senha e ação
do teclado são preservados.

Essas opções solicitam que teclados compatíveis ocultem o microfone. **Android não
oferece uma proibição universal de ditado para teclados de terceiros** e não informa
se `commitText`/`setComposingText` veio de voz, colagem, gestos ou teclas. Uma frase
inserida em bloco é recusada nos desafios; texto fornecido um caractere por vez por
um teclado que ignore as opções é indistinguível de digitação normal. Colagem de
um único caractere enviada diretamente como texto pelo IME tem o mesmo limite.
Impedir qualquer ditado, independentemente do IME, exigiria retirar o teclado do
sistema desses campos e fornecer um teclado interno. Não há monitoramento do
microfone nem serviço adicional em segundo plano.

## Validação

`ManualTypingTest` verifica substituições de seleção, inserção em bloco, edição
no meio do texto, exclusão, acentos combinados e Unicode. `ManualEntryUiTest`
exercita campos Compose, atalhos, área de transferência Android, diálogos,
conexões reais de entrada do Compose, composição de texto e ação Concluir.
Também percorre as cinco confirmações de senha da tela de emergência e verifica
que sua frase aceita digitação manual e recusa inserção em bloco.

Em 2026-11-02, os 9 testes novos passaram na execução direcionada. A suíte completa
executada em seguida passou com **66 testes, zero falhas, erros ou testes ignorados**.
Após o ajuste de compatibilidade com Android 7, a suíte completa passou novamente.
As compilações debug/release e o lint concluíram; o lint manteve os 255 avisos e
1 sugestão preexistentes, sem erros. A assinatura do APK release de teste foi
verificada com `apksigner`; ele usa a chave local de desenvolvimento.

Os testes rodam em Robolectric/Android 34; não executam o aplicativo Gboard nem
outros teclados proprietários. A presença/ausência do botão de microfone deve ser
validada em aparelhos com as versões de teclado utilizadas pelos usuários.
