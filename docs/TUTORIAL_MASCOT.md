# Robô do tutorial

O usuário escolheu o robô branco/cinza, com contornos escuros, detalhes laranja, extensões altas na cabeça, fones laterais e corpo compacto. A referência mais recente foi enviada na conversa, sem os elementos decorativos ao redor.

Foi preparada uma versão animável a partir dessa referência com a ferramenta de imagem: fundo transparente e painel facial sem olhos/boca. É uma adaptação da referência, não uma cópia binária do arquivo original. A base está em `app/src/main/res/drawable-nodpi/tutorial_robot_base.png`; olhos, sobrancelhas, boca e óculos são desenhados e animados no app em `TutorialRobot.kt`.

Expressões: feliz, medo, suspeito, sagaz, desperto, inteligente, determinado, preguiçoso e muito determinado. Cada capítulo escolhe uma expressão; olhos e rosto fazem transições e piscadas. A arte é decodificada fora da thread de interface, amostrada e compartilhada entre instâncias.

A pedido do usuário, a voz foi suspensa. O tutorial usa somente texto e animação; os roteiros anteriores ficam como histórico, sem dependência de gravações para usar o guia.
