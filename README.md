# 🌱 EcoFlux — Plataforma de Gestão de Reciclagem, Logística Reversa e Impacto Ambiental

O EcoFlux é uma API RESTful desenvolvida em Java 21 e Spring Boot para conectar geradores de resíduos recicláveis (cidadãos, condomínios e empresas), pontos de entrega voluntária (PEVs) e cooperativas de reciclagem. O sistema gerencia todo o ciclo operacional da coleta seletiva — desde o agendamento e roteirização de motoristas até a comprovação digital em campo e o cálculo automatizado de indicadores ambientais (EcoImpacto e EcoScore).

---

## Índice

- [Funcionalidades Principais](#-funcionalidades-principais)
- [Arquitetura e Tecnologias](#-arquitetura-e-tecnologias)
- [Perfis de Acesso e Segurança](#-perfis-de-acesso-e-segurança)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Passo a Passo para Executar o Projeto](#-passo-a-passo-para-executar-o-projeto)
- [Principais Endpoints da API](#-principais-endpoints-da-api)
- [Versionamento de Banco de Dados (Flyway)](#-versionamento-de-banco-de-dados-flyway)

---

## Funcionalidades Principais

- Autenticação e Controle de Acesso (RBAC): Cadastro de usuários, autenticação via tokens **JWT** e controle granular por perfil (`CIDADAO`, `SINDICO`, `COOPERATIVA`, `MOTORISTA`, `ADMINISTRADOR`).
- Gestão de Instituições e Vínculos de Membros:
  - Cadastro e fluxo de aprovação administrativa de Condomínios, Empresas e Cooperativas.
  - Vinculação de moradores e motoristas por meio de validação cruzada entre CPF e e-mail, impedindo associações indevidas e restringindo o papel de `MOTORISTA` exclusivamente a cooperativas.
- Pontos de Coleta (PEVs): Mapeamento georreferenciado (latitude/longitude), horários de funcionamento, materiais aceitos e moderação de status.
- Ciclo Completo de Coletas e Logística:
  - Agendamento de coletas com janela de horário, tipo de material e estimativa de peso.
  - Máquina de estados controlada (`ENVIADA` ➔ `AGENDADA` ➔ `EM_ROTA` ➔ `CONCLUIDA` / `CANCELADA`) com linha do tempo auditável (`HistoricoStatusColeta`).
  - Escalação de motoristas pela cooperativa, consulta de rota ativa (`/minha-rota`) e conclusão em campo com aferição de peso real (kg), identificação do recebedor e assinatura digital.
- Sustentabilidade e Gamificação (EcoImpacto & EcoScore):
  - Cálculo automático de impacto ambiental com base no peso real coletado (emissões de $\text{CO}_2$ evitadas, economia de energia elétrica e água).
  - Pontuação e ranking institucional para condomínios e empresas, acessível a síndicos e moradores vinculados.

---

## Arquitetura e Tecnologias

| Camada / Recurso | Tecnologia |
| :--- | :--- |
| Linguagem & Runtime| Java 21 (compatível com JDK 21+) |
| Framework Principal| Spring Boot 3.5.16 |
| Segurança | Spring Security + Auth0 Java JWT |
| Persistência & ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway Core (`V1` a `V10`) |
| Banco de Dados (Dev) | H2 Database (armazenamento em arquivo local) |
| Banco de Dados (Nuvem) | MySQL 8 (Aiven Cloud) |
| Documentação API | Springdoc OpenAPI (Swagger UI) |
| Build & Automação | Maven Wrapper (`mvnw`) + Lombok |

---

## Perfis de Acesso e Segurança

A API protege as rotas por autenticação Bearer JWT e valida regras de propriedade e vínculo institucional na camada de serviço:

- Administrador (`ADMINISTRADOR`): Aprova ou suspende instituições e pontos de coleta e supervisiona operações globais.
- Síndico / Gestor Institucional (`SINDICO`): Gerencia dados da instituição, vincula moradores e acompanha coletas e indicadores ambientais do condomínio/empresa.
- Gestor de Cooperativa (`COOPERATIVA`): Aceita solicitações de coleta, vincula motoristas à cooperativa e realiza a escalação de rotas.
- Motorista (`MOTORISTA`): Visualiza suas coletas designadas em rota e registra a entrega concluída com peso real e assinatura digital.
- Cidadão / Morador (`CIDADAO`): Solicita coletas residenciais, consulta pontos de coleta próximos e acompanha métricas ambientais do condomínio ao qual está vinculado.

---

## Estrutura do Projeto

```text
src/main/java/com/example/EcoFlux
├── configuracao/        # Filtros JWT, Spring Security, CORS, OpenAPI e Seeder Admin
├── controller/          # Controladores REST (Auth, Coletas, Instituições, Membros, Pontos, EcoImpacto, EcoScore)
├── dto/                 # Records de Request e Response com Bean Validation
├── entity/              # Entidades JPA e Enums de domínio
├── exception/           # Tratamento global de exceções (ControllerAdvice) e respostas padronizadas
├── repository/          # Interfaces Spring Data JPA
└── service/             # Contratos e implementações das regras de negócio

## Pré Requisitos
JDK 21 
Git

## Clonagem do repositório
git clone [https://github.com/Heverald0/EcoFlux.git](https://github.com/Heverald0/EcoFlux.git)

## Comando de inicialização
.\mvnw.cmd spring-boot:run
