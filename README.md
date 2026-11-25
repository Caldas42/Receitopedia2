# Receitopédia

Base de desenvolvimento Java e Angular organizada em módulos Maven. O código reutilizável desta base deriva do exemplo de DDD do professor, licenciado sob a GNU AGPL v3 conforme o arquivo LICENSE.

## Estrutura

- `pai` centraliza a configuração Maven.
- `dominio-acervo`, `dominio-administracao`, `dominio-analise` e `dominio-compartilhado` organizam as camadas de domínio.
- `aplicacao` reúne os casos de uso e `infraestrutura` concentra persistência, migrações e serviços técnicos.
- `apresentacao-backend`, `apresentacao-frontend` e `apresentacao-vaadin` mantêm as entradas Spring Boot, Angular e Vaadin.

## Compilar e executar

Com Java, Maven, Node.js, npm, Docker e Docker Compose instalados, rode na raiz:

```sh
mvn clean install
docker compose -f docker-compose-receitopedia.yml up
```

A aplicação fica em http://localhost:8080. O frontend inicial é uma estrutura Angular com roteamento preparado para as próximas telas.

Para compilar sem gerar a imagem Docker, use `mvn clean verify`. Para iniciar somente o PostgreSQL de desenvolvimento, use `docker compose up postgres`.
