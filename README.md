# Receitopédia

Rede social para compartilhar receitas. A implementação deste módulo permite adicionar,
listar, visualizar, editar e remover receitas.

## Contas e receitas

Em **Criar conta**, informe e-mail, nome de usuário e senha. Depois de entrar,
o menu do usuário fica no canto superior direito. A tela **Receitas** é pública
e mostra todas as receitas. **Minhas Receitas** filtra apenas as suas. As duas
telas dão acesso ao cadastro de uma receita.

Uma **receita** exige **ingredientes** e **modo de preparo**, com um item ou uma
etapa por linha. Título, descrição, tempo de preparo e fotos são opcionais.
Ao informar somente horas ou somente minutos, a outra parte vale zero.
A **foto principal** aparece na lista. A visualização também mostra as
fotos secundárias e o nome do **autor**. Até cinco imagens JPEG, PNG ou WebP
de até 5 MB cada podem ser enviadas.

A edição e a exclusão exigem a sessão do autor. Receitas criadas antes da
migração continuam na lista pública como **Receita sem autor associado**.
O banco original não registrava o autor, portanto essas receitas ficam
disponíveis apenas para leitura.

## Categorias e busca

Em **Categorias**, administradores podem criar, editar, remover e reorganizar
as categorias com os botões **Subir** e **Descer**. A ordem é salva a cada
movimento e define como as categorias aparecem no formulário de receitas.
Novas categorias entram no fim da lista. Remover uma categoria também tira
a tag das receitas associadas, que continuam disponíveis.

Qualquer usuário logado
pode vincular até cinco categorias existentes às suas receitas. O banco aplica
a unicidade pelo nome normalizado, inclusive quando duas criações concorrem.

Em **Receitas** e **Minhas Receitas**, a busca encontra trechos no título e
nas categorias sem diferenciar acentos ou maiúsculas. Cada palavra deve
aparecer no título ou em pelo menos uma das categorias da receita. A busca
aceita até 100 caracteres e as listas mostram 12 receitas por página.
Você também pode iniciar uma busca clicando em uma tag.

Na API, as listas aceitam `busca` e `pagina`, com a primeira página em
`0`. O cabeçalho `X-Tem-Proxima-Pagina` indica se há mais resultados.

## Como configurar o primeiro administrador

Crie uma conta pela tela **Criar conta**. Em seguida, defina
`RECEITOPEDIA_ADMIN_EMAIL` com o endereço dessa conta e reinicie a aplicação.
No PowerShell, execute na raiz do projeto:

```powershell
$env:RECEITOPEDIA_ADMIN_EMAIL = Read-Host "E-mail da conta já cadastrada"
docker compose -f docker-compose-receitopedia.yml up -d --force-recreate receitopedia
```

Ao iniciar, a aplicação promove a conta existente no banco. Saia e entre
novamente para carregar o papel `ADMIN` e acessar **Categorias** no menu.
A promoção é persistente, então você pode remover a variável depois.
Novos cadastros recebem o papel `USER`.

## Arquitetura e consistência

Os contextos de Contas e Receitas estão em `receitopedia.cml`. Os módulos de
domínio centralizam as regras, `aplicacao` reúne os casos de uso e
`infraestrutura` implementa persistência PostgreSQL via JPA e Flyway. A
apresentação usa Spring Boot e Angular.

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

A aplicação não cria pools de threads para cadastro ou upload. O teste de
cadastros simultâneos envia duas requisições concorrentes para comprovar a
garantia do banco.

## Build e execução

Pré-requisitos para gerar a imagem e executar o sistema com Compose:

- JDK 17 e Maven
- Node.js e npm (versões usadas no build original: Node 24.11.0 e npm 11.6.2)
- Docker e Docker Compose

Na raiz do repositório, execute:

```sh
mvn clean install
docker compose up
```

Abra <http://localhost:8080/receitas>. A API está em `/backend/receitas`,
e o Swagger UI fica em <http://localhost:8080/swagger-ui/index.html>.

Para compilar sem gerar a imagem Docker, use `mvn clean verify`. O build executa
os cenários Cucumber de gerenciamento de receitas e os testes de cadastro,
sessão e fotos em um banco de testes isolado.
