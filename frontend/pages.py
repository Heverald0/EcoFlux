"""As 10 telas internas do EcoFlux.

Cada tela monta o layout base e preenche o conteudo com os componentes de
``ui_kit`` e os dados de ``mocks``.
"""

from __future__ import annotations

from dataclasses import replace
from datetime import datetime
from html import escape
import re
from typing import Any

from nicegui import ui

from api_client import (
    ApiError,
    agendar_coleta,
    alterar_status_ponto_coleta,
    atualizar_ponto_coleta,
    atualizar_status_coleta,
    atualizar_status_cooperativa,
    atualizar_status_instituicao,
    atualizar_status_usuario,
    buscar_ecoimpacto_global,
    buscar_material_por_codigo,
    buscar_meu_ecoimpacto,
    buscar_meu_ecoscore,
    buscar_timeline_coleta,
    criar_ponto_coleta,
    eh_administrador,
    listar_coletas_operacional,
    listar_cooperativas,
    listar_ecoselos,
    listar_instituicoes,
    listar_materiais,
    listar_meus_ecoselos,
    listar_minhas_coletas,
    listar_minhas_instituicoes,
    listar_pontos_coleta,
    listar_ranking_ecoscore,
    listar_usuarios,
    obter_nome_atual,
    obter_token,
)
import mocks as m
from ui_kit import (
    PRIMARY, bar_chart, card, chip_select, data_table, donut, donut_with_legend,
    dot_steps, form_field, grid, h, icon, item_list, kpi_row, layout, metric_list,
    eco_button, numbered_steps, page_header, progress_footer, rank_list,
    route_map_html, stacked_metric_list, timeline,
)

_MAPA_MATERIAL_ENUM = {
    'papel': 'PAPEL',
    'papel e papelão': 'PAPEL',
    'papelao': 'PAPEL',
    'papelão': 'PAPEL',
    'plástico': 'PLASTICO',
    'plastico': 'PLASTICO',
    'vidro': 'VIDRO',
    'metal': 'METAL',
    'orgânico': 'ORGANICO',
    'organico': 'ORGANICO',
    'eletrônico': 'ELETRONICO',
    'eletronico': 'ELETRONICO',
    'eletrônicos': 'ELETRONICO',
    'eletronicos': 'ELETRONICO',
    'óleo': 'OLEO',
    'oleo': 'OLEO',
    'misto': 'MISTO',
}

_ENUM_PARA_CHIP = {
    'PAPEL': 'Papel',
    'PLASTICO': 'Plástico',
    'VIDRO': 'Vidro',
    'METAL': 'Metal',
    'ELETRONICO': 'Eletrônico',
    'ORGANICO': 'Orgânico',
    'OLEO': 'Óleo',
    'MISTO': 'Misto',
}

_CORES_DONUT_MATERIAL = [
    PRIMARY,
    '#4ADE80',
    '#DBEAFE',
    '#FEF3C7',
    '#FDE2E4',
    '#A7F3D0',
    '#BFDBFE',
    '#FDE68A',
]

_LABEL_STATUS_PONTO = {
    'ATIVO': 'Ativo',
    'INATIVO': 'Inativo',
    'MANUTENCAO': 'Manutenção',
    'MANUTENÇÃO': 'Manutenção',
    'BLOQUEADO': 'Bloqueado',
}

_STATUS_PONTO_OPCOES = {
    'ATIVO': 'Ativo',
    'INATIVO': 'Inativo',
    'MANUTENCAO': 'Manutenção',
    'BLOQUEADO': 'Bloqueado',
}

_OPCOES_TIPO_MATERIAL_COLETA = {
    'PAPEL': 'Papel e papelão',
    'PLASTICO': 'Plástico',
    'VIDRO': 'Vidro',
    'METAL': 'Metal',
    'ELETRONICO': 'Eletrônico',
    'ORGANICO': 'Orgânico',
    'OLEO': 'Óleo',
    'MISTO': 'Misto',
}

_STATUS_COLETA_OPCOES = {
    'ENVIADA': 'Enviada',
    'EM_ANALISE': 'Em análise',
    'AGENDADA': 'Agendada',
    'EM_ROTA': 'Em rota',
    'REALIZADA': 'Realizada',
    'CANCELADA': 'Cancelada',
}

_TIPOS_INSTITUICAO_OPCOES = {
    'EMPRESA': 'Empresa',
    'CONDOMINIO': 'Condomínio',
    'COOPERATIVA': 'Cooperativa',
}

_PERFIL_LABEL = {
    'USUARIO': 'Usuário',
    'INSTITUICAO': 'Condomínio / Instituição',
    'EMPRESA': 'Empresa',
    'COOPERATIVA': 'Cooperativa',
    'ADMINISTRADOR': 'Administrador',
}

_STATUS_CADASTRO_OPCOES = {
    'ATIVO': 'Aprovado (Ativo)',
    'AGUARDANDO_ANALISE': 'Aguardando análise',
    'REJEITADO': 'Rejeitado',
    'INATIVO': 'Inativo',
}

_MESES_CURTOS = ('Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez')


def _obrigatorios(**campos) -> bool:
    """Valida campos obrigatorios e avisa quais faltam."""
    faltando = [nome for nome, campo in campos.items() if not str(campo.value or '').strip()]
    if faltando:
        ui.notify(f'Preencha: {", ".join(faltando)}.', type='warning')
        return False
    return True


def _normalizar_materiais(selecionados: set[str]) -> list[str]:
    convertidos: list[str] = []
    for item in selecionados:
        chave = item.strip().lower()
        enum_val = _MAPA_MATERIAL_ENUM.get(chave)
        if enum_val and enum_val not in convertidos:
            convertidos.append(enum_val)
        elif not enum_val:
            limpo = re.sub(r'[^A-Z_]', '', chave.upper())
            if limpo and limpo not in convertidos:
                convertidos.append(limpo)
    return convertidos


def _extrair_endereco_4_partes(endereco_completo: str) -> tuple[str, str, str, str] | None:
    """Exige o formato: Rua, Numero, Bairro, Cidade."""
    partes = [p.strip() for p in (endereco_completo or '').split(',') if p.strip()]
    if len(partes) < 4:
        return None
    rua = partes[0][:150]
    numero = partes[1][:30]
    bairro = partes[2][:100]
    cidade = re.sub(r'\s*[-/]\s*[A-Za-z]{2}$', '', partes[3]).strip()[:100]
    if not (rua and numero and bairro and cidade):
        return None
    return rua, numero, bairro, cidade


def _montar_payload_ponto(
    *,
    nome: str,
    endereco_completo: str,
    responsavel: str,
    horarios: str,
    materiais_selecionados: set[str],
) -> dict[str, Any] | None:
    """Monta PontoColetaRequisicao validando Rua, Numero, Bairro, Cidade."""
    endereco_split = _extrair_endereco_4_partes(endereco_completo)
    if endereco_split is None:
        ui.notify(
            'Informe o endereço no formato: Rua, Numero, Bairro, Cidade (separados por vírgula).',
            type='warning',
        )
        return None

    logradouro, numero, bairro, cidade = endereco_split

    cep_match = re.search(r'\b\d{5}-?\d{3}\b', endereco_completo)
    cep_limpo = re.sub(r'\D', '', cep_match.group(0)) if cep_match else '45820000'
    if len(cep_limpo) < 8:
        cep_limpo = '45820000'

    uf_match = re.search(r'(?:[-/,\s]+)([A-Za-z]{2})(?:\s|$|,)', endereco_completo)
    estado = uf_match.group(1).upper() if uf_match else 'BA'

    horario_final = (horarios or '').strip()[:120] or 'Seg - Sex 08h às 17h'
    materiais_enum = _normalizar_materiais(materiais_selecionados)

    return {
        'nome': nome.strip()[:150],
        'nomeResponsavel': responsavel.strip()[:120],
        'horarioFuncionamento': horario_final,
        'logradouro': logradouro,
        'numero': numero,
        'bairro': bairro,
        'cidade': cidade,
        'estado': estado,
        'cep': cep_limpo,
        'latitude': -16.3766,
        'longitude': -39.5808,
        'materiaisAceitos': materiais_enum,
    }


def _converter_pontos_para_tabela(pontos: list[dict[str, Any]]) -> m.Table:
    rows: list[tuple] = []
    for p in pontos:
        nome = str(p.get('nome') or '-')
        bairro = str(p.get('bairro') or '-')
        cidade = str(p.get('cidade') or '')
        local = f'{bairro} - {cidade}' if cidade else bairro

        mats = p.get('materiaisAceitos') or []
        mats_str = ', '.join(str(x).capitalize() for x in mats) if mats else '-'

        status_raw = str(p.get('status') or 'ATIVO').upper()
        status_label = _LABEL_STATUS_PONTO.get(status_raw, status_raw.capitalize())
        tone = 'green' if status_raw == 'ATIVO' else ('amber' if 'MANUT' in status_raw else 'gray')

        rows.append((nome, local, mats_str, m.Badge(status_label, tone)))

    return m.Table(
        columns=('Ponto', 'Bairro / Cidade', 'Materiais', 'Status'),
        rows=rows,
    )


def _normalizar_tipo_material_coleta(valor: str) -> str:
    limpo = (valor or '').strip().lower()
    if limpo in _MAPA_MATERIAL_ENUM:
        return _MAPA_MATERIAL_ENUM[limpo]
    for k, v in _MAPA_MATERIAL_ENUM.items():
        if k in limpo:
            return v
    return re.sub(r'[^A-Z_]', '', (valor or '').strip().upper()) or 'PAPEL'


def _montar_data_hora_futura(data_str: str, janela_str: str) -> str | None:
    """Converte YYYY-MM-DD + janela de horario em ISO LocalDateTime no futuro."""
    data_limpa = (data_str or '').strip()
    if not data_limpa:
        return None
    try:
        dia = datetime.strptime(data_limpa, '%Y-%m-%d').date()
    except ValueError:
        return None

    hora_match = re.search(r'(\d{1,2})h', janela_str or '')
    hora = int(hora_match.group(1)) if hora_match else 9
    hora = max(0, min(23, hora))
    dt = datetime(dia.year, dia.month, dia.day, hora, 0, 0)
    if dt <= datetime.now():
        return None
    return dt.isoformat()


def _formatar_data_curta(iso_str: str) -> str:
    """Formata ISO datetime para '02 Abr'."""
    if not iso_str:
        return '-'
    try:
        dt = datetime.fromisoformat(iso_str.replace('Z', ''))
        return f'{dt.day:02d} {_MESES_CURTOS[dt.month - 1]}'
    except ValueError:
        return iso_str[:10]


def _ordenar_coletas_canceladas_por_ultimo(coletas: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Mantem a ordem original, mas empurra qualquer coleta CANCELADA para o final."""
    ativas_ou_concluidas = [
        c for c in coletas if str(c.get('status') or '').strip().upper() != 'CANCELADA'
    ]
    canceladas = [
        c for c in coletas if str(c.get('status') or '').strip().upper() == 'CANCELADA'
    ]
    return ativas_ou_concluidas + canceladas


def _converter_coletas_para_tabela(coletas: list[dict[str, Any]]) -> m.Table:
    rows: list[tuple] = []
    for c in coletas:
        data_fmt = _formatar_data_curta(str(c.get('dataHoraAgendamento') or c.get('dataCriacao') or ''))
        mat_raw = str(c.get('tipoMaterial') or 'PAPEL').upper()
        mat_lbl = _ENUM_PARA_CHIP.get(mat_raw, mat_raw.capitalize())
        qtd = c.get('quantidadeEstimadaKg')
        if qtd is not None:
            qtd_num = int(qtd) if float(qtd).is_integer() else qtd
            qtd_str = f'{qtd_num} kg'
        else:
            qtd_str = '-'

        st_raw = str(c.get('status') or 'AGENDADA').upper()
        st_lbl = _STATUS_COLETA_OPCOES.get(st_raw, st_raw.replace('_', ' ').capitalize())
        rows.append((data_fmt, mat_lbl, qtd_str, m.Badge(st_lbl)))

    return m.Table(
        columns=('Data', 'Material', 'Quantidade', 'Status'),
        rows=rows,
    )


def _converter_timeline_para_etapas(eventos: list[dict[str, Any]]) -> list[m.Step]:
    if not eventos:
        return []

    etapas: list[m.Step] = []
    ultimo_status = ''
    for ev in eventos:
        novo_st = str(ev.get('statusNovo') or '').upper()
        ultimo_status = novo_st
        titulo = _STATUS_COLETA_OPCOES.get(
            novo_st,
            novo_st.replace('_', ' ').capitalize() or 'Atualização',
        )
        data_fmt = _formatar_data_curta(str(ev.get('dataRegistro') or ''))
        hora_reg = str(ev.get('dataRegistro') or '')[11:16]
        obs = str(ev.get('observacao') or '').strip()
        meta_partes = [p for p in [f'{data_fmt} - {hora_reg}' if hora_reg else data_fmt, obs] if p]
        etapas.append(m.Step(title=titulo, meta=' • '.join(meta_partes), done=True))

    fluxo_padrao = [
        ('EM_ANALISE', 'Em análise'),
        ('AGENDADA', 'Agendada'),
        ('EM_ROTA', 'Em rota'),
        ('REALIZADA', 'Realizada'),
    ]
    if ultimo_status not in ('REALIZADA', 'CANCELADA'):
        ja_feitos = {str(e.get('statusNovo') or '').upper() for e in eventos}
        for cod_st, nome_st in fluxo_padrao:
            if cod_st not in ja_feitos:
                etapas.append(m.Step(title=nome_st, meta='Aguardando...', done=False))

    return etapas


def _formatar_numero_pt(valor: float) -> str:
    if float(valor).is_integer():
        return f'{int(valor):,}'.replace(',', '.')
    return f'{valor:,.1f}'.replace(',', 'X').replace('.', ',').replace('X', '.')


def _atualizar_kpi(base: m.Kpi, label: str, value: str) -> m.Kpi:
    try:
        return replace(base, label=label, value=value)
    except TypeError:
        campos = getattr(base, '__dataclass_fields__', {})
        kwargs: dict[str, Any] = {'label': label, 'value': value}
        for nome in campos:
            if nome not in ('label', 'value'):
                kwargs[nome] = getattr(base, nome)
        return m.Kpi(**kwargs)


def _construir_rank_item(pos: int, name: str, value: str, pct: int) -> m.Metric:
    """Cria um m.Metric compativel com rank_list (metric.label, metric.value, metric.pct)."""
    return m.Metric(label=name, value=value, pct=pct)


def _normalizar_nome_ranking(nome: str) -> str:
    """Evita nomes totalmente minusculos no ranking sem alterar capitalizacao ja existente."""
    limpo = (nome or '').strip()
    if not limpo:
        return limpo
    if limpo.islower():
        return limpo.title()
    return limpo


def _montar_dados_dashboard() -> tuple[list[m.Kpi], m.Table]:
    """Busca dados reais disponiveis na API para KPIs e solicitacoes recentes."""
    coletas: list[dict[str, Any]] = []
    pontos: list[dict[str, Any]] = []
    instituicoes: list[dict[str, Any]] = []

    tem_coletas = False
    tem_pontos = False
    tem_instituicoes = False

    try:
        coletas = listar_minhas_coletas()
        tem_coletas = True
    except ApiError:
        coletas = []

    try:
        pontos = listar_pontos_coleta()
        tem_pontos = True
    except ApiError:
        pontos = []

    try:
        instituicoes = listar_minhas_instituicoes()
        tem_instituicoes = True
    except ApiError:
        instituicoes = []

    kpis_base = list(m.KPIS_DASHBOARD)

    if tem_coletas and len(kpis_base) >= 2:
        realizadas = [
            c for c in coletas if str(c.get('status') or '').strip().upper() == 'REALIZADA'
        ]
        total_kg = sum(
            float(c.get('quantidadeEstimadaKg') or 0) for c in realizadas
        )
        kpis_base[0] = _atualizar_kpi(
            kpis_base[0],
            'TOTAL RECICLADO (KG)',
            _formatar_numero_pt(total_kg),
        )
        kpis_base[1] = _atualizar_kpi(
            kpis_base[1],
            'COLETAS REALIZADAS',
            f'{len(realizadas):02d}' if len(realizadas) < 10 else str(len(realizadas)),
        )

    if tem_pontos and len(kpis_base) >= 3:
        ativos = [
            p for p in pontos if str(p.get('status') or 'ATIVO').strip().upper() == 'ATIVO'
        ]
        kpis_base[2] = _atualizar_kpi(
            kpis_base[2],
            'PONTOS ATIVOS',
            f'{len(ativos):02d}' if len(ativos) < 10 else str(len(ativos)),
        )

    if tem_instituicoes and len(kpis_base) >= 4:
        kpis_base[3] = _atualizar_kpi(
            kpis_base[3],
            'INSTITUIÇÕES',
            f'{len(instituicoes):02d}' if len(instituicoes) < 10 else str(len(instituicoes)),
        )

    if tem_coletas and coletas:
        mapa_inst = {
            int(i['id']): str(i.get('nome') or f"Instituição #{i['id']}")
            for i in instituicoes
            if i.get('id') is not None
        }
        recentes_rows: list[tuple] = []
        for c in list(reversed(coletas))[:5]:
            inst_id = c.get('instituicaoId')
            origem = (
                mapa_inst.get(int(inst_id))
                if inst_id is not None and int(inst_id) in mapa_inst
                else (str(c.get('bairro') or '').strip() or 'Solicitação própria')
            )
            mat_raw = str(c.get('tipoMaterial') or 'PAPEL').upper()
            mat_lbl = _ENUM_PARA_CHIP.get(mat_raw, mat_raw.capitalize())
            st_raw = str(c.get('status') or 'ENVIADA').upper()
            st_lbl = _STATUS_COLETA_OPCOES.get(st_raw, st_raw.replace('_', ' ').capitalize())
            recentes_rows.append((origem, mat_lbl, m.Badge(st_lbl)))

        tabela_recentes = m.Table(
            columns=('Instituição', 'Material', 'Status'),
            rows=recentes_rows,
        )
    else:
        tabela_recentes = m.SOLICITACOES_RECENTES

    return kpis_base, tabela_recentes


def _fmt_qtd_admin(qtd: int) -> str:
    if qtd < 10:
        return f'{qtd:02d}'
    return _formatar_numero_pt(float(qtd))


def _montar_kpis_admin() -> list[m.Kpi]:
    """Consulta usuarios, instituicoes e cooperativas reais para os KPIs de /administracao."""
    kpis_admin = list(m.KPIS_ADMIN)

    try:
        usuarios = listar_usuarios()
        ativos = [u for u in usuarios if str(u.get('status') or '').upper() == 'ATIVO']
        pendentes = [u for u in usuarios if str(u.get('status') or '').upper() == 'AGUARDANDO_ANALISE']
        hint_u = (
            f'{len(pendentes)} aguardando análise'
            if pendentes
            else f'{len(usuarios)} cadastrados no total'
        )
        if len(kpis_admin) >= 1:
            kpis_admin[0] = replace(
                kpis_admin[0],
                label='USUÁRIOS ATIVOS',
                value=_fmt_qtd_admin(len(ativos)),
                hint=hint_u,
            )
    except ApiError:
        pass

    try:
        instituicoes = listar_instituicoes()
        somente_inst = [
            i for i in instituicoes
            if str(i.get('tipo') or '').upper() != 'COOPERATIVA'
        ]
        inst_pend = [
            i for i in somente_inst
            if str(i.get('status') or '').upper() == 'AGUARDANDO_ANALISE'
        ]
        inst_ativas = [
            i for i in somente_inst
            if str(i.get('status') or '').upper() == 'ATIVO'
        ]
        hint_i = (
            f'{len(inst_pend)} aguardando análise'
            if inst_pend
            else f'{len(inst_ativas)} ativas'
        )
        if len(kpis_admin) >= 2:
            kpis_admin[1] = replace(
                kpis_admin[1],
                label='INSTITUIÇÕES',
                value=_fmt_qtd_admin(len(somente_inst)),
                hint=hint_i,
            )
    except ApiError:
        pass

    try:
        cooperativas = listar_cooperativas()
        coop_ativas = [
            c for c in cooperativas
            if str(c.get('status') or '').upper() == 'ATIVO'
        ]
        if len(kpis_admin) >= 3:
            kpis_admin[2] = replace(
                kpis_admin[2],
                label='COOPERATIVAS',
                value=_fmt_qtd_admin(len(cooperativas)),
                hint=f'{len(coop_ativas)} ativas',
            )
    except ApiError:
        pass

    if len(kpis_admin) >= 4:
        kpis_admin[3] = replace(
            kpis_admin[3],
            label='PERMISSÕES',
            value='05',
            hint='perfis configurados',
        )

    return kpis_admin


def _formatar_co2_kpi(co2_kg: float) -> str:
    if co2_kg >= 1000:
        return f'{_formatar_numero_pt(round(co2_kg / 1000, 1))} t'
    return f'{_formatar_numero_pt(round(co2_kg, 1))} kg'


def _formatar_agua_kpi(litros: float) -> str:
    if litros >= 1000:
        return f'{_formatar_numero_pt(round(litros / 1000, 1))} mil L'
    return f'{_formatar_numero_pt(round(litros, 1))} L'


def _formatar_energia_kpi(kwh: float) -> str:
    if kwh >= 1000:
        return f'{_formatar_numero_pt(round(kwh / 1000, 1))} MWh'
    return f'{_formatar_numero_pt(round(kwh, 1))} kWh'


def _ultimos_5_meses_base_atual() -> list[tuple[int, int]]:
    """Retorna os 5 meses encerrando no mes/ano atual em ordem cronologica."""
    hoje = datetime.now().date()
    ano = hoje.year
    mes = hoje.month
    meses: list[tuple[int, int]] = []
    for _ in range(5):
        meses.append((ano, mes))
        mes -= 1
        if mes < 1:
            mes = 12
            ano -= 1
    meses.reverse()
    return meses


def _montar_dados_ecoimpacto() -> dict[str, Any]:
    """Busca dados reais de EcoImpacto, coletas e instituicoes sem mascarar ausencia de dados."""
    kpis_out = list(m.KPIS_ECOIMPACTO)
    slices_out: list[m.Slice] = []
    tem_meses_reais = False
    equivalencias_out = [
        m.Item(title='0 kg', meta='de CO2 evitadas', tone='green'),
        m.Item(title='0 kg', meta='fora de aterros', tone='blue'),
        m.Item(title='0', meta='coletas concluídas', tone='amber'),
    ]
    impacto_inst_out: list[m.Metric] = []

    dados: dict[str, Any] | None = None
    for fetcher in (buscar_ecoimpacto_global, buscar_meu_ecoimpacto):
        try:
            res = fetcher()
            if isinstance(res, dict) and res:
                dados = res
                break
        except ApiError:
            continue

    coletas: list[dict[str, Any]] = []
    try:
        coletas = listar_coletas_operacional()
    except ApiError:
        try:
            coletas = listar_minhas_coletas()
        except ApiError:
            coletas = []

    instituicoes_mapa: dict[int, str] = {}
    try:
        for inst in listar_instituicoes() + listar_cooperativas():
            if inst.get('id') is not None:
                instituicoes_mapa[int(inst['id'])] = str(
                    inst.get('nome') or f"Instituição #{inst['id']}"
                )
    except ApiError:
        instituicoes_mapa = {}

    if dados:
        co2_kg = float(dados.get('co2EvitadoKg') or 0)
        arvores = float(dados.get('arvoresPreservadasEquivalentes') or 0)
        agua_l = float(dados.get('aguaEconomizadaLitros') or 0)
        energia_kwh = float(dados.get('energiaEconomizadaKwh') or 0)
        total_kg = float(dados.get('totalResiduosRecicladosKg') or 0)
        total_coletas = int(dados.get('totalColetasConcluidas') or 0)

        hint_co2 = (
            f'{total_coletas} coleta{"s" if total_coletas != 1 else ""} concluída{"s" if total_coletas != 1 else ""}'
            if co2_kg > 0 or total_coletas > 0
            else 'sem registro no período'
        )
        hint_agua = (
            f'{_formatar_numero_pt(round(total_kg, 1))} kg reciclados'
            if agua_l > 0 or total_kg > 0
            else 'sem economia no período'
        )

        if len(kpis_out) >= 1:
            kpis_out[0] = replace(
                kpis_out[0],
                label='CO2 EVITADO',
                value=_formatar_co2_kpi(co2_kg),
                hint=hint_co2,
            )
        if len(kpis_out) >= 2:
            kpis_out[1] = replace(
                kpis_out[1],
                label='ÁRVORES PRESERVADAS',
                value=_formatar_numero_pt(arvores),
                hint='equivalência estimada',
            )
        if len(kpis_out) >= 3:
            kpis_out[2] = replace(
                kpis_out[2],
                label='ÁGUA ECONOMIZADA',
                value=_formatar_agua_kpi(agua_l),
                hint=hint_agua,
            )
        if len(kpis_out) >= 4:
            kpis_out[3] = replace(
                kpis_out[3],
                label='ENERGIA POUPADA',
                value=_formatar_energia_kpi(energia_kwh),
                hint='por reciclagem',
            )

        detalhes = dados.get('detalhamentoPorMaterial')
        if isinstance(detalhes, list) and detalhes:
            pesos = [float(item.get('pesoTotalKg') or 0) for item in detalhes]
            soma_pesos = sum(pesos)
            if soma_pesos <= 0:
                pesos = [float(item.get('co2EvitadoKg') or 0) for item in detalhes]
                soma_pesos = sum(pesos)

            if soma_pesos > 0:
                novas_slices: list[m.Slice] = []
                pcts_brutos: list[int] = []
                for item, val in zip(detalhes, pesos):
                    cod = str(item.get('codigoMaterial') or '').upper()
                    nome_mat = (
                        str(item.get('nomeMaterial') or '').strip()
                        or _ENUM_PARA_CHIP.get(cod, cod.capitalize() or 'Material')
                    )
                    pct_val = int(round((max(val, 0.0) / soma_pesos) * 100))
                    pcts_brutos.append(pct_val)
                    novas_slices.append(
                        m.Slice(
                            label=nome_mat,
                            pct=pct_val,
                            color=_CORES_DONUT_MATERIAL[len(novas_slices) % len(_CORES_DONUT_MATERIAL)],
                        )
                    )

                diff = 100 - sum(pcts_brutos)
                if novas_slices and diff != 0:
                    idx_maior = max(range(len(novas_slices)), key=lambda i: novas_slices[i].pct)
                    novas_slices[idx_maior] = replace(
                        novas_slices[idx_maior],
                        pct=max(0, novas_slices[idx_maior].pct + diff),
                    )
                slices_out = [s for s in novas_slices if s.pct > 0]

        equivalencias_out = [
            m.Item(
                title=_formatar_co2_kpi(co2_kg),
                meta='de CO2 evitadas',
                tone='green',
            ),
            m.Item(
                title=_formatar_co2_kpi(total_kg),
                meta='fora de aterros',
                tone='blue',
            ),
            m.Item(
                title=str(total_coletas),
                meta='coletas concluídas',
                tone='amber',
            ),
        ]

    janela_5_meses = _ultimos_5_meses_base_atual()
    por_mes: dict[tuple[int, int], float] = {chave: 0.0 for chave in janela_5_meses}
    por_inst: dict[str, float] = {}

    for c in coletas:
        st = str(c.get('status') or '').upper()
        if st == 'CANCELADA':
            continue
        kg = float(c.get('quantidadeEstimadaKg') or 0)
        if kg <= 0:
            continue

        data_ref = str(c.get('dataHoraAgendamento') or c.get('dataCriacao') or '')
        if len(data_ref) >= 7:
            try:
                dt = datetime.fromisoformat(data_ref.replace('Z', ''))
                chave_mes = (dt.year, dt.month)
                if chave_mes in por_mes:
                    por_mes[chave_mes] += kg
                    tem_meses_reais = True
            except ValueError:
                pass

        inst_id = c.get('instituicaoId')
        if inst_id is not None and int(inst_id) in instituicoes_mapa:
            nome_inst = instituicoes_mapa[int(inst_id)]
        else:
            nome_inst = str(c.get('bairro') or '').strip() or 'Solicitação avulsa'
        por_inst[nome_inst] = por_inst.get(nome_inst, 0.0) + kg

    max_kg_mes = max(por_mes.values(), default=0.0)
    meses_out = [
        m.Bar(
            label=_MESES_CURTOS[mes - 1],
            pct=int(round((por_mes[(ano, mes)] / max_kg_mes) * 100)) if max_kg_mes > 0 else 0,
        )
        for ano, mes in janela_5_meses
    ]

    if por_inst:
        top_inst = sorted(por_inst.items(), key=lambda x: x[1], reverse=True)[:4]
        max_kg_inst = max((v for _, v in top_inst), default=0.0)
        if max_kg_inst > 0:
            impacto_inst_out = [
                m.Metric(
                    label=nome_i,
                    value=_formatar_co2_kpi(kg_i),
                    pct=max(10, int(round((kg_i / max_kg_inst) * 100))),
                )
                for nome_i, kg_i in top_inst
            ]

    return {
        'kpis': kpis_out,
        'slices': slices_out,
        'meses': meses_out,
        'tem_meses_reais': tem_meses_reais,
        'equivalencias': equivalencias_out,
        'impacto_inst': impacto_inst_out,
    }


def _montar_dados_ecoselo() -> dict[str, Any]:
    """Busca apenas selos reais vinculados ao usuário autenticado em /api/v1/ecoselos/meus."""
    selos: list[dict[str, Any]] = []
    try:
        res = listar_meus_ecoselos()
        if isinstance(res, list):
            selos = res
    except ApiError:
        selos = []

    qtd_total = len(selos)
    qtd_ouro = sum(
        1 for s in selos
        if 'OURO' in str(
            s.get('nivel') or s.get('selo') or s.get('nome') or s.get('codigo') or ''
        ).upper()
    )

    kpis_out = [
        replace(
            m.KPIS_ECOSELO[0],
            label='SELOS CONQUISTADOS',
            value=_fmt_qtd_admin(qtd_total),
            hint='vinculados à sua conta' if qtd_total else 'nenhum selo conquistado',
        ),
        replace(
            m.KPIS_ECOSELO[1],
            label='SELO OURO',
            value=_fmt_qtd_admin(qtd_ouro),
            hint='nível ouro' if qtd_ouro else 'nenhum selo ouro',
        ),
        replace(
            m.KPIS_ECOSELO[2],
            label='ADERÊNCIA MÉDIA',
            value='-',
            hint='não fornecido pela API',
        ),
        replace(
            m.KPIS_ECOSELO[3],
            label='RENOVAÇÕES',
            value='-',
            hint='não fornecido pela API',
        ),
    ]

    return {
        'kpis': kpis_out,
        'selos': selos,
        'destaque': selos[0] if selos else None,
    }


def _montar_dados_ecoscore() -> dict[str, Any]:
    """Busca EcoScore individual (/meu), ranking (/ranking) e materiais dos ultimos 5 meses."""
    nome_usuario = obter_nome_atual() or 'Usuário'
    meu_score: dict[str, Any] = {}
    try:
        meu_score = buscar_meu_ecoscore()
    except ApiError:
        meu_score = {}

    pontos_num = int(round(float(
        meu_score.get('pontuacaoTotal')
        or meu_score.get('pontuacao')
        or meu_score.get('pontos')
        or meu_score.get('score')
        or 0
    )))
    nome_exibicao = _normalizar_nome_ranking(str(
        meu_score.get('nomeUsuario')
        or meu_score.get('nomeInstituicao')
        or meu_score.get('nome')
        or nome_usuario
    )) or nome_usuario

    nivel_atual = str(
        meu_score.get('nivel')
        or meu_score.get('classificacao')
        or meu_score.get('selo')
        or ('Iniciante' if pontos_num == 0 else 'EcoSelo Ativo')
    ).strip()

    prox_meta = int(round(float(meu_score.get('proximaMetaPontos') or 0)))
    faltam_pts = int(round(float(meu_score.get('pontosParaProximoNivel') or 0)))
    if prox_meta > 0:
        pct_score = max(0, min(100, int(round((pontos_num / prox_meta) * 100))))
    else:
        pct_score = max(0, min(100, int(round((pontos_num / 2000) * 100)))) if pontos_num > 0 else 0

    if faltam_pts > 0:
        meta_texto = f'+{faltam_pts} pts para o próximo nível'
    elif meu_score:
        meta_texto = f'{int(meu_score.get("totalColetasConcluidas") or 0)} coletas concluídas'
    else:
        meta_texto = 'Pontuação calculada conforme suas coletas concluídas'

    ranking_bruto: list[dict[str, Any]] = []
    try:
        ranking_bruto = listar_ranking_ecoscore()
    except ApiError:
        ranking_bruto = []

    ranking_itens: list[m.Metric] = []
    if ranking_bruto:
        def _pts_item(r: dict[str, Any]) -> float:
            return float(
                r.get('pontuacaoTotal')
                or r.get('pontuacao')
                or r.get('pontos')
                or r.get('score')
                or 0
            )

        ranking_ordenado = sorted(ranking_bruto, key=_pts_item, reverse=True)
        max_pts = max((_pts_item(r) for r in ranking_ordenado), default=0.0)

        for idx, r in enumerate(ranking_ordenado[:10], start=1):
            pts_r = int(round(_pts_item(r)))
            nome_r = _normalizar_nome_ranking(str(
                r.get('nomeUsuario')
                or r.get('nomeInstituicao')
                or r.get('nome')
                or f'Participante #{idx}'
            ))
            pct_r = int(round((pts_r / max_pts) * 100)) if max_pts > 0 else 0
            ranking_itens.append(
                _construir_rank_item(
                    pos=idx,
                    name=nome_r,
                    value=_formatar_numero_pt(float(pts_r)),
                    pct=pct_r,
                )
            )

    coletas: list[dict[str, Any]] = []
    try:
        coletas = listar_coletas_operacional()
    except ApiError:
        try:
            coletas = listar_minhas_coletas()
        except ApiError:
            coletas = []

    janela_5_meses = _ultimos_5_meses_base_atual()
    por_mes: dict[tuple[int, int], float] = {chave: 0.0 for chave in janela_5_meses}
    tem_meses_reais = False

    for c in coletas:
        if str(c.get('status') or '').upper() == 'CANCELADA':
            continue
        kg = float(c.get('quantidadeEstimadaKg') or 0)
        if kg <= 0:
            continue
        data_ref = str(c.get('dataHoraAgendamento') or c.get('dataCriacao') or '')
        if len(data_ref) >= 7:
            try:
                dt = datetime.fromisoformat(data_ref.replace('Z', ''))
                chave = (dt.year, dt.month)
                if chave in por_mes:
                    por_mes[chave] += kg
                    tem_meses_reais = True
            except ValueError:
                pass

    max_kg_mes = max(por_mes.values(), default=0.0)
    meses_out = [
        m.Bar(
            label=_MESES_CURTOS[mes - 1],
            pct=int(round((por_mes[(ano, mes)] / max_kg_mes) * 100)) if max_kg_mes > 0 else 0,
        )
        for ano, mes in janela_5_meses
    ]

    return {
        'nome_exibicao': nome_exibicao,
        'pontos_fmt': f'{_formatar_numero_pt(float(pontos_num))} pts',
        'pct_score': pct_score,
        'nivel': nivel_atual,
        'meta_texto': meta_texto,
        'ranking': ranking_itens,
        'total_participantes': len(ranking_bruto),
        'meses': meses_out,
        'tem_meses_reais': tem_meses_reais,
    }


def _carregar_materiais_catalogo_api() -> list[dict[str, Any]]:
    """Busca o catalogo completo em GET /api/v1/materiais com fallback para m.TIPOS_REGISTRO."""
    try:
        itens = listar_materiais()
        if isinstance(itens, list) and itens:
            return [item for item in itens if isinstance(item, dict)]
    except ApiError:
        pass

    return [
        {
            'codigo': _normalizar_tipo_material_coleta(nome),
            'nome': nome,
            'descricao': '',
            'unidadePadrao': 'L' if 'OLEO' in _normalizar_tipo_material_coleta(nome) else 'kg',
            'fatorCarbonoEvitadoKg': None,
        }
        for nome in m.TIPOS_REGISTRO
    ]


@ui.page('/dashboard')
def dashboard() -> None:
    with layout('/dashboard'):
        page_header('Visão Geral', 'Resumo das atividades de reciclagem - Abril 2026')
        kpis_dashboard, tabela_recentes = _montar_dados_dashboard()
        kpi_row(kpis_dashboard)

        with grid('2'):
            with card('Materiais coletados - 05 meses'):
                bar_chart(m.MESES_5)
            with card('Solicitações recentes', more='Ver mais'):
                data_table(tabela_recentes)

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

        dialog_editar = ui.dialog()
        carregar_tabela_ref: list[Any] = [None]

        def abrir_modal_edicao(ponto: dict[str, Any]) -> None:
            dialog_editar.clear()
            ponto_id = ponto.get('id')
            if not ponto_id:
                ui.notify('Ponto sem ID válido para edição.', type='warning')
                return

            end_inicial = ', '.join(
                parte for parte in [
                    str(ponto.get('logradouro') or '').strip(),
                    str(ponto.get('numero') or '').strip(),
                    str(ponto.get('bairro') or '').strip(),
                    str(ponto.get('cidade') or '').strip(),
                ]
                if parte
            )

            mats_iniciais = {
                _ENUM_PARA_CHIP.get(str(x).upper(), str(x).capitalize())
                for x in (ponto.get('materiaisAceitos') or [])
            }
            mats_iniciais = {x for x in mats_iniciais if x in m.MATERIAIS_ACEITOS}

            status_atual = str(ponto.get('status') or 'ATIVO').upper()
            if status_atual not in _STATUS_PONTO_OPCOES:
                status_atual = 'ATIVO'

            with dialog_editar, ui.card().style('width:540px;max-width:95vw;border-radius:16px;padding:20px'):
                h(f'<div class="card-title" style="margin-bottom:12px">Editar ponto #{ponto_id}</div>')
                with ui.element('div').classes('form-grid'):
                    ed_nome = form_field('Nome do ponto', value=str(ponto.get('nome') or ''))
                    ed_end = form_field(
                        'Endereço completo',
                        value=end_inicial,
                        placeholder='Rua, Numero, Bairro, Cidade',
                    )
                    with ui.element('div').classes('form-grid cols-2'):
                        ed_resp = form_field('Responsável', value=str(ponto.get('nomeResponsavel') or ''))
                        ed_hor = form_field('Horários', value=str(ponto.get('horarioFuncionamento') or ''))
                    with ui.element('div'):
                        h('<label class="form-label">Materiais aceitos</label>')
                        ed_mats = chip_select(m.MATERIAIS_ACEITOS, mats_iniciais)

                    with ui.element('div').classes('form-grid cols-2'):
                        with ui.element('div'):
                            h('<label class="form-label">Status do ponto</label>')
                            ed_status = ui.select(
                                _STATUS_PONTO_OPCOES,
                                value=status_atual,
                            ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')
                        ed_motivo = form_field(
                            'Motivo do status (opcional)',
                            value=str(ponto.get('motivoStatus') or ''),
                            placeholder='Ex: Manutenção programada',
                        )

                    def salvar_edicao() -> None:
                        if not _obrigatorios(nome=ed_nome, endereço=ed_end, responsável=ed_resp):
                            return
                        if not ed_mats:
                            ui.notify('Selecione ao menos um material aceito.', type='warning')
                            return

                        payload = _montar_payload_ponto(
                            nome=ed_nome.value,
                            endereco_completo=ed_end.value,
                            responsavel=ed_resp.value,
                            horarios=ed_hor.value,
                            materiais_selecionados=ed_mats,
                        )
                        if payload is None:
                            return

                        if ponto.get('latitude') is not None:
                            payload['latitude'] = ponto['latitude']
                        if ponto.get('longitude') is not None:
                            payload['longitude'] = ponto['longitude']
                        if ponto.get('telefoneContato'):
                            payload['telefoneContato'] = ponto['telefoneContato']
                        if ponto.get('idInstituicao') is not None:
                            payload['instituicaoId'] = ponto['idInstituicao']

                        try:
                            atualizar_ponto_coleta(int(ponto_id), payload)
                            novo_status = str(ed_status.value or status_atual).upper()
                            motivo_novo = (ed_motivo.value or '').strip()
                            motivo_antigo = str(ponto.get('motivoStatus') or '').strip()
                            if novo_status != status_atual or motivo_novo != motivo_antigo:
                                alterar_status_ponto_coleta(
                                    int(ponto_id),
                                    novo_status,
                                    motivo_novo or None,
                                )
                            ui.notify('Ponto de coleta atualizado com sucesso!', type='positive')
                            dialog_editar.close()
                            if carregar_tabela_ref[0]:
                                carregar_tabela_ref[0](notificar_erro=True)
                        except ApiError as exc:
                            ui.notify(exc.message, type='negative')

                    with ui.row().classes('justify-end gap-2').style('margin-top:8px'):
                        eco_button('Cancelar', dialog_editar.close, cls='outline')
                        eco_button(
                            'Salvar alterações',
                            salvar_edicao,
                            cls='dark',
                            props='unelevated no-caps text-white',
                        ).style('background-color: #17693A !important; color: #FFFFFF !important;')

            dialog_editar.open()

        with grid('2'):
            with card():
                with ui.element('div').classes('map-box'):
                    h(icon('map') +
                      '<div style="font-size:14px;font-weight:500">Mapa interativo de pontos</div>'
                      '<div class="muted" style="font-size:11px">34 pontos ativos na região</div>'
                      '<div style="font-size:11px">• Papel/Plástico • Eletrônico • Metal/Vidro</div>')
                    eco_button('Planejar rota',
                               lambda: ui.navigate.to('/rotas')
                               ).style('width:130px;margin-top:10px')

            with card('Cadastrar novo ponto'):
                with ui.element('div').classes('form-grid'):
                    nome = form_field('Nome do ponto',
                                      placeholder='Ex: Ecoponto Jardim de Eunápolis')
                    endereco = form_field('Endereço completo',
                                          placeholder='Rua, Numero, Bairro, Cidade')
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

                        payload = _montar_payload_ponto(
                            nome=nome.value,
                            endereco_completo=endereco.value,
                            responsavel=responsavel.value,
                            horarios=horarios.value,
                            materiais_selecionados=materiais,
                        )
                        if payload is None:
                            return
                        if not payload['materiaisAceitos']:
                            ui.notify('Nenhum material válido selecionado para envio.', type='warning')
                            return

                        try:
                            criado = criar_ponto_coleta(payload)
                            nome_criado = criado.get('nome') or nome.value
                            ui.notify(f'Ponto "{nome_criado}" cadastrado com sucesso!',
                                      type='positive')
                            nome.value = ''
                            endereco.value = ''
                            responsavel.value = ''
                            horarios.value = ''
                            if carregar_tabela_ref[0]:
                                carregar_tabela_ref[0](notificar_erro=True)
                        except ApiError as exc:
                            ui.notify(exc.message, type='negative')

                    eco_button('Cadastrar ponto', cadastrar, cls='dark eco-block')

        with card('Pontos cadastrados'):
            tabela_container = ui.element('div')

            def carregar_tabela(notificar_erro: bool = False) -> None:
                tabela_container.clear()
                with tabela_container:
                    try:
                        pontos = listar_pontos_coleta()
                        if pontos:
                            data_table(
                                _converter_pontos_para_tabela(pontos),
                                action='Editar',
                                on_action=lambda idx, _row, pts=pontos: abrir_modal_edicao(pts[idx]),
                            )
                        else:
                            h('<div class="muted" style="font-size:12px;padding:8px 0">'
                              'Nenhum ponto de coleta cadastrado até o momento.</div>')
                    except ApiError as exc:
                        if notificar_erro:
                            ui.notify(exc.message, type='warning')
                        data_table(m.PONTOS_CADASTRADOS, action='Editar')

            carregar_tabela_ref[0] = carregar_tabela
            carregar_tabela(notificar_erro=False)


@ui.page('/agendamentos')
def agendamentos() -> None:
    with layout('/agendamentos'):
        page_header('Agendamento de retirada',
                    'Solicite a coleta seletiva e acompanhe o status da solicitação')

        selecionada_id: dict[str, int | None] = {'id': None}
        dialog_status = ui.dialog()
        carregar_coletas_ref: list[Any] = [None]
        render_timeline_ref: list[Any] = [None]

        mapa_instituicoes: dict[int, str] = {}
        mapa_cooperativas: dict[int, str] = {}
        try:
            for inst in listar_minhas_instituicoes():
                if inst.get('id') is not None:
                    inst_id = int(inst['id'])
                    nome_inst = str(inst.get('nome') or f'Instituição #{inst_id}')
                    tipo_inst = str(inst.get('tipo') or '').strip().upper()
                    if tipo_inst == 'COOPERATIVA':
                        mapa_cooperativas[inst_id] = nome_inst
                    else:
                        mapa_instituicoes[inst_id] = nome_inst
        except ApiError:
            mapa_instituicoes = {}
            mapa_cooperativas = {}

        def abrir_modal_status(coleta: dict[str, Any]) -> None:
            dialog_status.clear()
            cid = coleta.get('id')
            if not cid:
                ui.notify('Coleta sem ID válido.', type='warning')
                return

            st_atual = str(coleta.get('status') or 'ENVIADA').upper()
            if st_atual not in _STATUS_COLETA_OPCOES:
                st_atual = 'ENVIADA'

            with dialog_status, ui.card().style('width:440px;max-width:95vw;border-radius:16px;padding:20px'):
                h(f'<div class="card-title" style="margin-bottom:12px">Status da coleta #{cid}</div>')
                with ui.element('div').classes('form-grid'):
                    with ui.element('div'):
                        h('<label class="form-label">Novo status</label>')
                        sel_st = ui.select(
                            _STATUS_COLETA_OPCOES,
                            value=st_atual,
                        ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')
                    obs_st = form_field('Observação (opcional)', placeholder='Ex: Coletado em 11 de abr')

                    def salvar_status() -> None:
                        try:
                            atualizar_status_coleta(
                                int(cid),
                                str(sel_st.value or st_atual).upper(),
                                obs_st.value,
                            )
                            ui.notify(f'Status da coleta #{cid} atualizado!', type='positive')
                            dialog_status.close()
                            if carregar_coletas_ref[0]:
                                carregar_coletas_ref[0](notificar_erro=True, focar_id=int(cid))
                        except ApiError as exc:
                            ui.notify(exc.message, type='negative')

                    with ui.row().classes('justify-end gap-2').style('margin-top:8px'):
                        eco_button('Cancelar', dialog_status.close, cls='outline')
                        eco_button(
                            'Atualizar',
                            salvar_status,
                            cls='dark',
                            props='unelevated no-caps text-white',
                        ).style('background-color: #17693A !important; color: #FFFFFF !important;')

            dialog_status.open()

        with grid('2'):
            with card('Nova solicitação'):
                with ui.element('div').classes('form-grid'):
                    with ui.element('div'):
                        h('<label class="form-label">Instituição solicitante</label>')
                        instituicao_select = ui.select(
                            mapa_instituicoes,
                            value=None,
                        ).props(
                            'outlined dense hide-bottom-space options-dense clearable '
                            'aria-label="Instituição solicitante"'
                        ).classes('eco-field')

                    material = form_field(
                        'Tipo do material',
                        options=list(_OPCOES_TIPO_MATERIAL_COLETA.values()),
                        value=_OPCOES_TIPO_MATERIAL_COLETA['PAPEL'],
                    )
                    with ui.element('div').classes('form-grid cols-2'):
                        data = form_field('Data preferencial', props='type=date')
                        janela = form_field('Horários', options=m.JANELAS_HORARIO,
                                            value=m.JANELAS_HORARIO[0])
                    with ui.element('div').classes('form-grid cols-2'):
                        quantidade = form_field('Quantidade estimada', value='50',
                                                props='suffix=KG type=number min=0.1 step=0.1')
                        with ui.element('div'):
                            h('<label class="form-label">Cooperativa</label>')
                            cooperativa_select = ui.select(
                                mapa_cooperativas,
                                value=None,
                            ).props(
                                'outlined dense hide-bottom-space options-dense clearable '
                                'aria-label="Cooperativa"'
                            ).classes('eco-field')
                    endereco_coleta = form_field(
                        'Endereço de coleta',
                        placeholder='Rua, Numero, Bairro, Cidade',
                    )
                    observacoes = form_field('Observações', placeholder='Informações disponíveis...')

                    def enviar() -> None:
                        if not _obrigatorios(
                            material=material,
                            data=data,
                            quantidade=quantidade,
                            endereço=endereco_coleta,
                        ):
                            return

                        data_hora_iso = _montar_data_hora_futura(
                            str(data.value or ''),
                            str(janela.value or ''),
                        )
                        if not data_hora_iso:
                            ui.notify(
                                'Escolha uma data preferencial futura válida.',
                                type='warning',
                            )
                            return

                        end_split = _extrair_endereco_4_partes(str(endereco_coleta.value or ''))
                        if end_split is None:
                            ui.notify(
                                'Informe o endereço no formato: Rua, Numero, Bairro, Cidade.',
                                type='warning',
                            )
                            return

                        try:
                            qtd_val = float(str(quantidade.value or '0').replace(',', '.'))
                        except ValueError:
                            ui.notify('Quantidade estimada inválida.', type='warning')
                            return

                        if qtd_val < 0.1:
                            ui.notify('A quantidade estimada deve ser no mínimo 0.1 kg.', type='warning')
                            return

                        logradouro, numero, bairro, cidade = end_split
                        payload: dict[str, Any] = {
                            'dataHoraAgendamento': data_hora_iso,
                            'janelaHorario': str(janela.value or '08:00 - 10:00')[:50],
                            'tipoMaterial': _normalizar_tipo_material_coleta(str(material.value or '')),
                            'quantidadeEstimadaKg': qtd_val,
                            'observacoes': (str(observacoes.value or '')).strip()[:255] or None,
                            'logradouro': logradouro,
                            'numero': numero,
                            'bairro': bairro,
                            'cidade': cidade,
                            'estado': 'BA',
                            'cep': '45820000',
                        }
                        if mapa_instituicoes and instituicao_select.value is not None:
                            payload['instituicaoId'] = int(instituicao_select.value)
                        if mapa_cooperativas and cooperativa_select.value is not None:
                            payload['cooperativaId'] = int(cooperativa_select.value)

                        try:
                            criada = agendar_coleta(payload)
                            novo_id = criada.get('id')
                            ui.notify(
                                f'Solicitação #{novo_id or ""} enviada com sucesso!',
                                type='positive',
                            )
                            data.value = ''
                            endereco_coleta.value = ''
                            observacoes.value = ''
                            if carregar_coletas_ref[0]:
                                carregar_coletas_ref[0](
                                    notificar_erro=True,
                                    focar_id=int(novo_id) if novo_id is not None else None,
                                )
                        except ApiError as exc:
                            ui.notify(exc.message, type='negative')

                    eco_button('Enviar Solicitação', enviar, cls='dark eco-block')

            with card():
                status_card_container = ui.element('div')

                def render_timeline(coleta_id: int | None) -> None:
                    status_card_container.clear()
                    with status_card_container:
                        if not coleta_id:
                            h('<div class="card-head"><div><div class="card-title">'
                              'Status da solicitação</div></div></div>'
                              '<div class="muted" style="font-size:12px;padding:8px 0">'
                              'Nenhuma solicitação selecionada.</div>')
                            return
                        h(f'<div class="card-head"><div><div class="card-title">'
                          f'Status da solicitação # {coleta_id}</div></div></div>')
                        try:
                            eventos = buscar_timeline_coleta(coleta_id)
                            etapas = _converter_timeline_para_etapas(eventos)
                            if etapas:
                                timeline(etapas)
                            else:
                                h('<div class="muted" style="font-size:12px;padding:8px 0">'
                                  'Sem eventos registrados para esta solicitação.</div>')
                        except ApiError as exc:
                            if exc.status_code != 404:
                                ui.notify(exc.message, type='warning')
                            h('<div class="muted" style="font-size:12px;padding:8px 0">'
                              'Sem eventos registrados para esta solicitação.</div>')

                render_timeline_ref[0] = render_timeline
                render_timeline(None)

        with ui.element('div').classes('narrow'):
            with card('Históricos de coletas'):
                historico_container = ui.element('div')

                def carregar_coletas(notificar_erro: bool = False, focar_id: int | None = None) -> None:
                    historico_container.clear()
                    with historico_container:
                        try:
                            coletas_raw = listar_minhas_coletas()
                            coletas = _ordenar_coletas_canceladas_por_ultimo(coletas_raw)
                            if not coletas:
                                h('<div class="muted" style="font-size:12px;padding:8px 0">'
                                  'Nenhuma coleta agendada até o momento.</div>')
                                if render_timeline_ref[0]:
                                    render_timeline_ref[0](None)
                                return

                            alvo_id = focar_id or selecionada_id['id'] or coletas[0].get('id')
                            if alvo_id is not None:
                                selecionada_id['id'] = int(alvo_id)

                            def acao_linha(idx: int, _row: tuple, cols: list[dict[str, Any]] = coletas) -> None:
                                item = cols[idx]
                                if item.get('id') is not None:
                                    cid_int = int(item['id'])
                                    selecionada_id['id'] = cid_int
                                    if render_timeline_ref[0]:
                                        render_timeline_ref[0](cid_int)
                                abrir_modal_status(item)

                            data_table(
                                _converter_coletas_para_tabela(coletas),
                                action='Status',
                                on_action=acao_linha,
                            )
                            if render_timeline_ref[0]:
                                render_timeline_ref[0](selecionada_id['id'])
                        except ApiError as exc:
                            if notificar_erro:
                                ui.notify(exc.message, type='warning')
                            h('<div class="muted" style="font-size:12px;padding:8px 0">'
                              'Nenhuma coleta agendada até o momento.</div>')
                            if render_timeline_ref[0]:
                                render_timeline_ref[0](None)

                carregar_coletas_ref[0] = carregar_coletas
                carregar_coletas(notificar_erro=False)


@ui.page('/materiais')
def materiais() -> None:
    with layout('/materiais'):
        page_header('Controle de materiais recicláveis',
                    'Registro, rastreamento e situação de todos os resíduos processados')
        kpi_row(m.KPIS_MATERIAIS)

        catalogo = _carregar_materiais_catalogo_api()
        mapa_opcoes: dict[str, str] = {}
        for item in catalogo:
            cod = str(item.get('codigo') or '').strip().upper()
            nome_mat = str(item.get('nome') or cod).strip()
            if cod:
                mapa_opcoes[cod] = nome_mat

        codigo_inicial = next(iter(mapa_opcoes.keys()), 'PAPEL')
        detalhe_box_ref: list[Any] = [None]

        def _render_detalhe_material(detalhe: dict[str, Any], qtd_informada: float = 0.0) -> None:
            if detalhe_box_ref[0] is None:
                return
            detalhe_box_ref[0].clear()
            with detalhe_box_ref[0]:
                cod = str(detalhe.get('codigo') or '').strip().upper()
                nome_mat = str(detalhe.get('nome') or cod or 'Material').strip()
                desc_mat = str(detalhe.get('descricao') or '').strip()
                unid = str(detalhe.get('unidadePadrao') or 'kg').strip()
                fator_raw = detalhe.get('fatorCarbonoEvitadoKg')

                partes_meta: list[str] = [f'Código: {cod}', f'Unidade padrão: {unid}']
                if fator_raw is not None:
                    fator_num = float(fator_raw)
                    partes_meta.append(
                        f'Fator CO₂ evitado: {_formatar_numero_pt(fator_num)} kg/{unid}'
                    )
                    if qtd_informada > 0:
                        co2_est = qtd_informada * fator_num
                        partes_meta.append(
                            f'Estimativa para {_formatar_numero_pt(qtd_informada)} {unid}: '
                            f'{_formatar_numero_pt(co2_est)} kg CO₂e'
                        )

                desc_html = (
                    f'<div class="muted" style="font-size:11px;margin-top:4px">{escape(desc_mat)}</div>'
                    if desc_mat
                    else ''
                )
                h(
                    f'<div style="margin-top:14px;padding:12px 14px;border-radius:10px;'
                    f'background:#F5F7F8;border:1px solid #E5E7EB">'
                    f'<div style="font-size:13px;font-weight:600;color:{PRIMARY}">'
                    f'{escape(nome_mat)}</div>'
                    f'{desc_html}'
                    f'<div style="font-size:11px;margin-top:6px">'
                    f'{escape(" • ".join(partes_meta))}</div>'
                    f'</div>'
                )

        with card('Registrar entrada de material'):
            with ui.element('div').classes('form-grid cols-4'):
                with ui.element('div'):
                    h('<label class="form-label upper">Tipo</label>')
                    tipo_sel = ui.select(
                        mapa_opcoes,
                        value=codigo_inicial,
                    ).props(
                        'outlined dense hide-bottom-space options-dense aria-label="Tipo"'
                    ).classes('eco-field')
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
                        cod_escolhido = str(tipo_sel.value or codigo_inicial).strip().upper()
                        try:
                            qtd_num = float(str(peso.value or '0').replace(',', '.'))
                        except ValueError:
                            qtd_num = 0.0

                        try:
                            detalhe = buscar_material_por_codigo(cod_escolhido)
                            _render_detalhe_material(detalhe, qtd_informada=qtd_num)
                            nome_mat = str(detalhe.get('nome') or cod_escolhido)
                            ui.notify(f'Material {nome_mat} consultado na API!', type='positive')
                        except ApiError as exc:
                            ui.notify(exc.message, type='warning')

                with ui.element('div').classes('cell-end'):
                    eco_button('REGISTRAR', registrar, cls='eco-block')

            detalhe_box_ref[0] = ui.element('div').classes('w-full')

        with card('Últimos registros', more='Ver mais'):
            data_table(m.ULTIMOS_REGISTROS)


@ui.page('/ecoscore')
def ecoscore() -> None:
    with layout('/ecoscore'):
        page_header('EcoScore',
                    'Pontuação por descarte correto - acompanhe seu desempenho e evolua')
        dados_es = _montar_dados_ecoscore()

        with grid('2'):
            with card(f'Seu Score - {dados_es["nome_exibicao"]}'):
                pct_sc = dados_es['pct_score']
                anel = donut([m.Slice('Score', pct_sc, '#4ADE80'),
                              m.Slice('Restante', 100 - pct_sc, '#D9D9D9')],
                             size=72, thickness=22)
                h(f'''<div style="display:flex;align-items:center;gap:20px;margin-bottom:18px">
                        {anel}
                        <div>
                          <div style="font-size:20px;font-weight:600">{escape(dados_es["pontos_fmt"])}</div>
                          <div class="badge b-gold" style="margin:6px 0;font-size:11px">{escape(dados_es["nivel"])}</div>
                          <div class="muted" style="font-size:11px">
                            {escape(dados_es["meta_texto"])}</div>
                        </div>
                      </div>
                      <div style="font-size:12px;font-weight:500;margin-bottom:6px">Critérios de pontuação</div>''')
                h('<div class="muted" style="font-size:12px;padding:6px 0">'
                  'Detalhamento por critério não disponível na API no momento.</div>')

            with card():
                meses_select = [
                    f'{_MESES_CURTOS[mes - 1]} {ano}'
                    for ano, mes in reversed(_ultimos_5_meses_base_atual())
                ]
                with ui.element('div').classes('card-head'):
                    titulo = h('<div class="card-title">Ranking geral</div>')
                    mes = ui.select(meses_select, value=meses_select[0]).props(
                        'outlined dense hide-bottom-space options-dense aria-label="Mês do ranking"'
                    ).classes('eco-field').style('width:150px')
                    titulo.bind_content_from(
                        mes, 'value', lambda v: f'<div class="card-title">Ranking geral - {v}</div>')
                if dados_es['ranking']:
                    total_p = dados_es['total_participantes']
                    rodape_rank = (
                        f'{total_p} participante{"s" if total_p != 1 else ""} no ranking'
                    )
                    rank_list(
                        dados_es['ranking'],
                        divided=True,
                        top_colored=2,
                        green_value=True,
                        footer=rodape_rank,
                    )
                else:
                    h('<div class="muted" style="font-size:12px;padding:8px 0">'
                      'Nenhum participante pontuado no ranking até o momento.</div>')

        with ui.element('div').classes('narrow-45'):
            with card('Materiais coletados - 05 meses'):
                if dados_es['tem_meses_reais']:
                    bar_chart(dados_es['meses'])
                else:
                    h('<div class="muted" style="font-size:12px;padding:8px 0">'
                      'Nenhuma coleta com peso registrado nos últimos 5 meses até o momento.</div>')


@ui.page('/ecoselo')
def ecoselo() -> None:
    with layout('/ecoselo'):
        page_header('Certificação ambiental',
                    'Selos de desempenho para reconhecer instituições sustentáveis')
        dados_selo = _montar_dados_ecoselo()
        kpi_row(dados_selo['kpis'])

        with grid('2'):
            with card():
                destaque = dados_selo['destaque']
                if destaque:
                    nome_selo = str(
                        destaque.get('nome')
                        or destaque.get('titulo')
                        or destaque.get('nivel')
                        or destaque.get('codigo')
                        or 'EcoSelo'
                    )
                    entidade_selo = str(
                        destaque.get('instituicaoNome')
                        or destaque.get('nomeInstituicao')
                        or destaque.get('descricao')
                        or ''
                    )
                    validade_selo = str(
                        destaque.get('validade')
                        or destaque.get('dataValidade')
                        or ''
                    )
                    h(f'''<div style="display:flex;align-items:center;justify-content:space-between;
                                     gap:16px;margin-bottom:18px">
                            <div>
                              <div style="color:{PRIMARY};font-size:16px;font-weight:600">
                                {escape(nome_selo)}</div>
                              <div style="font-size:12px;font-weight:500">{escape(entidade_selo)}</div>
                              <div class="muted" style="font-size:11px;margin-top:14px">
                                {escape(validade_selo)}</div>
                            </div>
                            <div class="seal"><div class="seal-inner">Eco</div></div>
                          </div>''')
                else:
                    h('<div class="muted" style="font-size:12px;padding:8px 0">'
                      'Nenhum selo conquistado ou vinculado até o momento.</div>')

            with card('Painel de certificação'):
                if dados_selo['selos']:
                    rows_selo: list[tuple] = []
                    for s in dados_selo['selos']:
                        nome_item = str(
                            s.get('nome')
                            or s.get('titulo')
                            or s.get('instituicaoNome')
                            or s.get('codigo')
                            or f"Selo #{s.get('id') or '-'}"
                        )
                        pts_item = str(s.get('pontuacao') or s.get('score') or '-')
                        nivel_item = str(s.get('nivel') or s.get('selo') or s.get('status') or 'Ativo')
                        rows_selo.append((nome_item, pts_item, m.Badge(nivel_item)))
                    data_table(m.Table(columns=('Selo / Instituição', 'Score', 'Status'), rows=rows_selo))
                else:
                    h('<div class="muted" style="font-size:12px;padding:8px 0">'
                      'Nenhuma certificação registrada até o momento.</div>')

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
        dados_ei = _montar_dados_ecoimpacto()
        kpi_row(dados_ei['kpis'])

        with grid('2'):
            with card('Evolução de impacto - 05 meses'):
                h('<div class="muted" style="font-size:12px;margin:-4px 0 10px">'
                  'CO₂ evitado, água e energia poupada em escala agregada</div>')
                if dados_ei['tem_meses_reais']:
                    bar_chart(dados_ei['meses'])
                else:
                    h('<div class="muted" style="font-size:12px;padding:8px 0">'
                      'Nenhuma coleta com peso registrado nos últimos 5 meses até o momento.</div>')
            with card('Distribuição por material'):
                if dados_ei['slices']:
                    donut_with_legend(dados_ei['slices'])
                else:
                    h('<div class="muted" style="font-size:12px;padding:8px 0">'
                      'Nenhum material reciclado registrado até o momento.</div>')

        with grid('3'):
            with card('Metas ESG', cls='card-sm'):
                h('<div class="muted" style="font-size:12px;padding:8px 0">'
                  'Nenhuma meta ESG configurada no sistema até o momento.</div>')
            with card('Equivalências ambientais', cls='card-sm'):
                item_list(dados_ei['equivalencias'], big_title=True)
            with card('Impacto por instituição', cls='card-sm'):
                if dados_ei['impacto_inst']:
                    metric_list(dados_ei['impacto_inst'])
                else:
                    h('<div class="muted" style="font-size:12px;padding:8px 0">'
                      'Nenhum impacto por instituição registrado até o momento.</div>')


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
    if not obter_token():
        ui.navigate.to('/')
        return

    if not eh_administrador():
        ui.notify('Acesso restrito: apenas administradores podem acessar /administracao.', type='negative')
        ui.navigate.to('/dashboard')
        return

    with layout('/administracao'):
        page_header('Usuários e instituições',
                    'Controle de perfis, permissões e aprovação de cadastros')

        kpis_container = ui.element('div').classes('w-full')

        def atualizar_kpis_admin() -> None:
            kpis_container.clear()
            with kpis_container:
                kpi_row(_montar_kpis_admin())

        atualizar_kpis_admin()

        dialog_analise = ui.dialog()
        dialog_editar_entidade = ui.dialog()
        dialog_excluir_entidade = ui.dialog()
        carregar_pendentes_ref: list[Any] = [None]
        carregar_listas_ref: list[Any] = [None]
        inst_box_ref: list[Any] = [None]
        coop_box_ref: list[Any] = [None]

        def abrir_modal_editar_entidade(entidade: dict[str, Any], categoria: str) -> None:
            """Permite atualizar status/motivo via PATCH /{id}/status; avisa que a API nao edita dados."""
            dialog_editar_entidade.clear()
            eid = entidade.get('id')
            nome_atual = str(entidade.get('nome') or '-')
            doc_atual = str(entidade.get('documento') or '-')
            tipo_raw = str(entidade.get('tipo') or categoria).upper()
            tipo_lbl = _TIPOS_INSTITUICAO_OPCOES.get(tipo_raw, tipo_raw.capitalize())
            status_atual = str(entidade.get('status') or 'AGUARDANDO_ANALISE').upper()
            if status_atual not in _STATUS_CADASTRO_OPCOES:
                status_atual = 'AGUARDANDO_ANALISE'

            with dialog_editar_entidade, ui.card().style('width:460px;max-width:95vw;border-radius:16px;padding:20px'):
                h(
                    f'<div class="card-title" style="margin-bottom:6px">'
                    f'Editar {escape(categoria.lower())} #{eid or "-"}</div>'
                    f'<div class="muted" style="font-size:12px;margin-bottom:12px">'
                    f'Tipo: <strong>{escape(tipo_lbl)}</strong> • Doc: {escape(doc_atual)}<br>'
                    f'Nota: a API não possui endpoint de edição de dados cadastrais, '
                    f'apenas alteração de status.</div>'
                )

                with ui.element('div').classes('form-grid'):
                    campo_nome = form_field('Nome', value=nome_atual)
                    with ui.element('div'):
                        h('<label class="form-label">Status do cadastro</label>')
                        sel_status = ui.select(
                            _STATUS_CADASTRO_OPCOES,
                            value=status_atual,
                        ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')
                    campo_motivo = form_field(
                        'Motivo (opcional)',
                        value=str(entidade.get('motivoRejeicao') or ''),
                        placeholder='Ex: Documentação conferida ou rejeitada',
                    )

                    def salvar_entidade() -> None:
                        novo_nome = (campo_nome.value or '').strip()
                        novo_st = str(sel_status.value or status_atual).upper()
                        motivo_txt = (campo_motivo.value or '').strip()

                        if novo_nome != nome_atual:
                            ui.notify(
                                'A API não consegue alterar o nome ou dados cadastrais; '
                                'apenas o status foi enviado.',
                                type='warning',
                            )

                        if eid is None:
                            ui.notify('Registro sem ID válido.', type='warning')
                            return

                        try:
                            if categoria == 'COOPERATIVA':
                                atualizar_status_cooperativa(
                                    int(eid),
                                    novo_st,
                                    motivo=motivo_txt or None,
                                )
                            else:
                                atualizar_status_instituicao(
                                    int(eid),
                                    novo_st,
                                    motivo=motivo_txt or None,
                                )
                            ui.notify(f'Status de {nome_atual} atualizado!', type='positive')
                            dialog_editar_entidade.close()
                            atualizar_kpis_admin()
                            if carregar_listas_ref[0]:
                                carregar_listas_ref[0]()
                        except ApiError as exc:
                            ui.notify(exc.message, type='negative')

                    with ui.row().classes('justify-end gap-2').style('margin-top:8px'):
                        eco_button('Cancelar', dialog_editar_entidade.close, cls='outline')
                        eco_button(
                            'Salvar status',
                            salvar_entidade,
                            cls='dark',
                            props='unelevated no-caps text-white',
                        ).style('background-color: #17693A !important; color: #FFFFFF !important;')

            dialog_editar_entidade.open()

        def abrir_modal_excluir_entidade(entidade: dict[str, Any], categoria: str) -> None:
            """Avisa que a API atual nao possui endpoint DELETE para instituicao/cooperativa."""
            dialog_excluir_entidade.clear()
            eid = entidade.get('id')
            nome_ent = str(entidade.get('nome') or f'{categoria.capitalize()} #{eid or "-"}')

            with dialog_excluir_entidade, ui.card().style('width:420px;max-width:95vw;border-radius:16px;padding:20px'):
                h(
                    f'<div class="card-title" style="margin-bottom:8px">Excluir {escape(categoria.lower())}</div>'
                    f'<div style="font-size:12px;margin-bottom:14px">'
                    f'A API atual não possui endpoint <strong>DELETE</strong> para excluir '
                    f'<strong>{escape(nome_ent)}</strong>.</div>'
                )

                with ui.row().classes('justify-end gap-2').style('margin-top:8px'):
                    eco_button('Fechar', dialog_excluir_entidade.close, cls='outline')

            dialog_excluir_entidade.open()

        def _render_lista_entidade(
            container: ui.element,
            itens: list[dict[str, Any]],
            categoria: str,
            mensagem_vazia: str,
        ) -> None:
            container.clear()
            with container:
                if not itens:
                    h(f'<div class="muted" style="font-size:12px;padding:8px 0">'
                      f'{escape(mensagem_vazia)}</div>')
                    return

                with ui.element('div').classes('list-divided'):
                    for item in itens:
                        nome_e = str(item.get('nome') or f"{categoria.capitalize()} #{item.get('id')}")
                        doc_e = str(item.get('documento') or '').strip()
                        bairro_e = str(item.get('bairro') or '').strip()
                        cidade_e = str(item.get('cidade') or '').strip()
                        local_e = (
                            f'{bairro_e} - {cidade_e}'
                            if (bairro_e and cidade_e)
                            else (bairro_e or cidade_e or '')
                        )
                        st_raw = str(item.get('status') or 'ATIVO').upper()
                        st_lbl = _STATUS_CADASTRO_OPCOES.get(st_raw, st_raw.capitalize())
                        tone = 'green' if st_raw == 'ATIVO' else ('amber' if 'ANALISE' in st_raw else 'gray')

                        meta_partes = [p for p in [doc_e, local_e] if p]
                        meta_txt = ' • '.join(meta_partes) if meta_partes else categoria.capitalize()

                        with ui.element('div').classes('item'):
                            with ui.element('div').style('flex:1;min-width:0'):
                                h(f'<div class="item-title">{escape(nome_e)}</div>'
                                  f'<div class="item-meta">{escape(meta_txt)}</div>')
                            h(f'<span class="badge b-{tone}" style="margin-right:6px">'
                              f'{escape(st_lbl)}</span>')
                            with ui.row().classes('gap-1 items-center'):
                                ui.button(
                                    'Editar',
                                    on_click=lambda _e, ent=item, cat=categoria: abrir_modal_editar_entidade(ent, cat),
                                ).props('flat dense no-caps').classes('btn-ghost')
                                ui.button(
                                    'Excluir',
                                    on_click=lambda _e, ent=item, cat=categoria: abrir_modal_excluir_entidade(ent, cat),
                                ).props('flat dense no-caps').classes('btn-ghost').style(
                                    'border-color:#B91C1C !important;color:#B91C1C !important;'
                                )

        def carregar_listas_admin() -> None:
            if inst_box_ref[0] is not None:
                try:
                    todas_inst = listar_instituicoes()
                    somente_inst = [
                        i for i in todas_inst
                        if str(i.get('tipo') or '').upper() != 'COOPERATIVA'
                    ]
                    _render_lista_entidade(
                        inst_box_ref[0],
                        somente_inst,
                        'INSTITUICAO',
                        'Nenhuma instituição vinculada até o momento.',
                    )
                except ApiError as exc:
                    ui.notify(exc.message, type='warning')
                    _render_lista_entidade(
                        inst_box_ref[0],
                        [],
                        'INSTITUICAO',
                        'Nenhuma instituição vinculada até o momento.',
                    )

            if coop_box_ref[0] is not None:
                try:
                    coops = listar_cooperativas()
                    _render_lista_entidade(
                        coop_box_ref[0],
                        coops,
                        'COOPERATIVA',
                        'Nenhuma cooperativa vinculada até o momento.',
                    )
                except ApiError as exc:
                    ui.notify(exc.message, type='warning')
                    _render_lista_entidade(
                        coop_box_ref[0],
                        [],
                        'COOPERATIVA',
                        'Nenhuma cooperativa vinculada até o momento.',
                    )

        carregar_listas_ref[0] = carregar_listas_admin

        def abrir_modal_analise(usuario: dict[str, Any]) -> None:
            dialog_analise.clear()
            uid = usuario.get('id')
            if not uid:
                ui.notify('Cadastro sem ID válido.', type='warning')
                return

            nome_u = str(usuario.get('nome') or f'Usuário #{uid}')
            email_u = str(usuario.get('email') or '-')
            doc_u = str(usuario.get('documento') or '-')
            perfil_raw = str(usuario.get('perfil') or 'USUARIO').upper()
            perfil_lbl = _PERFIL_LABEL.get(perfil_raw, perfil_raw)
            status_atual = str(usuario.get('status') or 'AGUARDANDO_ANALISE').upper()
            status_lbl = _STATUS_CADASTRO_OPCOES.get(status_atual, status_atual)

            with dialog_analise, ui.card().style('width:460px;max-width:95vw;border-radius:16px;padding:20px'):
                h(
                    f'<div class="card-title" style="margin-bottom:6px">Analisar cadastro #{uid}</div>'
                    f'<div class="muted" style="font-size:12px;margin-bottom:10px">'
                    f'<strong>{escape(nome_u)}</strong> • {escape(perfil_lbl)}<br>'
                    f'E-mail: {escape(email_u)} • Doc: {escape(doc_u)}</div>'
                    f'<div style="margin-bottom:14px">'
                    f'<span class="badge b-amber">Status atual: {escape(status_lbl)}</span></div>'
                )

                campo_motivo = form_field(
                    'Motivo (obrigatório para rejeitar)',
                    placeholder='Ex: Documentação incompleta ou inconsistente',
                )

                def aplicar_status(status_escolhido: str) -> None:
                    motivo_texto = (campo_motivo.value or '').strip()
                    if status_escolhido == 'REJEITADO' and not motivo_texto:
                        ui.notify('Informe o motivo ao rejeitar um cadastro.', type='warning')
                        return

                    try:
                        atualizar_status_usuario(
                            int(uid),
                            status_escolhido,
                            motivo=motivo_texto if status_escolhido == 'REJEITADO' else None,
                        )
                        rotulo = _STATUS_CADASTRO_OPCOES.get(status_escolhido, status_escolhido)
                        ui.notify(f'Cadastro de {nome_u} atualizado para: {rotulo}.', type='positive')
                        dialog_analise.close()
                        atualizar_kpis_admin()
                        carregar_listas_admin()
                        if carregar_pendentes_ref[0]:
                            carregar_pendentes_ref[0](notificar_erro=True)
                    except ApiError as exc:
                        ui.notify(exc.message, type='negative')

                with ui.row().classes('justify-between items-center w-full').style('margin-top:12px'):
                    eco_button('Cancelar', dialog_analise.close, cls='outline')
                    with ui.row().classes('gap-2'):
                        eco_button(
                            'Rejeitar',
                            lambda: aplicar_status('REJEITADO'),
                            cls='outline',
                        )
                        eco_button(
                            'Aprovar',
                            lambda: aplicar_status('ATIVO'),
                            cls='dark',
                            props='unelevated no-caps text-white',
                        ).style('background-color: #17693A !important; color: #FFFFFF !important;')

            dialog_analise.open()

        with grid('65'):
            with ui.element('div').classes('stack'):
                with card('Instituições'):
                    inst_box_ref[0] = ui.element('div').classes('w-full')

                with card('Cooperativas'):
                    coop_box_ref[0] = ui.element('div').classes('w-full')

            with card('Cadastros em análise', cls='card-sm'):
                pendentes_container = ui.element('div')

                def carregar_pendentes(notificar_erro: bool = False) -> None:
                    pendentes_container.clear()
                    with pendentes_container:
                        try:
                            pendentes = listar_usuarios(status='AGUARDANDO_ANALISE')
                            if not pendentes:
                                h('<div class="muted" style="font-size:12px;padding:8px 0">'
                                  'Nenhum cadastro aguardando aprovação no momento.</div>')
                                return

                            with ui.element('div').classes('list-divided'):
                                for u in pendentes:
                                    nome_u = str(u.get('nome') or f"Usuário #{u.get('id')}")
                                    perfil_raw = str(u.get('perfil') or 'USUARIO').upper()
                                    tipo_lbl = _PERFIL_LABEL.get(perfil_raw, perfil_raw.capitalize())
                                    with ui.element('div').classes('item'):
                                        with ui.element('div').style('flex:1'):
                                            h(f'<div class="item-title">{escape(nome_u)}</div>'
                                              f'<div class="item-meta">{escape(tipo_lbl)}</div>')
                                        ui.button(
                                            'Analisar',
                                            on_click=lambda _e, item=u: abrir_modal_analise(item),
                                        ).props('flat dense no-caps').classes('badge b-amber').style(
                                            'border:none;cursor:pointer'
                                        )
                        except ApiError as exc:
                            if notificar_erro:
                                ui.notify(exc.message, type='warning')
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

                carregar_pendentes_ref[0] = carregar_pendentes
                carregar_pendentes(notificar_erro=False)

        carregar_listas_admin()


def _tela_rota(rota_atual: str, dados: m.RotaView, acao_topo, acao_principal) -> None:
    """3 telas para rotas."""
    with layout(rota_atual):
        page_header(dados.page_title, dados.page_subtitle)

        with grid('65'):
            with card():
                with ui.element('div').classes('card-head'):
                    h(f'<div>'
                      f'<div class="card-title" style="font-size:16px;font-weight:600">'
                      f'{escape(dados.nome_rota)}</div>'
                      f'<div class="card-subtitle" style="font-size:11px;margin-top:4px">'
                      f'{dados.paradas_total} paradas • {escape(dados.distancia)} • '
                      f'duração estimada de {escape(dados.duracao)}</div>'
                      f'</div>')
                    eco_button(dados.btn_topo, acao_topo).style('min-width:155px;height:32px')

                route_map_html()

            with card('Resumo da rota'):
                h(f'''
                <div>
                  <div class="summary-item">
                    <div class="summary-label">Distância total</div>
                    <div class="summary-val">{escape(dados.distancia)}</div>
                  </div>
                  <div class="summary-item">
                    <div class="summary-label">Tempo estimado</div>
                    <div class="summary-val">{escape(dados.duracao)}</div>
                  </div>
                  <div class="summary-item">
                    <div class="summary-label">Carga prevista</div>
                    <div class="summary-val">{escape(dados.carga)}</div>
                  </div>
                  <div class="summary-item">
                    <div class="summary-label">Emissão evitada</div>
                    <div class="summary-val green">{escape(dados.co2)}</div>
                  </div>
                </div>
                ''')

                pct = int((dados.paradas_concluidas / dados.paradas_total) * 100)
                h(f'''
                <div style="margin: 14px 0 16px">
                  <div style="font-size:11px;font-weight:500;margin-bottom:6px">Execução da rota</div>
                  <div style="display:flex;align-items:center;gap:10px">
                    <div class="track" style="height:7px">
                      <div class="fill" style="width:{pct}%"></div>
                    </div>
                    <span style="font-size:9px;color:{PRIMARY};font-weight:500;white-space:nowrap">
                      {dados.paradas_concluidas} de {dados.paradas_total} paradas
                    </span>
                  </div>
                </div>
                ''')

                eco_button(dados.btn_acao, acao_principal, cls='eco-block')

        with card('Sequência de paradas'):
            data_table(dados.tabela_paradas)


@ui.page('/rotas')
def planejamento_rotas() -> None:
    _tela_rota(
        rota_atual='/rotas',
        dados=m.ROTA_PLANEJAMENTO,
        acao_topo=lambda: ui.navigate.to('/rotas/otimizada'),
        acao_principal=lambda: ui.navigate.to('/rotas/em-andamento'),
    )


@ui.page('/rotas/otimizada')
def rota_otimizada() -> None:
    _tela_rota(
        rota_atual='/rotas/otimizada',
        dados=m.ROTA_OTIMIZADA,
        acao_topo=lambda: ui.notify('Rota recalculada com os parâmetros mais recentes!', type='positive'),
        acao_principal=lambda: ui.navigate.to('/rotas/em-andamento'),
    )


@ui.page('/rotas/em-andamento')
def coleta_em_andamento() -> None:
    _tela_rota(
        rota_atual='/rotas/em-andamento',
        dados=m.ROTA_ANDAMENTO,
        acao_topo=lambda: ui.notify('Rota recalculada com sucesso!', type='positive'),
        acao_principal=lambda: (
            ui.notify('Rota pausada.', type='warning'),
            ui.navigate.to('/rotas/otimizada'),
        ),
    )