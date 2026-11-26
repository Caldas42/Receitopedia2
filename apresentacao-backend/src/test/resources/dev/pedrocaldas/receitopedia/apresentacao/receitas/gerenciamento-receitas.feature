# language: pt
Funcionalidade: Gerenciamento de receitas

  Cenário: Adicionar, visualizar, editar e remover uma receita
    Quando cadastro uma receita com o título "Bolo de cenoura"
    Então a receita aparece na lista
    E o detalhe mostra os ingredientes e as etapas na ordem cadastrada
    Quando edito o título para "Bolo de cenoura com cobertura"
    Então consulto a receita com o título "Bolo de cenoura com cobertura"
    Quando removo a receita na versão atual
    Então a receita deixa de aparecer na lista e o detalhe retorna 404

  Cenário: Rejeitar o cadastro sem ingredientes
    Quando tento cadastrar "Sopa" sem ingredientes
    Então o cadastro é recusado com a mensagem "Informe entre 1 e 50 ingredientes."

  Cenário: Resolver duas edições simultâneas da mesma versão
    Dado que cadastrei uma receita com o título "Pão de queijo"
    Quando dois clientes alteram somente os ingredientes usando a mesma versão
    Então apenas uma edição é aceita e a outra retorna conflito
    E os ingredientes da edição aceita ficam salvos
    E uma exclusão baseada na versão antiga é recusada

  Cenário: Somente o dono pode modificar uma receita
    Dado que cadastrei uma receita com o título "Bolo de milho"
    Então a receita é pública e identifica seu dono
    E um visitante precisa entrar para criar receitas
    E outro usuário e um administrador recebem 403 ao editar ou remover a receita

  Cenário: Ver as próprias receitas em uma aba separada
    Dado que cadastrei uma receita com o título "Receita pessoal"
    Então Minhas Receitas lista somente as receitas do usuário logado

  Cenário: Recusar receita sem título
    Quando cadastro ingredientes e modo de preparo sem título

  Cenário: Usar somente minutos no tempo de preparo
    Quando cadastro uma receita com 45 minutos e sem horas
    Então o tempo consta como 45 minutos no detalhe e na lista
    E horas fracionárias são rejeitadas
