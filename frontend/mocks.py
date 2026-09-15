"""Dados mockados do EcoFlux (referencia: abril/2026).

Tudo aqui e estatico e tipado - trocar por chamadas de API sem tocar nas telas.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Kpi:
    label: str
    value: str
    hint: str = ''
    highlighted: bool = False
    hint_plain: bool = False  # hint em cor de texto normal, nao verde


@dataclass(frozen=True)
class Badge:
    """Celula de tabela renderizada como pilula de status."""
    text: str
    tone: str = ''  # vazio = deduz o tom pelo texto (ver ui_kit.TONES)


Cell = str | Badge


@dataclass(frozen=True)
class Table:
    columns: list[str]
    rows: list[list[Cell]]


@dataclass(frozen=True)
class Bar:
    """Barra do grafico mensal (altura em % da area do grafico)."""
    label: str
    pct: int


@dataclass(frozen=True)
class Metric:
    """Linha 'rotulo | barra de progresso | valor'."""
    label: str
    value: str
    pct: int


@dataclass(frozen=True)
class Item:
    """Linha de lista: marcador + titulo + meta.

    O marcador e um icone (``icon``), uma extensao de arquivo (``ext``) ou,
    na ausencia dos dois, um circulo colorido conforme ``tone``.
    """
    title: str
    meta: str
    tone: str = 'green'
    icon: str = ''
    ext: str = ''


@dataclass(frozen=True)
class Step:
    """Etapa de timeline/stepper."""
    title: str
    meta: str
    done: bool = False
    partial: bool = False


@dataclass(frozen=True)
class Slice:
    label: str
    pct: int
    color: str


USER_NAME = 'Luca Soares'
USER_ROLE = 'administrador'
USER_SCORE = 'EcoScore: 1.840 pontos'

MESES_5 = [Bar('Dez', 40), Bar('Jan', 50), Bar('Fev', 62), Bar('Mar', 74), Bar('Abr', 88)]

KPIS_DASHBOARD = [
    Kpi('Total reciclado (kg)', '8.240', '12% vs mês anterior', highlighted=True),
    Kpi('Coletas realizadas', '147', '+8 essa semana'),
    Kpi('Pontos ativos', '34', '03 novos este mês'),
    Kpi('Instituições', '58', 'Cooperativas, empresas...', hint_plain=True),
]

SOLICITACOES_RECENTES = Table(
    ['Instituição', 'Material', 'Status'],
    [
        ['Cond. Parque Verde', 'Plástico', Badge('Agendada')],
        ['Escola Araucária', 'Papel', Badge('Em análise')],
        ['TechCorp LTDA', 'Eletrônico', Badge('Enviada')],
        ['Coop. Recicla BA', 'Metal', Badge('Realizada')],
        ['UFSB', 'Vidro', Badge('Cancelada')],
    ],
)

RANKING_DASHBOARD = [
    Metric('Coop. Recicla SP', '2.430', 75),
    Metric('Cond. Parque Verde', '1.840', 60),
    Metric('Escola Araucária', '1.550', 50),
    Metric('TechCorp LTDA', '1.240', 15),
]

NOTIFICACOES = [
    Item('Coleta agendada p/ amanhã', 'há 01 hora · Cond. Parque Verde',
         tone='muted', icon='notifications'),
    Item('Você subiu para EcoSelo Ouro', 'há 02 dias', tone='amber', icon='light_mode'),
    Item('Coleta de papel realizada', 'há 03 dias · +20 pontos EcoScore',
         tone='green', icon='check'),
]

TIPOS_MATERIAL = [
    Metric('Papel', '3.200kg', 80),
    Metric('Plástico', '2.400kg', 60),
    Metric('Metal', '1.400kg', 40),
    Metric('Vidro', '620kg', 15),
    Metric('Eletrônico', '600kg', 10),
]

MATERIAIS_ACEITOS = ['Papel', 'Plástico', 'Vidro', 'Metal', 'Eletrônico', 'Óleo']
MATERIAIS_PRE_SELECIONADOS = ['Papel', 'Plástico']

PONTOS_CADASTRADOS = Table(
    ['Nome', 'Bairro', 'Responsável', 'Materiais aceitos', 'Status', ''],
    [
        ['EcoPonto Central', 'Centro', 'João Souza', 'Papel, Plástico, Metal', Badge('Ativo')],
        ['Coop. Verde Norte', 'Santana', 'Maria Lima', 'Eletrônico, Metal', Badge('Ativo')],
        ['EcoPonto Sul', 'Ipiranga', 'Carlos Reis', 'Vidro, Papel', Badge('Manutenção')],
        ['Escola Verde', 'Pinheiros', 'Ana Paula', 'Papel, Plástico', Badge('Ativo')],
    ],
)

INSTITUICOES = ['Cond. Parque Verde', 'Escola Araucária', 'TechCorp LTDA',
                'Coop. Recicla BA', 'UFSB']
COOPERATIVAS = ['Qualquer disponível', 'Coop. Recicla BA', 'Coop. Verde Norte',
                'Eco Eletrônicos']
JANELAS_HORARIO = ['08: 00 - 10:00', '10:00 - 12:00', '13:00 - 15:00', '15:00 - 17:00']

SOLICITACAO_ID = '2041'
SOLICITACAO_ETAPAS = [
    Step('Enviada', '08 abr · 09:14', done=True),
    Step('Em análise', '08 abr · 14:30', done=True),
    Step('Agendada', '10 abr · Coletada em 11 de abr', done=True),
    Step('Aguardando', 'Aguardando...'),
    Step('Concluída', 'Aguardando...'),
]

HISTORICO_COLETAS = Table(
    ['Data', 'Material', 'Quantidade', 'Status'],
    [
        ['02 Abr', 'Papel', '80 kg', Badge('Realizada')],
        ['18 Mar', 'Plástico', '45 kg', Badge('Realizada')],
        ['05 Mar', 'Metal', '30 kg', Badge('Realizada')],
        ['12 Fev', 'Óleo', '150 L', Badge('Realizada')],
    ],
)

KPIS_MATERIAIS = [
    Kpi('Papel e papelão', '3.200 KG'),
    Kpi('Plástico', '2.400 KG', highlighted=True),
    Kpi('Metal', '1.400 KG'),
    Kpi('Óleo e derivados', '580 L'),
]

TIPOS_REGISTRO = ['PAPEL', 'PLÁSTICO', 'VIDRO', 'METAL', 'ELETRÔNICO', 'ÓLEO']
DESTINOS = ['COOP. RECICLA BA', 'COOP. VERDE NORTE', 'ECO ELETRÔNICOS',
            'METAL RECICLA', 'GOTA DO ÓLEO']
SITUACOES = ['COLETADO', 'TRIAGEM', 'AGENDADA', 'COMERCIALIZADO']

ULTIMOS_REGISTROS = Table(
    ['Material', 'Peso', 'Volume', 'Data', 'Origem', 'Destino', 'Status'],
    [
        ['Papel', '80 KG', '320 L', '08/04', 'Cond. Parque Verde', 'Coop. Recicla BA',
         Badge('Agendada', 'solid')],
        ['Plástico', '45 KG', '400 L', '07/04', 'TechCorp LTDA', 'Coop. Verde Norte',
         Badge('Triagem')],
        ['Eletrônico', '22 KG', '60 L', '06/04', 'Escola Araucária', 'Eco Eletrônicos',
         Badge('Coletado')],
        ['Metal', '110 KG', '220 L', '05/04', 'Escola Araucária', 'Metal Recicla',
         Badge('Comercializado')],
        ['Óleo', '220 L', '220 L', '04/04', 'Topázio Toyota', 'Gota do Óleo',
         Badge('Triagem')],
    ],
)

SCORE_PCT = 75
SCORE_PONTOS = '1840 pts'
CRITERIOS_PONTUACAO = [
    Metric('Frequência', '+ 680 pts', 77),
    Metric('Separação', '+ 576 pts', 67),
    Metric('Quantidade', '+ 480 pts', 58),
    Metric('Regularidade', '+ 104 pts', 50),
]

MESES_RANKING = ['Abril 2026', 'Março 2026', 'Fevereiro 2026', 'Janeiro 2026']
RANKING_GERAL = [
    Metric('Coop. Recicla BA', '2.340', 100),
    Metric('Cond. Parque Verde', '1.840', 78),
    Metric('Escola Araucária', '1.550', 66),
    Metric('TechCorp LTDA', '1.240', 53),
    Metric('Bradesco', '1.010', 43),
    Metric('Topázio Toyota', '800', 34),
]

KPIS_ECOSELO = [
    Kpi('Instituições certificadas', '42', '+6 este mês', highlighted=True),
    Kpi('Selo ouro', '12', 'acima de 1.800 pts'),
    Kpi('Aderência média', '87%', '+9% vs mês anterior'),
    Kpi('Renovações', '06', 'pendentes em maio'),
]

PAINEL_CERTIFICACAO = Table(
    ['Instituição', 'Score', 'Selo'],
    [
        ['Cond. Parque Verde', '1.840', Badge('Ouro')],
        ['Escola Araucária', '1.550', Badge('Prata')],
        ['TechCorp LTDA', '1.240', Badge('Bronze')],
        ['Coop. Recicla BA', '980', Badge('Análise', 'red')],
        ['UFSB', '820', Badge('Inicial')],
    ],
)

CRITERIOS_SELO = [
    Metric('Frequência de coleta', '+680 pts', 85),
    Metric('Separação correta', '+576 pts', 72),
    Metric('Volume reciclado', '+480 pts', 60),
    Metric('Regularidade', '+104 pts', 30),
]

ACOES_RECOMENDADAS = [
    Item('Registrar evidências de descarte', 'melhora auditoria do EcoSelo', 'green'),
    Item('Convidar novos moradores', '+120 pts de engajamento', 'amber'),
    Item('Agendar coleta de eletrônicos', 'reduz risco ambiental', 'green'),
]

ETAPAS_CERTIFICACAO = [
    Step('Análise de dados', 'concluída', done=True),
    Step('Auditoria', 'em andamento', done=True),
    Step('Emissão do selo', '12 mai'),
    Step('Renovação', 'abr/2027'),
]

KPIS_ECOIMPACTO = [
    Kpi('CO2 evitado', '3,8 t', '+18% em abril', highlighted=True),
    Kpi('Árvores preservadas', '96', 'equivalência estimada'),
    Kpi('Água economizada', '42 mil L', '+7 mil L no mês'),
    Kpi('Energia poupada', '18 MWh', 'por reciclagem'),
]

MESES_IMPACTO = [Bar('Dez', 35), Bar('Jan', 48), Bar('Fev', 62), Bar('Mar', 80),
                 Bar('Abr', 100)]

DISTRIBUICAO_MATERIAL = [
    Slice('Papel', 39, '#1F8A43'),
    Slice('Plástico', 29, '#4ADE80'),
    Slice('Metal', 17, '#DBEAFE'),
    Slice('Vidro', 10, '#FEF3C7'),
    Slice('Eletrônico', 5, '#FEE2E2'),
]

METAS_ESG = [
    Metric('Reduzir descarte incorreto', '78%', 78),
    Metric('Aumentar participação', '64%', 64),
    Metric('Rotas otimizadas', '52%', 52),
    Metric('Relatórios emitidos', '86%', 86),
]

EQUIVALENCIAS = [
    Item('3,8 t', 'de CO2 evitadas', 'green'),
    Item('12 t', 'fora de aterros', 'blue'),
    Item('1.840 km', 'de emissão compensada', 'amber'),
]

IMPACTO_INSTITUICAO = [
    Metric('Cond. Parque Verde', '1,2 t', 100),
    Metric('Escola Araucária', '840 kg', 70),
    Metric('TechCorp LTDA', '690 kg', 57),
    Metric('Coop. Recicla BA', '510 kg', 42),
]

KPIS_RELATORIOS = [
    Kpi('Relatórios gerados', '128', '+24 em abril', highlighted=True),
    Kpi('Exportações PDF', '76', '59% do total'),
    Kpi('Envios agendados', '14', 'próximos 7 dias'),
    Kpi('Pendências', '03', 'revisar dados'),
]

PERIODOS = ['Abril 2026', 'Março 2026', 'Q1/2026', '2026']
TIPOS_RELATORIO = ['ESG + Operacional', 'ESG', 'Operacional', 'Auditoria EcoSelo']

RELATORIOS_RECENTES = Table(
    ['Nome', 'Período', 'Formato', 'Status'],
    [
        ['Mensal ESG', 'Abr/2026', 'PDF', Badge('Enviado')],
        ['Coletas por rota', 'Abr/2026', 'XLSX', Badge('Pronto')],
        ['Materiais reciclados', 'Mar/2026', 'PDF', Badge('Pronto')],
        ['Auditoria EcoSelo', 'Q1/2026', 'PDF', Badge('Análise')],
        ['Instituições ativas', '2026', 'CSV', Badge('Pendente', 'red')],
    ],
)

RESUMO_MES = [
    ('Coletas realizadas', '147', True),
    ('Materiais processados', '8.240 kg', False),
    ('Instituições', '58', False),
    ('Rotas otimizadas', '21', False),
]

KPIS_EDUCATIVA = [
    Kpi('Conteúdos publicados', '32', '+5 esta semana', highlighted=True),
    Kpi('Usuários engajados', '1.240', '67% concluindo trilhas'),
    Kpi('Quizzes respondidos', '890', '+140 em abril'),
    Kpi('Pontos gerados', '18k', 'via educação'),
]

CONTEUDOS_DESTAQUE = [
    ('green', 'Separação correta', 'Guia rápido para plástico, papel, vidro e metal', 82),
    ('blue', 'Coleta seletiva no condomínio', 'Como organizar pontos e horários de descarte', 64),
    ('amber', 'Eletrônicos e óleo', 'Cuidados especiais para resíduos de risco', 47),
]

QUIZ_PERGUNTA = 'Qual material precisa de descarte especial?'
QUIZ_OPCOES = ['Óleo de cozinha', 'Papel limpo', 'Vidro comum']
QUIZ_CORRETA = 'Óleo de cozinha'

CAMPANHAS_ATIVAS = [
    Item('Semana do plástico', '34 instituições', 'green'),
    Item('Dia da coleta consciente', '12 rotas', 'blue'),
    Item('Mutirão de eletrônicos', '05 pontos', 'amber'),
]

MATERIAIS_DOWNLOAD = [
    Item('Cartilha PDF', '12 páginas', ext='PDF'),
    Item('Checklist de coleta', 'modelo editável', ext='DOC'),
    Item('Sinalização de ecoponto', 'impressão A4', ext='PNG'),
]

TRILHA_RECOMENDADA = [
    Step('Separar', '100%', done=True),
    Step('Armazenar', '100%', done=True),
    Step('Agendar', '35%', partial=True),
    Step('Acompanhar', '35%', partial=True),
]

KPIS_ADMIN = [
    Kpi('Usuários ativos', '1.240', '+86 no mês', highlighted=True),
    Kpi('Instituições', '58', '12 aguardando análise'),
    Kpi('Cooperativas', '09', '4 rotas ativas'),
    Kpi('Permissões', '04', 'perfis configurados'),
]

USUARIOS = Table(
    ['Nome', 'Instituição', 'Perfil', 'Status'],
    [
        ['Luca Soares', 'Cond. Parque Verde', 'Admin', Badge('Ativo')],
        ['Ana Clara', 'Escola Araucária', 'Gestor', Badge('Ativo')],
        ['Marcos Lima', 'Coop. Recicla BA', 'Coletor', Badge('Ativo')],
        ['Bianca Reis', 'TechCorp LTDA', 'Analista', Badge('Pendente')],
        ['Arthur Lucas', 'UFSB', 'Gestor', Badge('Bloqueado')],
    ],
)

CADASTROS_EM_ANALISE = [
    ('Mercado Central', 'empresa ESG'),
    ('Residencial Ipê', 'condomínio'),
    ('Coop. Sul Bahia', 'cooperativa'),
]

PERFIS_ACESSO = [
    Metric('Admin', 'todos os módulos', 100),
    Metric('Gestor', 'coletas e relatórios', 75),
    Metric('Coletor', 'rotas e registros', 50),
    Metric('Visitante', 'educação e score', 35),
]
