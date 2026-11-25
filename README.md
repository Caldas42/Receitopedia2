# Receitopédia

Rede social para compartilhar receitas. A implementação deste módulo permite adicionar,
listar, visualizar, editar e remover receitas.

## Gerenciamento de receitas

Uma **receita** tem **título**, **descrição** opcional, **ingredientes** e **modo de preparo**.
Cada linha dos ingredientes contém um item e pode incluir sua quantidade. No modo de preparo,
cada linha é uma **etapa**, mantida na ordem em que foi cadastrada. A **versão da receita**
identifica a edição aberta por quem usa o formulário.

O contexto de Receitas está modelado em `receitopedia.cml`. O módulo `dominio-receitas`
valida os dados, `aplicacao` coordena os casos de uso, `infraestrutura` implementa
persistência PostgreSQL via JPA e Flyway, e os módulos de apresentação expõem a API REST
e a interface Angular.

O banco aplica as alterações em transações e usa controle otimista de versão nas edições.
A API devolve `409 Conflict` quando uma edição ou remoção usa uma versão desatualizada.
O CRUD pode ser demonstrado localmente sem login. O controle de acesso depende da integração
com a funcionalidade de autenticação do projeto.

## Build e execução

Pré-requisitos para gerar a imagem e executar o sistema com Compose:

- JDK 17 e Maven
- Node.js e npm (versões usadas no build original: Node 24.11.0 e npm 11.6.2)
- Docker e Docker Compose

Na raiz do repositório, execute:

```sh
mvn clean install
docker compose -f docker-compose-receitopedia.yml up
```

Abra <http://localhost:8080/receitas>. A API está em `/backend/receitas`,
e o Swagger UI fica em <http://localhost:8080/swagger-ui/index.html>.

Para compilar sem gerar a imagem Docker, use `mvn clean verify`. O build executa
os cenários Cucumber de gerenciamento de receitas, incluindo duas edições concorrentes,
em um banco de testes isolado.
