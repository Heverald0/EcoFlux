"""As 10 telas internas do EcoFlux.

Cada tela monta o layout base e preenche o conteudo com os componentes de
``ui_kit`` e os dados de ``mocks``.
"""

from __future__ import annotations

from html import escape

from nicegui import ui

import mocks as m
from ui_kit import (
    PRIMARY, bar_chart, card, chip_select, data_table, donut, donut_with_legend,
    dot_steps, form_field, grid, h, icon, item_list, kpi_row, layout, metric_list,
    eco_button, numbered_steps, page_header, progress_footer, rank_list,
    stacked_metric_list, timeline,
)


def _obrigatorios(**campos) -> bool:
    """Valida campos obrigatorios e avisa quais faltam."""
    faltando = [nome for nome, campo in campos.items() if not str(campo.value or '').strip()]
    if faltando:
        ui.notify(f'Preencha: {", ".join(faltando)}.', type='warning')
        return False
    return True


@ui.page('/dashboard')
def dashboard() -> None:
    with layout('/dashboard'):
        page_header('Visão Geral', 'Resumo das atividades de reciclagem - Abril 2026')
        kpi_row(m.KPIS_DASHBOARD)

        with grid('2'):
            with card('Materiais coletados - 05 meses'):
                bar_chart(m.MESES_5)
            with card('Solicitações recentes', more='Ver mais'):
                data_table(m.SOLICITACOES_RECENTES)

        with grid('3'):
            with card('Ranking de instituições', cls='card-sm'):
                rank_list(m.RANKING_DASHBOARD)
            with card('Notificações', cls='card-sm'):
                item_list(m.NOTIFICACOES)
            with card('Tipos de material', cls='card-sm'):
                metric_list(m.TIPOS_MATERIAL)


@ui.page('/pontos-de-coleta')
def pontos_de_coleta() -> None:
    with layout('/pontos-de-coleta'):
        page_header('Pontos de coleta',
                    'Gerencie e visualize os pontos de descarte cadastrados na plataforma')

        with grid('2'):
            with card():
                with ui.element('div').classes('map-box'):
                    h(icon('map') +
                      '<div style="font-size:14px;font-weight:500">Mapa interativo de pontos</div>'
                      '<div class="muted" style="font-size:11px">34 pontos ativos na região</div>'
                      '<div style="font-size:11px">• Papel/Plástico • Eletrônico • Metal/Vidro</div>')
                    eco_button('Planejar rota',
                               lambda: ui.notify('Rota planejada com 34 pontos ativos.',
                                                 type='positive')
                               ).style('width:130px;margin-top:10px')

            with card('Cadastrar novo ponto'):
                with ui.element('div').classes('form-grid'):
                    nome = form_field('Nome do ponto',
                                      placeholder='Ex: Ecoponto Jardim de Eunápolis')
                    endereco = form_field('Endereço completo',
                                          placeholder='Rua, número, bairro, cidade')
                    with ui.element('div').classes('form-grid cols-2'):
                        responsavel = form_field('Responsável:', placeholder='Nome')
                        horarios = form_field('Horários', placeholder='Seg - Sex 8h à 17h')
                    with ui.element('div'):
                        h('<label class="form-label">Materiais aceitos</label>')
                        materiais = chip_select(m.MATERIAIS_ACEITOS,
                                                m.MATERIAIS_PRE_SELECIONADOS)

                    def cadastrar() -> None:
                        if not _obrigatorios(nome=nome, endereço=endereco,
                                             responsável=responsavel):
                            return
                        if not materiais:
                            ui.notify('Selecione ao menos um material aceito.', type='warning')
                            return
                        ui.notify(f'Ponto "{nome.value}" cadastrado com sucesso!',
                                  type='positive')

                    eco_button('Cadastrar ponto', cadastrar, cls='dark eco-block')

        with card('Pontos cadastrados'):
            data_table(m.PONTOS_CADASTRADOS, action='Editar')


@ui.page('/agendamentos')
def agendamentos() -> None:
    with layout('/agendamentos'):
        page_header('Agendamento de retirada',
                    'Solicite a coleta seletiva e acompanhe o status da solicitação')

        with grid('2'):
            with card('Nova solicitação'):
                with ui.element('div').classes('form-grid'):
                    form_field('Instituição solicitante', options=m.INSTITUICOES,
                               value=m.INSTITUICOES[0])
                    material = form_field('Tipo do material', value='Papel e papelão')
                    with ui.element('div').classes('form-grid cols-2'):
                        data = form_field('Data preferencial', props='type=date')
                        form_field('Horários', options=m.JANELAS_HORARIO,
                                   value=m.JANELAS_HORARIO[0])
                    with ui.element('div').classes('form-grid cols-2'):
                        quantidade = form_field('Quantidade estimada', value='50',
                                                props='suffix=KG type=number min=1')
                        form_field('Cooperativa', options=m.COOPERATIVAS,
                                   value=m.COOPERATIVAS[0])
                    form_field('Observações', placeholder='Informações disponíveis...')

                    def enviar() -> None:
                        if _obrigatorios(material=material, data=data, quantidade=quantidade):
                            ui.notify('Solicitação enviada! Acompanhe o status ao lado.',
                                      type='positive')

                    eco_button('Enviar Solicitação', enviar, cls='dark eco-block')

            with card(f'Status da solicitação # {m.SOLICITACAO_ID}'):
                timeline(m.SOLICITACAO_ETAPAS)

        with ui.element('div').classes('narrow'):
            with card('Históricos de coletas'):
                data_table(m.HISTORICO_COLETAS)


@ui.page('/materiais')
def materiais() -> None:
    with layout('/materiais'):
        page_header('Controle de materiais recicláveis',
                    'Registro, rastreamento e situação de todos os resíduos processados')
        kpi_row(m.KPIS_MATERIAIS)

        with card('Registrar entrada de material'):
            with ui.element('div').classes('form-grid cols-4'):
                form_field('Tipo', options=m.TIPOS_REGISTRO, value=m.TIPOS_REGISTRO[0],
                           upper=True)
                peso = form_field('Peso/Litros', value='0,00', upper=True)
                form_field('Volume', value='0,00', upper=True)
                data = form_field('Data', upper=True, props='type=date')
                origem = form_field('Origem', value='COND. PARQUE VERDE', upper=True)
                form_field('Destino', options=m.DESTINOS, value=m.DESTINOS[0], upper=True)
                form_field('Situação', options=m.SITUACOES, value=m.SITUACOES[0], upper=True)

                def registrar() -> None:
                    if peso.value in ('', '0,00', None):
                        ui.notify('Informe o peso ou o volume em litros.', type='warning')
                        return
                    if _obrigatorios(origem=origem, data=data):
                        ui.notify('Entrada de material registrada!', type='positive')

                with ui.element('div').classes('cell-end'):
                    eco_button('REGISTRAR', registrar, cls='eco-block')

        with card('Últimos registros', more='Ver mais'):
            data_table(m.ULTIMOS_REGISTROS)


@ui.page('/ecoscore')
def ecoscore() -> None:
    with layout('/ecoscore'):
        page_header('EcoScore',
                    'Pontuação por descarte correto - acompanhe seu desempenho e evolua')

        with grid('2'):
            with card('Seu Score - Cond. Parque Verde'):
                anel = donut([m.Slice('Score', m.SCORE_PCT, '#4ADE80'),
                              m.Slice('Restante', 100 - m.SCORE_PCT, '#D9D9D9')],
                             size=72, thickness=22)
                h(f'''<div style="display:flex;align-items:center;gap:20px;margin-bottom:18px">
                        {anel}
                        <div>
                          <div style="font-size:18px;font-weight:600">{m.SCORE_PONTOS}</div>
                          <div class="muted" style="font-size:8px">Cond. Parque Verde</div>
                          <div class="badge b-gold" style="margin:6px 0">EcoSelo Ouro</div>
                          <div class="muted" style="font-size:8px">
                            +160 pts para Referência Sustentável</div>
                        </div>
                      </div>
                      <div style="font-size:10px;margin-bottom:4px">Critérios de pontuação</div>''')
                metric_list(m.CRITERIOS_PONTUACAO)

            with card():
                with ui.element('div').classes('card-head'):
                    titulo = h('<div class="card-title">Ranking geral</div>')
                    mes = ui.select(m.MESES_RANKING, value=m.MESES_RANKING[0]).props(
                        'outlined dense hide-bottom-space options-dense aria-label="Mês do ranking"'
                    ).classes('eco-field').style('width:150px')
                    titulo.bind_content_from(
                        mes, 'value', lambda v: f'<div class="card-title">Ranking geral - {v}</div>')
                rank_list(m.RANKING_GERAL, divided=True, top_colored=2, green_value=True,
                          footer='58 instituições participando este mês')

        with ui.element('div').classes('narrow-45'):
            with card('Materiais coletados - 05 meses'):
                bar_chart(m.MESES_5)


@ui.page('/ecoselo')
def ecoselo() -> None:
    with layout('/ecoselo'):
        page_header('Certificação ambiental',
                    'Selos de desempenho para reconhecer instituições sustentáveis')
        kpi_row(m.KPIS_ECOSELO)

        with grid('2'):
            with card():
                h(f'''<div style="display:flex;align-items:center;justify-content:space-between;
                                 gap:16px;margin-bottom:18px">
                        <div>
                          <div style="color:{PRIMARY};font-size:16px;font-weight:600">
                            EcoSelo Ouro</div>
                          <div style="font-size:12px;font-weight:500">Cond. Parque Verde</div>
                          <div class="muted" style="font-size:11px;margin-top:14px">
                            Validade: abril/2026 a abril/2027</div>
                        </div>
                        <div class="seal"><div class="seal-inner">Eco</div></div>
                      </div>''')
                progress_footer('Critérios atendidos', 92)

            with card('Painel de certificação'):
                data_table(m.PAINEL_CERTIFICACAO)

        with grid('3'):
            with card('Critérios do selo', cls='card-sm'):
                metric_list(m.CRITERIOS_SELO, alternate=True, bar_width='60px')
            with card('Ações recomendadas', cls='card-sm'):
                item_list(m.ACOES_RECOMENDADAS, divided=False)
            with card('Etapas de certificação', cls='card-sm'):
                dot_steps(m.ETAPAS_CERTIFICACAO)


@ui.page('/ecoimpacto')
def ecoimpacto() -> None:
    with layout('/ecoimpacto'):
        page_header('Impacto ambiental',
                    'Indicadores de CO2, economia de recursos e resultados sustentáveis')
        kpi_row(m.KPIS_ECOIMPACTO)

        with grid('2'):
            with card('Evolução de impacto - 05 meses',
                      subtitle='CO₂ evitado, água e energia poupada em escala agregada'):
                bar_chart(m.MESES_IMPACTO)
            with card('Distribuição por material'):
                donut_with_legend(m.DISTRIBUICAO_MATERIAL)

        with grid('3'):
            with card('Metas ESG', cls='card-sm'):
                stacked_metric_list(m.METAS_ESG)
            with card('Equivalências ambientais', cls='card-sm'):
                item_list(m.EQUIVALENCIAS, big_title=True)
            with card('Impacto por instituição', cls='card-sm'):
                metric_list(m.IMPACTO_INSTITUICAO)


@ui.page('/relatorios')
def relatorios() -> None:
    with layout('/relatorios'):
        page_header('Relatórios operacionais e ambientais',
                    'Gere documentos para gestão logística, ESG e prestação de contas')
        kpi_row(m.KPIS_RELATORIOS)

        with card('Gerar novo relatório'):
            with ui.element('div').classes('form-grid cols-4'):
                periodo = form_field('Período', options=m.PERIODOS, value=m.PERIODOS[0],
                                     white=True)
                instituicao = form_field('Instituição', options=['Todas'] + m.INSTITUICOES,
                                         value='Todas', white=True)
                tipo = form_field('Tipo', options=m.TIPOS_RELATORIO,
                                  value=m.TIPOS_RELATORIO[0], white=True)
                with ui.element('div').classes('cell-end'):
                    eco_button(
                        'Gerar relatório',
                        lambda: ui.notify(
                            f'Relatório {tipo.value} ({periodo.value} · {instituicao.value}) '
                            'em geração.', type='positive'),
                        cls='eco-block')

        with grid('65'):
            with card('Relatórios recentes'):
                data_table(m.RELATORIOS_RECENTES, divided=True, tall=True)
            with card('Resumo do mês'):
                linhas = ''.join(
                    f'<div class="item" style="display:block">'
                    f'<div class="muted" style="font-size:9px">{escape(label)}</div>'
                    f'<div style="font-size:16px;{"color:" + PRIMARY if green else ""}">'
                    f'{escape(valor)}</div></div>'
                    for label, valor, green in m.RESUMO_MES
                )
                h(f'<div class="list-divided">{linhas}</div>'
                  '<div class="muted" style="font-size:9px;margin-top:10px">'
                  'Dados prontos para apresentação final do projeto.</div>')


@ui.page('/area-educativa')
def area_educativa() -> None:
    with layout('/area-educativa'):
        page_header('Trilhas de educação ambiental',
                    'Conteúdos para orientar o descarte correto e aumentar a participação')
        kpi_row(m.KPIS_EDUCATIVA)

        with grid('2'):
            with card('Conteúdos em destaque'):
                cores = {'green': '#DCFCE7', 'blue': '#DBEAFE', 'amber': '#FEF3C7'}
                linhas = ''.join(
                    f'<div class="content-row">'
                    f'<span class="thumb" style="background:{cores[tone]}"></span>'
                    f'<div style="flex:1;min-width:0">'
                    f'<div class="item-title">{escape(titulo)}</div>'
                    f'<div class="item-meta" style="font-size:7px">{escape(desc)}</div></div>'
                    f'<div class="track" style="max-width:110px">'
                    f'<div class="fill" style="width:{pct}%"></div></div>'
                    f'<span style="font-size:9px;width:26px;text-align:right">{pct}%</span></div>'
                    for tone, titulo, desc, pct in m.CONTEUDOS_DESTAQUE
                )
                h(linhas)

            with card('Quiz EcoScore'):
                h('<div class="muted" style="font-size:10px;margin:-8px 0 12px">'
                  'Teste seus conhecimentos e ganhe pontos por boas práticas.</div>'
                  f'<div class="quiz-box">{escape(m.QUIZ_PERGUNTA)}</div>')
                with ui.element('div').classes('chips').style('margin-top:14px'):
                    botoes: list[ui.button] = []

                    def responder(opcao: str, botao: ui.button) -> None:
                        for outro in botoes:
                            outro.classes(remove='on')
                        botao.classes(add='on')
                        certo = opcao == m.QUIZ_CORRETA
                        ui.notify('Correto! +20 pontos EcoScore.' if certo
                                  else 'Não é esse. Tente outra opção.',
                                  type='positive' if certo else 'warning')

                    for opcao in m.QUIZ_OPCOES:
                        botao = eco_button(
                            opcao, cls=f'outline{" on" if opcao == m.QUIZ_CORRETA else ""}')
                        botao.on_click(lambda o=opcao, b=botao: responder(o, b))
                        botoes.append(botao)

        with grid('3'):
            with card('Campanhas ativas', cls='card-sm'):
                item_list(m.CAMPANHAS_ATIVAS)
            with card('Materiais para baixar', cls='card-sm'):
                item_list(m.MATERIAIS_DOWNLOAD)
            with card('Trilha recomendada', cls='card-sm'):
                numbered_steps(m.TRILHA_RECOMENDADA)


@ui.page('/administracao')
def administracao() -> None:
    with layout('/administracao'):
        page_header('Usuários e instituições',
                    'Controle de perfis, permissões e cadastro institucional')
        kpi_row(m.KPIS_ADMIN)

        with grid('65'):
            with card('Usuários e responsáveis'):
                data_table(m.USUARIOS, divided=True, tall=True)

            with ui.element('div').classes('stack'):
                with card('Cadastros em análise', cls='card-sm'):
                    linhas = ''.join(
                        f'<div class="item"><div style="flex:1">'
                        f'<div class="item-title">{escape(nome)}</div>'
                        f'<div class="item-meta">{escape(tipo)}</div></div>'
                        f'<button class="badge b-amber" style="border:none;cursor:pointer" '
                        f'onclick="ecoToast(\'Análise de {escape(nome)} aberta.\')">'
                        f'Analisar</button></div>'
                        for nome, tipo in m.CADASTROS_EM_ANALISE
                    )
                    h(f'<div class="list-divided">{linhas}</div>')

                with card('Perfis de acesso', cls='card-sm'):
                    stacked_metric_list(m.PERFIS_ACESSO)
