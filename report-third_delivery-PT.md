Relatório da Terceira Entrega
===

A nossa implementação final do sistema Turmas utiliza Vector Clocks para manter a coerência eventual do sistema.

Apesar da nossa implementação não oferecer nenhum tipo de garantia de sessão aos seus clientes, esta converge para um estado único, comum a todos os servidores, e funciona mesmo quando há mais do que um servidor secundário no sistema.

A politica de reconciliação aplicada em cada propagação de estado é:
- Escolhe-se qual dos dois estados é o mais prioritário (s1) com base no qualificador (P ou S) e, em caso de empate, com base no porto do servidor.
- Partindo do estado prioritário fazem-se as seguintes modificações:
    - Se o estado menos prioriario (s2) tem um número de vagas superior ao estado s1 então o novo número de vagas é o de s2.
    - Todos os alunos que estão inscritos em s2 são adicionados à turma em s1 a não ser que uma das duas condições se verifique:
        - Esse aluno já está na lista de alunos desinscritos em s1
        - A capacidade da turma de s1 tenha sido excedida, neste caso todos os alunos por inscrever são adicionados à lista de alunos desinscritos
    - Todos os alunos que estão desinscritos em s2 são adicionados à lista de alunos desinscritos em s1 a não ser que:
        - Esse aluno já esteja na lista de alunos inscritos em s1
    - A propriedade que diz respeito às inscrições estarem abertas mantém-se sempre igual à do estado s1.

A implementação de Vector Clocks utilizada baseia-se nas seguintes regras:
- Sempre que é feito um propagateState o servidor envia o clock respetivo à sua própria entrada no Vector Clock que está a manter em memória.
- Sempre que é recebido um propagateState o servidor compara a entrada que tem guardada em memória respetiva ao servidor que lhe enviou a propagação de estado e usa-a para decidir como conciliar os conflitos.
- Sempre que é executado algum comando por parte dos clientes ou é recebida alguma propagação de estado, o servidor aumenta a sua própria entrada no seu Vector Clock.

Esta implementação foi escolhida, uma vez que, para este problema, não é necessário garantir a coerência causal dos eventos e, por isso, podemos usar uma versão mais relaxada das regras originais de Vector Clocks. 