# Tutorial com destaque circular e robô animado

O guia usa uma camada dentro da mesma janela do app. Uma máscara escurece o restante da tela, preservando a região circular em torno do alvo real. O círculo é limitado às bordas do visor. As posições vêm das coordenadas dos controles após layout/rolagem, sem coordenadas fixas por modelo de celular.

O balão é posicionado no maior espaço livre ao redor do círculo e o texto longo é rolável. Durante o guia há somente duas ações: **Ocultar** ou **Próximo/Concluir**. Não existem seleção direta de capítulos, botão Anterior, fechar nem pular. O robô desliza, nesta ordem, entre inferior direito, superior direito, inferior esquerdo e superior esquerdo. O círculo e o balão animam posição e tamanho conforme o alvo e a etapa.

Um host estável mantém o guia durante a navegação interna. Para usuários que concluíram o cadastro, os capítulos navegam às seções correspondentes. Nas configurações, a lista rola até o item explicado; estatísticas também posiciona o gráfico/indicador. Ocultar salva a etapa atual para retomada posterior. O guia não concede permissões, não cria rotinas, não inicia bloqueios nem altera limites por conta própria.

No primeiro cadastro, o guia começa automaticamente salvo se a pessoa o tiver ocultado. A etapa de conta só avança após a autenticação ou cadastro terminar. A etapa de permissões espera Acessibilidade, Acesso ao uso dos apps e Sobreposição. A escolha do nível espera a confirmação do nível. Essas transições ocorrem automaticamente depois da ação real; o botão Próximo permanece desativado enquanto a condição não for cumprida. Se o recurso ainda não estiver disponível na tela, o guia mostra orientação em vez de desenhar um círculo em coordenadas inventadas.

O robô foi adaptado da referência do usuário, com corpo em PNG transparente e rosto animado em Canvas. Há nove expressões. Piscadas funcionam apenas enquanto o guia está visível e com lifecycle RESUMED. A opção de desativar animações do Android é respeitada; não há vídeo, áudio, serviço de fundo nem biblioteca adicional. Uma imagem amostrada de cerca de 1,5 MiB é compartilhada e carregada fora da thread principal.

## Verificação

Os testes de interface verificam a máscara, a retomada após ocultar, o deslocamento do robô, as nove expressões com movimento reduzido, o posicionamento do balão fora do círculo em retrato/paisagem e a rolagem até a opção real da Play Store sem ativá-la. Também verificam que conta, permissões e nível bloqueiam Próximo até a ação correspondente. Imagens de conferência são renderizadas com Robolectric/Roborazzi. Nenhum teste em aparelho físico foi realizado.

Na primeira verificação, nem todas as instâncias observavam a conclusão do carregamento da imagem: o cache passou a publicar um estado compartilhado observável. A rolagem também passou a aguardar o posicionamento do item na lista antes de pedir a exibição do controle. Os testes foram repetidos para verificar essas correções.

Uma revisão visual anterior detectou que limitar o círculo à borda do visor deslocava seu centro acima da opção Play Store. A solicitação de rolagem reserva 108 dp de espaço vertical em volta do controle e o teste exige que o centro do alvo esteja na região interna do visor.

## Validação mais recente

- `:app:testDebugUnitTest`: 190 testes, zero falhas, zero erros e zero ignorados.
- `:app:lintDebug`: zero erros, 239 avisos e uma sugestão. Os avisos são principalmente textos fixos em widgets e APIs Android depreciadas já existentes.
- `:app:assembleRelease`: concluído; o APK foi verificado com `apksigner` usando assinatura v2 de desenvolvimento.
