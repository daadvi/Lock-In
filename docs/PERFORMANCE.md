# Otimização de tamanho e trabalho em segundo plano

Validação concluída em 2 de novembro de 2026. Estas mudanças preservam as correções de estabilidade da revisão anterior.

## Tamanho medido

Comparação entre APKs do mesmo projeto, antes/depois desta etapa, com a mesma chave local e o mesmo tipo de build:

| Variante | Antes | Depois | Redução |
| --- | ---: | ---: | ---: |
| Release | 17.094.739 bytes (17,09 MB) | 1.976.698 bytes (1,98 MB) | 88,44% |
| Debug | 26.173.460 bytes (26,17 MB) | 25.004.672 bytes (25,00 MB) | 4,47% |

O release final contém um arquivo DEX. O debug mantém ferramentas e código de desenvolvimento, por isso seu tamanho não representa o pacote otimizado para distribuição.

- R8 e redução de recursos habilitados somente no release.
- Removidas dependências não utilizadas: Firebase AI/App Check, Retrofit/Moshi, DataStore, Navigation Compose e interceptor de logging. OkHttp continua atendendo as chamadas usadas pelo aplicativo.
- As referências aos provedores TLS opcionais do OkHttp 4.10 receberam regras específicas do R8. O Android usa sua implementação TLS do sistema; nenhuma validação de certificado foi desativada. As demais verificações de classes ausentes continuam ativas.
- Informações de linha preservadas para diagnóstico. Guardar `app/build/outputs/mapping/release/mapping.txt` junto de cada versão para interpretar stack traces ofuscados.

## Trabalho e memória evitados

- MainActivity e telas principais observam seus dados apenas enquanto o lifecycle está STARTED ou RESUMED; voltam a observar ao retornar à tela. Os serviços de monitoramento continuam independentes.
- Flows Room da MainActivity são lembrados durante recomposições, evitando cancelar e recriar consultas a cada atualização visual.
- Widgets compartilham um trabalhador e uma fila com apenas um pedido pendente. Teste com 10 mil solicitações durante um processamento comprova duas execuções no total (a corrente e a pendente), sem trabalhadores simultâneos.
- VPN aguarda disponibilidade do descritor via `Os.poll`, em vez de acordar uma coroutine a cada 10 ms na ausência de tráfego. O timeout de um segundo permite verificar o encerramento.
- A tela de estatísticas atualiza a permissão no retorno das configurações, eliminando a consulta periódica a cada 1,5 segundo.
- Ícones são rasterizados na dimensão em pixels exigida pelo tamanho/densidade da tela, respeitando o cache limitado por bytes, em vez de sempre usar 128×128.

Essas são reduções de trabalho demonstráveis pelo código/testes. Não foi medido ganho percentual de FPS, tempo de abertura, RAM total ou bateria em aparelho físico.

## Validação executada

- Build debug e release otimizado: sucesso.
- 57 testes JVM/Robolectric: zero falhas, erros ou ignorados. Incluem fila de widgets e suspensão/retomada de coleta da MainActivity, além das regressões anteriores.
- Lint debug: zero erros, 255 avisos e uma sugestão.
- Lint vital release: sucesso.
- APK otimizado: assinatura verificada com apksigner; manifesto confirma applicationId original, minSdk 24 e targetSdk 36.
- `git diff --check`: sucesso.

Comandos utilizados:

```bash
source /workspace/.setup/activate.sh
cd /workspace/Lock-In
gradle --no-daemon --continue \
  -I /workspace/.setup/optimization/local-signing.gradle \
  :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

O init script externo apenas troca a assinatura de release pela chave de desenvolvimento neste ambiente, para permitir testar sem credenciais de produção. A configuração de assinatura de produção do repositório foi preservada. O APK em `app/build/outputs/apk/release/app-release.apk` é, portanto, um **APK otimizado para teste**, não um lançamento publicado.

Para distribuição, usar a chave de release existente, guardar o mapping e validar o APK otimizado em aparelhos, incluindo navegação, banco local, restauração de estado, VPN e monitoramento prolongado. Os testes Robolectric não executam o DEX ofuscado em hardware Android.

Artefatos de referência e logs desta sessão: `/workspace/.setup/optimization/` (`release-before.apk`, `debug-before.apk`, `metrics.json`, `baseline.log`, `optimized.log`, `final.log`).
