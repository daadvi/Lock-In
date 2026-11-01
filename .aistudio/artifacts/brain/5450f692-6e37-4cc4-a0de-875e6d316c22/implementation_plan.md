# Auditoria Operacional Ponta a Ponta & Blindagem Preventiva (Lock In)

Plano de auditoria de experiência do usuário, cobertura de casos de borda e blindagem preventiva cobrindo todos os fluxos reais do Lock In: inicialização pós-boot, detecção de vídeos/shorts, enforcer de bloqueio, firewall VPN e desafios de alta fricção.

---

### Decisões Confirmadas pelo Usuário (Fase 1)

> [!IMPORTANT]
> **Escopo da Auditoria**: Foco total em todos os fluxos reais de ponta a ponta.  
> **Tratamento de Riscos**: Mapear detalhadamente os riscos operacionais e aplicar correções preventivas no código.

---

## 1. Visão Geral & Mapeamento de Fluxos Reais do Usuário

A experiência do Lock In é composta por 6 fluxos críticos onde o usuário interage diretamente com o sistema operacional e o hardware:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   FLUXOS DE USO CRÍTICOS DO LOCK IN                     │
└────────────────────────────────────────────────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    ▼                               ▼                               ▼
[1. Ciclo de Inicialização]   [2. Monitoramento & Ilha]     [3. Enforcer & Bloqueio]
 • Boot do Android             • Overlay / Dynamic Island    • BlockedActivity
 • Permissões de Sistema       • Diferenciação Vídeo/Short   • FocusLockActivity
 • Auto-Resume da VPN          • Isenção Bancária (Bancos)   • Trava de Desinstalação
    │                               │                               │
    └───────────────────────────────┼───────────────────────────────┘
                                    │
    ┌───────────────────────────────┴───────────────────────────────┐
    ▼                                                               ▼
[4. Firewall / Escudo de Rede]                                [5. Desbloqueios Estritos]
 • Bloqueio de Apps Esgotados                                  • Desbloqueio de Confiança (5x)
 • Sinkhole DNS de Sites Proibidos                             • Desafio de Alta Fricção (4 Etapas)
 • Sockets UDP Efêmeros Thread-Safe                            • Desbloqueio de Emergência (Max 2/mês)
```

---

## 2. Diagnóstico de Riscos & Cenários de Borda Identificados

Durante a análise minuciosa de cada jornada de uso, foram identificados 4 cenários de borda onde um usuário real poderia vivenciar inconsistências ou brechas no uso diário:

### Cenário 1: Reinicialização do Celular com Escudo de Rede (VPN) Habilitado
- **Comportamento atual:** O `BootReceiver` restabelece a ilha flutuante e a sessão de Focus Time, mas não checa o estado do `LockInVpnManager`. Se o usuário reinicia o aparelho, os apps esgotados continuavam com acesso à internet até que ele abrisse o app do Lock In manualmente.
- **Risco:** Perda temporária da proteção de rede após reinicialização do celular.
- **Mitigação Preventiva:** No `BootReceiver.kt`, verificar se `LockInVpnManager.isVpnEnabled.value` está ativo e `!isPermissionRequired(context)`. Se sim, acionar `LockInVpnManager.startVpn(context)` de forma transparente.

### Cenário 2: Concorrência e Transbordamento no Limite Mensal de Emergência
- **Comportamento atual:** O método `recordUnlockUsed` em `EmergencyUnlockManager.kt` grava o timestamp e incrementa `currentCount + 1`. Se múltiplos eventos de clique ou chamadas paralelas ocorrerem quando a cota estiver em 2/2, a contagem poderia incrementar para 3.
- **Risco:** Inconsistência na cota mensal de 2 desbloqueios máximos.
- **Mitigação Preventiva:** Adicionar trava atômica inicial em `recordUnlockUsed`: se `currentCount >= MAX_MONTHLY_UNLOCKS`, a execução é rejeitada e abortada sumariamente.

### Cenário 3: Colisão de Tópico de Rede em Salas com Código em Branco
- **Comportamento atual:** Em `FriendsPartyCloudClient.kt`, caso um código de sala seja submetido apenas com espaços ou caracteres especiais, `clean` resulta em string vazia, gerando o tópico `lockin_party_`.
- **Risco:** Múltiplos usuários com código malformado conectando-se ao mesmo canal genérico de notificações.
- **Mitigação Preventiva:** Se `clean.isBlank()`, rejeitar a submissão e retornar código de erro controlado sem realizar polling no tópico vazio.

### Cenário 4: Estabilidade de Transição do Modo Não Perturbe (DND)
- **Comportamento atual:** O bloqueio de foco altera configurações de notificação do sistema, mas precisa garantir que ao término da sessão o estado anterior de DND seja fielmente restaurado sem deixar o usuário incomunicável.
- **Risco:** Perda de notificações de chamadas telefônicas após encerrar a sessão de foco.
- **Mitigação Preventiva:** Validar a preservação do estado prévio do AudioManager/NotificationManager ao iniciar e finalizar sessões.

---

## 3. Arquitetura Técnica & Estratégia de Mitigação

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MAPA TÉCNICO DE CORREÇÕES                        │
├─────────────────────────┬──────────────────────────────────────────────┤
│ Arquivo                 │ Correção Preventiva                          │
├─────────────────────────┼──────────────────────────────────────────────┤
│ BootReceiver.kt         │ Auto-resume da VPN e Firewall pós-reboot     │
│ EmergencyUnlockManager  │ Trava idempotente contra overflow de cota    │
│ FriendsPartyCloudClient │ Sanitização estrita de identificador de sala │
│ ExampleRobolectricTest  │ Adição de testes unitários dos novos limites │
└─────────────────────────┴──────────────────────────────────────────────┘
```

---

## 4. Plano de Implementação

1. **Boot Lifecycle:**
   - Adicionar ativação segura de `LockInVpnManager.startVpn(context)` em `BootReceiver.kt` quando o serviço já estiver consentido pelo usuário.
2. **Quota Enforcement:**
   - Blindar `recordUnlockUsed` contra overflow de cota no `EmergencyUnlockManager.kt`.
3. **Cloud Client Hardening:**
   - Adicionar guarda contra código de sala nulo/vazio em `FriendsPartyCloudClient.kt`.
4. **Verificação & Testes:**
   - Adicionar casos de teste para os limites de emergência e regras de boot no `ExampleRobolectricTest.kt`.
   - Executar `compile_applet` e `gradle :app:testDebugUnitTest` para validação de zero regressão.
