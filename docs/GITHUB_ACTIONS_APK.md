# APK pelo GitHub Actions

O workflow `Gerar APK instalável` roda em cada push para `main`, em pull requests para `main` e quando for iniciado manualmente pela aba **Actions** do GitHub.

Ele executa testes unitários, lint e `assembleDebug`. Ao terminar, publica o arquivo `app-debug.apk` como o artefato `Lock-In-APK-<número da execução>`.

## Como instalar

1. Abra o repositório no GitHub e entre em **Actions**.
2. Abra uma execução verde de **Gerar APK instalável**.
3. Em **Artifacts**, baixe `Lock-In-APK-…` e extraia o arquivo ZIP.
4. Transfira `app-debug.apk` para o celular, permita instalações dessa origem quando o Android solicitar e abra o arquivo.

O workflow cria uma chave de desenvolvimento temporária a cada execução porque uma chave privada não fica no repositório. Por isso, para atualizar um APK produzido por uma execução anterior, talvez seja necessário desinstalar a cópia de teste antes de instalar a nova. Para uma distribuição estável ou Play Store, configure uma chave de upload persistente nos Secrets do GitHub e gere uma versão assinada para produção.
