# Configurações, frases, recuperação e desinstalação

## Etapa 1 — Configurações

A lista foi substituída por cartões em uma grade com duas colunas em janelas de celular e quatro quando há pelo menos 700 dp úteis. A largura da janela decide o layout, inclusive em rotação ou tela dividida. Os cartões são agrupados em foco/alertas, conta/aparência, bloqueios, dispositivo e informações. Ícones de chave, banco, rede, ajustes, camadas e loja distinguem as funções; não há uma sequência de escudos nos atalhos. O tema claro/escuro e os atalhos pixel foram preservados. O tutorial passa a rolar diretamente até o cartão correspondente, sem depender do índice de uma grande lista interna.

“Conta e backup” descreve o armazenamento local existente. Os créditos foram reunidos em um cartão que abre o diálogo já disponível. O estado do administrador é reconsultado ao voltar das configurações do Android.

## Etapa 2 — Frases

O comparador central já normalizava caixa e acentos com `Locale.ROOT`. A regra foi preservada, incluindo a tolerância anterior a pequenos erros de digitação. As instruções agora explicam que maiúsculas/minúsculas são aceitas. Os testes exercitam frases em caixa alta/baixa nos níveis 1/3/5 e no desafio completo de desinstalação. Senhas continuam diferenciando maiúsculas e minúsculas.

## Etapa 3 — Desinstalação

Foram encontrados caminhos não cobertos: confirmação pertencente ao launcher, evento descartado pelo debounce geral e tentativa nova suprimida pela remoção anterior de outro app. A inspeção de janelas de remoção agora precede o debounce e conta com uma nova consulta limitada após 250 ms se a árvore ainda não estiver pronta. Janelas de launcher exigem uma pergunta de remoção, evitando disparar o desafio só porque há um ícone do Lock In e um atalho genérico “Desinstalar”. Abrir os detalhes do app sem clicar na remoção também não dispara o desafio.

O desafio pode aparecer em uma janela `TYPE_ACCESSIBILITY_OVERLAY` do serviço de acessibilidade, com senha e frases. Assim ele não depende apenas de o fabricante permitir iniciar uma Activity em segundo plano. O ciclo de vida do Compose é explícito; cancelar ou destruir/interromper o serviço remove a janela. A Activity não exportada mantém o fluxo de desativação do administrador e o resultado da remoção do Android após a autorização. A confirmação final do Android continua necessária; um app comum não pode se remover silenciosamente.

É necessário que a acessibilidade do Lock In esteja ativada. Falhas específicas de fabricantes, ausência de eventos ou revogação de acesso não podem ser excluídas por testes JVM. A compatibilidade deve ser confirmada em celulares dedicados.

## Etapa 4 — Senha de confiança

A troca antiga comparava o texto digitado com o hash salvo, rejeitando a senha correta. Agora usa `CryptoPasswordHelper.verifyPassword` e conserva o armazenamento com hash.

A recuperação anterior não enviava e-mail: gerava e exibia um código local; também permitia uma frase pública como alternativa. Isso foi substituído pela confirmação de PIN, padrão ou senha de bloqueio do próprio Android, por `KeyguardManager` e Activity Result. Cancelar, não ter bloqueio de tela, não conseguir abrir a verificação ou retornar um resultado diferente de sucesso não autoriza alteração. A confirmação tem prazo de dois minutos e é consumida ao salvar. A nova senha deve satisfazer os requisitos e coincidir com sua confirmação.

Essa é recuperação local da **senha de confiança**, não recuperação remota de conta por e-mail. O projeto não possui um serviço de envio/autenticação de e-mail configurado. A autenticação real do PIN permanece sob controle do Android; o Robolectric permite verificar a solicitação e o tratamento do resultado, mas não reproduz a implementação de todos os fabricantes.

## Validação

Os testes usam o runner isolado em `/workspace/.setup/settings-recovery/run-isolated.py`, sem rede, com dependências locais e cópia descartável das fontes.

- `:app:testDebugUnitTest`: 187 testes em 36 classes, sem falhas, erros ou testes ignorados. Inclui controles e capturas da grade de configurações, recuperação de senha, níveis, frases em caixa alta/baixa e fluxo de desinstalação.
- `:app:lintDebug`: concluído com zero erros, 236 avisos e 1 sugestão. Os avisos remanescentes incluem formatação dependente de localidade, APIs privadas de fabricante, textos em layouts e considerações de bateria/acessibilidade.
- `git diff --check`: aprovado.

`:app:assembleRelease` também concluiu. O APK de teste é `com.aistudio.lockin.pzkvtr` (versão `1.0`, código `1`), com SHA-256 `afacb6de3eabe9c79d12e7db3ad7223f02fb1f56617729cce1409da80408cab8`. Ele usa a mesma chave de desenvolvimento dos APKs de teste anteriores (certificado SHA-256 `178b70c18f8e4bf9e8f64071ce8aa0740f012ce204a2d1689d0c866615b6fe64`), permitindo atualizar essa instalação de teste. Para publicação na Play Store, o projeto ainda precisa ser assinado com a chave privada de produção.

A recuperação foi exercitada com troca por senha atual, autenticação Android aprovada/cancelada, aparelho sem bloqueio de tela, expiração e confirmação divergente. A senha permanece salva como hash.

## Limites de validação

O resultado de senha do Android foi simulado no Robolectric. A tela nativa do PIN/padrão e diferenças entre fabricantes precisam de confirmação em aparelhos físicos. O desafio de desinstalação depende do serviço de acessibilidade estar habilitado; a tela final de remoção continua sendo confirmada pelo Android. Esses testes não demonstram comportamento em todos os launchers, instaladores ou versões de sistema.
