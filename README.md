# Zelda Tears of the Kingdom - Tradução PT-BR

O objetivo deste projeto é fornecer uma tradução completa para o Português Brasileiro do jogo Zelda Tears of the Kingdom. Este trabalho é realizado em duas partes principais: `jmsbt` e `translate-msbt`.

## Sumário
1. [jmsbt](#jmsbt)
2. [translate-msbt](#translate-msbt)
3. [Contribuindo](#contribuindo)
4. [Download da tradução](#download-da-tradução)

## jmsbt <a name="jmsbt"></a>

`jmsbt` é uma aplicação em Java 17, com gerenciamento de dependências via Gradle. Sua função é manipular arquivos MSBT, convertendo-os para YAML e também realizando a operação inversa. Essa ferramenta é essencial na pipeline de tradução.

### Configuração e Uso

Para configurar e usar o projeto `jmsbt`, siga estas etapas:

1. Navegue até a pasta do projeto usando `cd jmsbt`.
2. Execute o comando `./gradlew clean publishToMavenLocal` para instalar localmente usando `mavenPublishToLocal`. Isso preparará o `jmsbt` para ser utilizado pelo `translate-msbt`.

## translate-msbt <a name="translate-msbt"></a>

`translate-msbt` é um projeto que utiliza o `jmsbt` para carregar arquivos via YAML. Estes arquivos são então inseridos em um banco de dados Postgres e traduzidos utilizando a API do GPT.

### Configuração e Uso

Por enquanto, este projeto não está configurado para ser executado em um ambiente de produção. Para configurar e usar o projeto `translate-msbt` é preciso modificar o arquivo `application.yml`(pendente) e adicionar as credenciais do banco de dados e da API do GPT diretamente no código.

## Contribuindo <a name="contribuindo"></a>

Estamos abertos para colaborações! Agradecemos qualquer tipo de contribuição! Para contribuir, por favor:

1. Faça o Fork do projeto
2. Crie a sua nova branch (`git checkout -b feature/AmazingFeature`)
3. Faça o commit das suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Envie para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## Download da tradução <a name="download-da-tradução"></a>

A versão mais recente da tradução pode ser baixada clicando [aqui](build%2FMod%20-%20PT-BR%20Tradu%E7%E3o.zip).