# Receitopédia

Rede social para publicar e descobrir receitas, seguir pessoas e conversar
com amigos por mensagens diretas ou em grupos.

## Contas e receitas

Em **Criar conta**, informe e-mail, nome de usuário e senha. Depois de entrar,
o menu do usuário fica no canto superior direito. A tela **Receitas** é pública
e mostra todas as receitas. **Minhas Receitas** filtra apenas as suas. As duas
telas dão acesso ao cadastro de uma receita.

Uma **receita** exige **título**, **ingredientes** e **modo de preparo**, com um
ingrediente ou uma etapa por linha. Descrição, tempo de preparo e fotos são
opcionais.
Ao informar somente horas ou somente minutos, a outra parte vale zero.
A **foto principal** e o nome do **autor** aparecem na lista. A visualização
também mostra as fotos secundárias, que podem ser ampliadas, e permite seguir
o autor quando você está logado e a receita é de outra pessoa. Cada receita
aceita até cinco imagens JPEG, PNG ou WebP de até 5 MB cada.

A edição e a exclusão exigem a sessão do autor. Receitas criadas antes da
migração continuam na lista pública como **Receita sem autor associado**.
O banco original não registrava o autor, portanto essas receitas ficam
disponíveis apenas para leitura.

## Categorias e busca

Em **Categorias**, administradores podem criar, editar, remover e reordenar
as categorias. Arraste pela alça ou use as setas para cima e para baixo com
o foco na alça. A ordem define como as categorias aparecem no formulário de
receitas.
Novas categorias entram no fim da lista. Remover uma categoria também tira
a tag das receitas associadas, que continuam disponíveis.

Qualquer usuário logado pode vincular até cinco categorias existentes às suas
receitas. O banco aplica a unicidade pelo nome normalizado, inclusive quando
duas criações concorrem.

Em **Receitas** e **Minhas Receitas**, a busca encontra trechos no título, nas
categorias e no nome de usuário do autor sem diferenciar acentos ou maiúsculas.
Cada palavra deve aparecer em pelo menos um desses campos. A busca
aceita até 100 caracteres e as listas mostram 12 receitas por página.
Você também pode iniciar uma busca clicando em uma tag.

Na API, as listas aceitam `busca` e `pagina`, com a primeira página em
`0`. O cabeçalho `X-Tem-Proxima-Pagina` indica se há mais resultados.

## Pessoas e mensagens

Na aba **Pessoas**, pesquise pelo nome de usuário e siga ou deixe de seguir
alguém. Quando duas pessoas se seguem, elas se tornam amigas e uma conversa
direta aparece em **Mensagens**. Se uma deixar de seguir a outra, a conversa
é desativada. Se voltarem a ser amigas, a mesma conversa reaparece com o
histórico.

Em **Mensagens**, você pode enviar texto e imagens, buscar no histórico e
criar grupos. Cada grupo admite até 256 membros, e os administradores do
grupo podem adicionar apenas os próprios amigos. O autor pode editar ou
apagar uma mensagem em até 15 minutos.

## Como configurar o primeiro administrador

Crie uma conta pela tela **Criar conta**. Em seguida, defina
`RECEITOPEDIA_ADMIN_EMAIL` com o endereço dessa conta e reinicie a aplicação.
No PowerShell, execute na raiz do projeto:

```powershell
$env:RECEITOPEDIA_ADMIN_EMAIL = Read-Host "E-mail da conta já cadastrada"
docker compose up -d --force-recreate receitopedia
```

Ao iniciar, a aplicação promove a conta existente no banco. Saia e entre
novamente para carregar o papel `ADMIN` e acessar **Categorias** no menu.
A promoção é persistente, então você pode remover a variável depois.
Novos cadastros recebem o papel `USER`.

## Arquitetura e consistência

O [modelo de domínio](receitopedia.cml) cobre Contas, Receitas, Pessoas e
Comunicação. Os módulos de domínio centralizam as regras, `aplicacao` reúne
os casos de uso e
`infraestrutura` usa JPA e JDBC para persistir os dados no PostgreSQL. O Flyway
aplica as migrações do banco. A apresentação usa Spring Boot e Angular.

O Nginx distribui requisições entre duas réplicas web. Elas compartilham o
PostgreSQL, que também guarda as sessões, e o volume de fotos e anexos. Ao
enviar uma mensagem, a aplicação grava a mensagem e um evento na mesma
transação. O RabbitMQ entrega o evento ao worker, que processa os anexos,
gera miniaturas em paralelo quando há mais de uma imagem elegível e publica a
atualização que chega ao navegador por SSE.

O cadastro normaliza o e-mail e usa uma chave em minúsculas para comparar
nomes de usuário. Restrições `UNIQUE` decidem cadastros concorrentes no
PostgreSQL, e a API informa o campo que entrou em conflito. As senhas são
armazenadas com BCrypt. Spring Security gerencia a sessão e a proteção CSRF.

A API devolve `409 Conflict` quando uma edição ou remoção usa uma versão
desatualizada. As fotos são identificadas por UUID e guardadas fora da pasta
estática, em `./dados/fotos` por padrão. É possível definir
`RECEITOPEDIA_FOTOS_DIR`. O Compose usa um volume dedicado. Arquivos retirados
são enfileirados no banco na mesma transação da receita e apagados após o
commit. Exclusões pendentes são reprocessadas na inicialização.

O teste de cadastros simultâneos envia duas requisições concorrentes para
verificar a garantia de unicidade do banco.

## Build e execução

Pré-requisitos para gerar a imagem e executar o sistema com Compose:

- JDK 17 e Maven
- Node.js e npm (versões usadas no build original: Node 24.11.0 e npm 11.6.2)
- Docker e Docker Compose com suporte a `include`

Na raiz do repositório, execute:

```sh
mvn clean install
docker compose up
```

Abra <http://localhost:8080/receitas>. As rotas da API começam com `/backend`,
e o Swagger UI fica em <http://localhost:8080/swagger-ui/index.html>.

Para compilar sem gerar a imagem Docker, use `mvn clean verify`. O build executa
os cenários Cucumber de gerenciamento de receitas e os testes de contas,
fotos, categorias, Pessoas e Mensagens em bancos de teste isolados.
