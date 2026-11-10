# Tutorial e cores dos níveis

## Entrega

- Básico: verde suave; Determinado: âmbar; Sonho do Dev: degradê laranja-vermelho.
- Paleta compartilhada na escolha inicial e na troca do nível pelas configurações, adaptada ao tema claro/escuro. Nomes, números e estados selecionados continuam visíveis; a cor não é a única indicação.
- As regras dos níveis permanecem 1/3/5 repetições de senha e 3/2/1 desbloqueios diários por app.
- O primeiro cadastro abre um guia linear com robozinho pixelado. A pessoa pode ocultá-lo e retomá-lo depois, ou concluí-lo; não há botão para pular nem seleção direta de capítulos.
- Conta, permissões essenciais e confirmação do nível são acompanhadas pelo guia. Ele só avança depois da conclusão real de cada uma dessas etapas. Os demais 28 capítulos podem ser revisitados pelos ajustes após o cadastro.
- Texto do guia e roteiro de gravação correspondentes. O kit contém roteiro completo, 28 falas individuais, lista de nomes e manifesto.

## Voz: etapa dependente dos arquivos do usuário

Esta versão usa texto: **não contém narração**. O usuário gravará sua própria voz; os nomes sugeridos estão em `TUTORIAL_VOICE_SCRIPT.md` e `tutorial-audio-manifest.json`. Nenhuma voz sintética ou clonada foi criada. Depois do envio será necessário integrar a reprodução, tratar pausa/troca de capítulo e ajustar o texto às gravações reais. Os arquivos de áudio não devem ser simulados por arquivos vazios.

A rotina atual chamada de backup na nuvem grava arquivos locais. O capítulo 23 explica essa limitação e não promete restauração entre celulares ou após desinstalação. Os capítulos de detecção de vídeos, VPN, painel de amigos e proteção contra desinstalação também evitam garantias que dependem do Android, da conexão e de outros apps. Não foram reimplementadas essas funções nesta tarefa.

## Validação

Suíte completa executada: **125 testes, zero falhas, zero erros, zero ignorados** (`/workspace/.setup/tutorial/full-tests.log`). Os três testes novos verificam seleção de níveis em ambos os temas, identificação das paletas, regras preservadas, navegação/retomada/finalização do guia e nomes únicos para gravação.

As prévias são renderizadas por Robolectric/Roborazzi em tela de 360 dp. Uma primeira captura do nível claro estava sem a superfície de fundo na montagem de teste; a montagem foi corrigida para refletir o tema real. Capturas finais aguardam um quadro renderizado antes de gravar a imagem. Não houve teste em aparelho físico nem de reprodução de áudio nesta etapa.

Build final `assembleRelease` e `lintDebug` concluído com sucesso. Lint: zero erros, 250 avisos e uma sugestão; os dois avisos novos recomendam sintaxe KTX para SharedPreferences, sem indicar erro funcional. A rodada final dos três testes de tutorial também passou (`/workspace/.setup/tutorial/visual-final.log`).

APK de teste: `/workspace/artifacts/Lock-In-tutorial-e-niveis.apk`, assinatura de desenvolvimento verificada com apksigner. Roteiro: `/workspace/artifacts/Lock-In-roteiro-da-sua-voz.md` e `.txt`. Kit: `/workspace/artifacts/Lock-In-kit-de-gravacao.zip`, integridade conferida e 28 falas individuais presentes. Prévias: `/workspace/artifacts/Lock-In-previa-tutorial.png`, `/workspace/artifacts/Lock-In-previa-niveis-claro.png` e `/workspace/artifacts/Lock-In-previa-niveis-escuro.png`.

Atualização: a narração foi suspensa a pedido do usuário. O guia agora usa destaque circular, balão de texto e robô animado; ver `TUTORIAL_SPOTLIGHT.md`. Não é necessário enviar áudio para essa versão.
