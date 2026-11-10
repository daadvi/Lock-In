# Revisão de segurança e estabilidade do Lock In

Esta é uma revisão técnica com correções e testes locais, **não uma certificação de segurança nem uma conclusão de que todos os bugs foram eliminados**. O alvo foi o código Android de `daadvi/Lock-In`, sobre `dac3293ed4074eabd10d9f9e9f21d54cba91ef9d`, incluindo as alterações locais anteriores. Não houve publicação, acesso a grupos reais, tentativa contra o serviço ntfy ou alteração de infraestrutura externa.

## Problemas encontrados e correções

| Área / ação que acionava o problema | Comportamento anterior no código | Correção |
|---|---|---|
| Receber uma resposta de instalação/saída no painel de amigos | A resposta podia liberar um pacote fornecido na mensagem sem existir um pedido pendente correspondente. Identidade do emissor era apenas um campo JSON. | Exigir membro conhecido, pedido pendente, emissor diferente do solicitante e estado válido; pacote/solicitante vêm do pedido salvo. Repetir a aprovação não renova a liberação. |
| Acessar o canal do grupo | Convite de quatro dígitos determinava um tópico; conteúdo era publicado sem criptografia de aplicação. | Convite aleatório de 256 bits, tópico derivado separadamente e AES-256-GCM com nonce aleatório, versão autenticada e ID/timestamp dentro do conteúdo cifrado. Códigos antigos não enviam nem recebem mensagens. Não foi sondado o serviço real. |
| Desativar compartilhamento detalhado | A tela escondia os detalhes, mas a serialização ainda continha apps usados. | A mensagem enviada omite apps e tempo exato quando o compartilhamento está desativado; o usuário conserva seus dados locais. |
| Receber linhas ou JSON excessivos | Leitura de linha/resposta não tinha limite; estruturas profundamente aninhadas podiam sobrecarregar o parser. | Limites antes do parsing: 256 KiB por lote, 100 linhas, 8192 bytes por linha, 32 níveis JSON, 4096 bytes no envelope e 15 s por chamada HTTP. Validação iterativa estrita antes de JSONObject rejeita comentários, aspas simples e sintaxe permissiva que podia enganar a contagem de profundidade. Membros/pedidos também têm limite. |
| Celulares com relógios diferentes ou mais de 100 mensagens no mesmo segundo | O horário enviado pelo participante era usado como cursor de transporte; podia saltar mensagens ou repetir o mesmo lote. | Paginação por ID validado do relay; mensagem rejeitada ainda avança o cursor. Horário do participante é apenas metadado. Tópico derivado cabe no limite de 64 caracteres. |
| Alternar/sair de grupos durante sincronização | Resultados assíncronos de uma sala anterior podiam chegar depois da troca. | Geração de sessão e verificação da sala antes de aplicar resultados; uma única rota de ingestão; listas e deduplicação limitadas. Pedidos já resolvidos saem do histórico quando necessário, sem impedir novos pedidos ao chegar ao limite. |
| Usar emails diferentes com pontuação semelhante | A substituição de pontuação por `_` fazia contas diferentes apontarem para o mesmo arquivo. | Nome derivado do email completo normalizado; conferir proprietário dentro do documento antes de migrar/restaurar. |
| Restaurar backup ou escolher cópia externa mais recente | O backup participava da autenticação e continha hashes; o hash da senha de confiança podia ser hasheado novamente, tornando a senha digitada inválida. | Somente armazenamento privado; credenciais não são exportadas/restauradas; senha atual é preservada; gravação atômica e leitura limitada a 1 MiB/32 níveis. Sair da conta preserva seu verificador local. |
| Entrar na conta ou restaurar regras antigas | Login restaurava automaticamente e a ação de restauração não pedia o desafio, podendo reduzir regras atuais. | Login não aplica backup; restauração de instalação já configurada exige o fluxo de confiança de senha/frase. Cancelar conserva as regras. |
| Backup malformado ou rotina de um app | Erros em seções tardias ocorriam depois de alterar preferências; o pacote alvo da rotina não era preservado. | Serializar snapshot/restauração com mutex e pré-validar todas as seções antes de aplicar; salvar/restaurar `targetPackage` e `targetAppName`. Falha física de disco entre Room e preferências ainda não é uma transação única. |
| Iniciar uma rotina depois de selecionar apps globais | As exceções globais se somavam às da rotina e permaneciam em cache. | Uma lista efetiva por contexto: sessão direta ou rotina atual; uma rotina sem exceções não herda a lista global. Abrir um app não cria uma exceção temporária além da política. |
| Alterar a hora durante sessão direta de foco | O fim dependia apenas do horário civil. | Relógio monotônico e identificador de boot durante a mesma inicialização; recuperação após reinicialização continua usando horário civil. |
| Atualizar widgets | Os receivers iteravam IDs recebidos e faziam trabalho de renderização/Binder na thread principal. | Encaminhar à fila existente que agrupa pedidos, busca IDs próprios e atualiza fora da thread principal. Não foi reproduzido um ANR físico causado por esse caminho. |
| Armazenar a senha de conta | SHA-256 rápido com salt fixo. | PBKDF2-HMAC-SHA1 com 600.000 iterações, salt aleatório e comparação constante; trabalho em IO; migração após login correto. A senha de confiança usa seu formato anterior por compatibilidade e merece revisão separada de custo/latência. |

Também foram explicitadas exclusões de backup/transferência do Android para os dados privados, marcada a cópia do convite como sensível e movida a consulta de backup no diálogo de conta para IO. A tela agora explica que o backup é local.

## Efeito da atualização

- Grupos antigos ficam com sincronização pausada. Use **Recriar com convite seguro** e compartilhe o novo convite em particular; todos os participantes precisam atualizar.
- Backup local não substitui backup remoto e desaparece ao apagar os dados/remover o app. Cópias externas antigas não autenticam usuários nem são restauradas. Se uma versão antiga apagou o cadastro ao sair, pode ser necessário registrar novamente a conta local; não há recuperação por provedor de email implementada.
- As senhas de conta existentes continuam aceitas e são migradas após login correto. A senha de confiança atual permanece inalterada.
- Nenhum bloqueio de rede universal, identidade de provedor ou impossibilidade de desinstalar pelo dono do aparelho é prometido.

## Validação e evidências

Os testes são executados com JUnit/Robolectric/Compose, sem internet, em namespace de rede separado, ambiente limpo e lista explícita de ferramentas/variáveis. Fonte e caches são copiados para tmpfs de 5 GiB; ferramentas são somente leitura. Limites das execuções finais: 2 CPUs para as JVMs, 8 GiB de endereço virtual por processo, 256 processos, 512 MiB por arquivo, 600 s de CPU e 600 s de tempo total por execução; Gradle usa um worker e heap de 2 GiB. As execuções iniciais usavam limites de 6 GiB/256 MiB. O processo pai promove somente arquivos regulares esperados, com descritores no-follow e limites de tamanho.

O runner e os resultados estão em `/workspace/security-audit/Lock-In/run-1/`. Primeira verificação: quatro testes de DNS passaram, incluindo 10.000 entradas malformadas determinísticas. A primeira suíte completa falhou pela configuração do resolvedor offline do Android; a segunda passou 152/159, com sete casos ainda apontando para uma imagem Android 9 não instalada. Esses erros foram identificados como configuração dos testes, não defeitos do aplicativo; a classe foi ajustada para o Android 34 disponível. Os resultados posteriores estão registrados abaixo.

Testes novos cobrem convites/criptografia, adulteração, chave incorreta, mensagens antigas, limites de parsing, aprovação não solicitada, autoaprovação, pacote injetado, repetição, privacidade, isolamento de backups, arquivo externo, JSON profundo/grande, preservação de senha, escopo de rotina, desafio antes de restaurar, lista efetiva do foco e relógio monotônico. A suíte anterior cobre onboarding, níveis 1/3/5, telas de bloqueio, desinstalação, permissões, persistência, lifecycle, fila de widgets e tutorial.

## O que ainda impede afirmar “pronto e sem falhas”

1. **ANRs reais e uso prolongado:** não há aparelho físico nem emulador com imagem Android neste ambiente. É necessário observar 8–24 h de segundo plano, economia de bateria, alternância de apps, reinício, revogação de permissões e falhas OEM, coletando traces de ANR.
2. **Amigos:** não houve teste entre dois celulares ou validação de disponibilidade do relay. Expiração do cache pode perder mensagens; linhas externas inválidas sem ID aproveitável ainda podem impedir o avanço até saírem do cache. O convite é segredo de grupo; um participante malicioso com esse segredo ainda não tem uma identidade individual criptograficamente vinculada. Compartilhe apenas com pessoas confiáveis. A arquitetura não oferece autoridade de um servidor contra um cliente modificado.
3. **VPN:** filtra DNS pela rota específica; DoH/DoT, IP direto, DNS em cache e outros caminhos podem escapar. Não é firewall completo. Acessibilidade também depende de navegador, idioma e interface.
4. **Controle do dono do aparelho:** root, ADB autorizado, modo seguro, revogação e limpeza de dados são poderes do sistema. Calendários/reset e recuperação de duração após reboot ainda dependem do horário civil. Um app comum não consegue garantir impedimento desses poderes.
5. **Dados/publicação:** conta local não verifica propriedade do email; migração de banco antigo, perda de armazenamento durante restore, base atualizada de vulnerabilidades de dependências, assinatura de produção e requisitos Play Store precisam de validação própria.
6. **Teclados:** as sugestões para desabilitar ditado dependem do teclado, conforme a escolha anterior de manter o teclado do usuário. Não equivalem a bloqueio universal de fala.

## Ferramentas solicitadas

O roteiro de código do **Cloudflare Security** foi consultado; não há backend Cloudflare identificado no app nem configuração de conta conectada examinada. Ferramentas do **Codex Security Cloud** não estavam disponíveis na sessão.

O workflow formal completo do plugin está marcado **incompleto** em `run-metadata.json`: os validadores `.cjs` citados pelo pacote não puderam ser lidos e as fases formais de caça/crítica/verificação por registro não foram concluídas. Uma revisão independente complementar do código foi concluída posteriormente, encontrou dois defeitos adicionais e originou correções/testes; ela não substitui essas fases formais. O relatório formal preserva candidatos e lacunas; não afirma zero vulnerabilidades. Este documento registra a engenharia e a regressão efetivamente realizadas, separadas de uma certificação formal.

### Reprodução antes da correção

Em uma cópia descartável, substituímos apenas `FocusTimeManager.kt` pela implementação do commit-base e executamos os quatro testes novos de política de foco. **Os quatro falharam por asserções**, reproduzindo a herança indevida de exceções, a transição de sessão para rotina, a lista vazia e o uso do horário civil. Evidência: `agents/local-checks/artifacts/execution-4.log` e `test-4-com.example.service.FocusPolicySecurityTest.xml`. O código de trabalho não foi revertido.

### Limites do procedimento formal

O [SKILL.md do Cloudflare Security / Security Audit](skill://plugins_6ab3634387ec81919729f26fb2a8e67b/security-audit/SKILL.md) exige terminar com os validadores aprovados ou com “`run_status: "incomplete"` ... with its exact reason”. Foi usada a segunda condição: os dois scripts de validação mencionados pelo pacote retornaram `failed to read skill resource`, e a verificação formal de cada registro não foi concluída. A revisão independente complementar descrita abaixo tem escopo de engenharia. Isso limita a alegação de auditoria formal; não invalida os testes locais identificados neste documento.

A execução conjunta de testes e release com heap de 768 MiB falhou no compilador Kotlin e depois perdeu o daemon Gradle, portanto **não conta como suíte aprovada nem como APK validado** (`execution-5.log`). O runner foi ajustado para heap de 2 GiB, inicialmente com limite de endereço virtual de 6 GiB, namespaces e limite de tempo; testes e release passam a ser executados separadamente.

### Concorrência do backup

Na execução 6, 161 de 162 testes passaram. O teste de documento malformado revelou interferência de um backup automático disparado ao alterar o limite. As operações completas de snapshot/restauração passaram a usar um mutex, evitando gravar estado intermediário de uma restauração; a conclusão de uma gravação também não reinsere uma conta que saiu durante a operação. O teste aguarda os jobs de gravação originados pela preparação antes de inserir seu documento malformado. Os 12 testes de backup e o teste de interface da restauração passaram na execução 7.

## Resultados de teste

**Execução 8: 163 testes executados, zero falhas, zero erros e zero ignorados.** Esta suíte antecede os últimos ajustes de JSON e paginação. Compilação e testes concluídos em 5 min 16 s na execução 8 (`agents/local-checks/artifacts/execution-8.log`); XML individual preservado por classe e totais em `test-summary.json`. Inclui os 13 testes de backup/interface repetidos anteriormente, os 8 de mensagens/criptografia e os 9 de autorização de grupos. A implementação anterior de foco falhou nos quatro testes de regressão, e a implementação corrigida passou. Nenhum teste foi realizado em aparelho físico.

### Revisão independente complementar

A revisão seguinte identificou dois defeitos nos ajustes em andamento: a contagem manual de profundidade podia ser contornada pela sintaxe permissiva do parser Android, e o cursor de mensagens dependia do relógio de outro celular. A validação passou a usar `JsonReader` estrito e iterativo; a sincronização passou a paginar por IDs do relay, inclusive ao rejeitar conteúdo de mensagens. Foram adicionados três testes de JSON e cinco de transporte, incluindo 150 mensagens com o mesmo timestamp em dois lotes. Não foi necessário provocar um estouro de pilha: os testes verificam a rejeição antecipada do documento.

**Execução 10: 50 testes direcionados, zero falhas/erros/ignorados**, em 2 min 5 s. Abrange backup, sua interface, JSON, criptografia, transporte e autorização de grupos. Uma pequena redução posterior do trecho sincronizado do polling evita manter o monitor durante todo o lote; a suíte completa da execução 12 valida esse estado final.

A execução 9 de release/lint perdeu o daemon durante a otimização, sem APK promovido. Foram aumentados os limites de endereço virtual/arquivo, reduzido o paralelismo e habilitada coleta de falha da JVM; a causa exata dessa perda não foi comprovada. Essa tentativa não conta como build aprovado.

### APK gerado

A execução 11 concluiu `:app:assembleRelease` em 3 min 10 s, incluindo R8, redução de recursos e lint vital de release. APK de teste: `/workspace/artifacts/Lock-In-seguranca-revisada.apk` (2.704.634 bytes). Assinatura verificada com `apksigner`: certificado SHA-256 `178b70c18f8e4bf9e8f64071ce8aa0740f012ce204a2d1689d0c866615b6fe64`, igual ao APK anterior de teste. A chave é de desenvolvimento; não é assinatura de publicação na Play Store. A instalação como atualização exige que a versão instalada tenha essa mesma assinatura.

SHA-256 do arquivo APK: `5834f4899d658d1e0bb24e67d3a253c321d350b789f5e184f707059c213327c0`.

### Verificação final — execução 12

**171 testes, zero falhas, zero erros e zero ignorados, em 34 classes.** Comando: `:app:testDebugUnitTest :app:lintDebug -Proborazzi.test.record=true`, executado pelo runner isolado. Build/testes/lint concluídos em 7 min 38 s. Lint: **zero erros, 237 avisos e uma sugestão**, sem adicionar baseline nem desabilitar verificações. As fontes/configurações conferem com o snapshot do APK da execução 11. `git diff --check` também passou.

As capturas Roborazzi foram executadas em modo de gravação; não representam uma comparação visual automática contra imagens de referência. Os testes de interação usam asserções de estado/comportamento. Houve uma mensagem de limpeza de diretório temporário de fontes do Robolectric (`DirectoryNotEmptyException`); todos os resultados XML permaneceram sem falhas/erros, e o diretório descartável foi eliminado ao encerrar o namespace. Isso não foi uma exceção do aplicativo em um aparelho.

Entre os avisos persistentes: 112 recomendações KTX, 26 de locale, 26 de textos em recursos, 14 de fontes pequenas, além de acessibilidade e compatibilidade. A referência estática à ilha flutuante é limpa no `onDestroy`; isso não prova ausência de retenção em uso prolongado. As consultas refletidas a APIs privadas possuem fallback, mas continuam dependentes do fabricante. A solicitação de exceção de bateria exige avaliação de elegibilidade/política antes de publicação na Play Store. Esses avisos não foram declarados resolvidos.

Entrega: `Lock-In-seguranca-revisada.apk`, `Lock-In-relatorio-seguranca.md` e `Lock-In-evidencias-testes.zip` em `/workspace/artifacts/`. O ZIP inclui logs de build/verificação, os XMLs da suíte final, a reprodução anterior de foco, o lint e o inventário de hashes das fontes. Não inclui credenciais, dados de usuários nem tráfego de serviços reais.
