"""As telas internas do EcoFlux.

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
    buscar_coleta_por_id,
    buscar_ecoimpacto_global,
    buscar_material_por_codigo,
    buscar_meu_ecoimpacto,
    buscar_meu_ecoscore,
    buscar_timeline_coleta,
    cancelar_coleta,
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
    atualizar_sessao_atual,
    cadastrar_instituicao,
    desvincular_membro_instituicao,
    listar_membros_instituicao,
    listar_meus_vinculos_instituicao,
    vincular_membro_instituicao,
    atribuir_motorista_coleta,
    concluir_entrega_motorista,
    listar_minha_rota_motorista,
    obter_perfil_atual,
)
import mocks as m
from ui_kit import (
    PRIMARY, bar_chart, card, chip_select, data_table, donut, donut_with_legend,
    dot_steps, form_field, grid, h, icon, item_list, kpi_row, layout, metric_list,
    eco_button, numbered_steps, page_header, progress_footer, rank_list,
    stacked_metric_list, timeline,
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
    'EM_ANDAMENTO': 'Em andamento',
    'CONCLUIDA': 'Concluída',
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

_CONFIRMACOES_ROTAS_ADMIN: list[dict[str, Any]] = []


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

        origem_lbl = str(
            c.get('nomeInstituicao')
            or c.get('nomeCooperativa')
            or c.get('nomeSolicitante')
            or c.get('bairro')
            or '-'
        )

        st_raw = str(c.get('status') or 'AGENDADA').upper()
        st_lbl = _STATUS_COLETA_OPCOES.get(st_raw, st_raw.replace('_', ' ').capitalize())
        rows.append((data_fmt, mat_lbl, qtd_str, origem_lbl, m.Badge(st_lbl)))

    return m.Table(
        columns=('Data', 'Material', 'Quantidade', 'Origem / Entidade', 'Status'),
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
        autor = str(ev.get('nomeAutor') or '').strip()
        meta_partes = [
            p for p in [
                f'{data_fmt} - {hora_reg}' if hora_reg else data_fmt,
                obs,
                f'Por {autor}' if autor else '',
            ]
            if p
        ]
        etapas.append(m.Step(title=titulo, meta=' • '.join(meta_partes), done=True))

    fluxo_padrao = [
        ('AGENDADA', 'Agendada'),
        ('EM_ROTA', 'Em rota'),
        ('CONCLUIDA', 'Concluída'),
    ]
    if ultimo_status not in ('CONCLUIDA', 'REALIZADA', 'CANCELADA'):
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
            c for c in coletas
            if str(c.get('status') or '').strip().upper() in ('REALIZADA', 'CONCLUIDA')
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
            nome_inst_resp = str(c.get('nomeInstituicao') or '').strip()
            inst_id = c.get('idInstituicao') if c.get('idInstituicao') is not None else c.get('instituicaoId')
            origem = (
                nome_inst_resp
                or (
                    mapa_inst.get(int(inst_id))
                    if inst_id is not None and int(inst_id) in mapa_inst
                    else (str(c.get('bairro') or '').strip() or 'Solicitação própria')
                )
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

        nome_inst_resp = str(c.get('nomeInstituicao') or '').strip()
        inst_id = c.get('idInstituicao') if c.get('idInstituicao') is not None else c.get('instituicaoId')
        if nome_inst_resp:
            nome_inst = nome_inst_resp
        elif inst_id is not None and int(inst_id) in instituicoes_mapa:
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
    if not obter_token():
        ui.navigate.to('/')
        return

    atualizar_sessao_atual()
    perfil_atual = (obter_perfil_atual() or '').upper()
    pode_gerenciar = perfil_atual in ('INSTITUICAO', 'EMPRESA', 'ADMINISTRADOR')

    with layout('/pontos-de-coleta'):
        page_header(
            'Pontos de coleta',
            'Cadastre, filtre e gerencie os pontos de descarte na plataforma'
            if pode_gerenciar
            else 'Consulte e filtre os pontos de descarte disponíveis na plataforma',
        )

        dialog_editar = ui.dialog()
        carregar_tabela_ref: list[Any] = [None]

        mapa_inst_usuario: dict[int, str] = {}
        if pode_gerenciar:
            try:
                lista_inst = listar_instituicoes() if eh_administrador() else listar_minhas_instituicoes()
                for inst in lista_inst:
                    if inst.get('id') is not None:
                        iid = int(inst['id'])
                        mapa_inst_usuario[iid] = str(inst.get('nome') or f'Instituição #{iid}')
            except ApiError:
                pass

        def _montar_payload_ponto_campos(
            *,
            nome: str,
            responsavel: str,
            telefone: str,
            horarios: str,
            logradouro: str,
            numero: str,
            bairro: str,
            cidade: str,
            estado: str,
            cep: str,
            materiais_selecionados: set[str],
            instituicao_id: int | None = None,
            latitude: float = -16.3766,
            longitude: float = -39.5808,
        ) -> dict[str, Any] | None:
            tel_digitos = re.sub(r'\D', '', telefone or '')
            if tel_digitos and len(tel_digitos) not in (10, 11):
                ui.notify('Informe o telefone com DDD (10 ou 11 dígitos, ex: 73999999999).', type='warning')
                return None

            uf_val = (estado or '').strip().upper()
            if len(uf_val) != 2 or not uf_val.isalpha():
                ui.notify('Informe a UF com 2 letras (ex: BA).', type='warning')
                return None

            cep_digitos = re.sub(r'\D', '', cep or '')
            if len(cep_digitos) != 8:
                ui.notify('Informe um CEP válido com 8 dígitos (somente números).', type='warning')
                return None

            mats_enum = _normalizar_materiais(materiais_selecionados)
            if not mats_enum:
                ui.notify('Selecione ao menos um material aceito.', type='warning')
                return None

            payload: dict[str, Any] = {
                'nome': nome.strip()[:150],
                'nomeResponsavel': responsavel.strip()[:120],
                'telefoneContato': tel_digitos or None,
                'horarioFuncionamento': (horarios or '').strip()[:120] or 'Seg - Sex 08h às 17h',
                'logradouro': logradouro.strip()[:150],
                'numero': numero.strip()[:30],
                'bairro': bairro.strip()[:100],
                'cidade': cidade.strip()[:100],
                'estado': uf_val,
                'cep': cep_digitos,
                'latitude': latitude,
                'longitude': longitude,
                'materiaisAceitos': mats_enum,
            }
            if instituicao_id is not None:
                payload['instituicaoId'] = int(instituicao_id)
            return payload

        def abrir_modal_edicao(ponto: dict[str, Any]) -> None:
            dialog_editar.clear()
            ponto_id = ponto.get('id')
            if not ponto_id:
                ui.notify('Ponto sem ID válido para edição.', type='warning')
                return

            mats_iniciais = {
                _ENUM_PARA_CHIP.get(str(x).upper(), str(x).capitalize())
                for x in (ponto.get('materiaisAceitos') or [])
            }
            mats_iniciais = {x for x in mats_iniciais if x in m.MATERIAIS_ACEITOS}

            status_atual = str(ponto.get('status') or 'ATIVO').upper()
            if status_atual not in _STATUS_PONTO_OPCOES:
                status_atual = 'ATIVO'

            inst_atual = ponto.get('idInstituicao')
            inst_inicial = int(inst_atual) if inst_atual is not None and int(inst_atual) in mapa_inst_usuario else None

            with dialog_editar, ui.card().style('width:580px;max-width:95vw;border-radius:16px;padding:20px'):
                h(f'<div class="card-title" style="margin-bottom:12px">Editar ponto #{ponto_id}</div>')
                with ui.element('div').classes('form-grid'):
                    ed_nome = form_field('Nome do ponto', value=str(ponto.get('nome') or ''))
                    with ui.element('div').classes('form-grid cols-2'):
                        ed_resp = form_field('Responsável', value=str(ponto.get('nomeResponsavel') or ''))
                        ed_tel = form_field(
                            'Telefone de contato',
                            value=re.sub(r'\D', '', str(ponto.get('telefoneContato') or '')),
                            placeholder='73999999999',
                            props='mask="###########" inputmode="numeric"',
                        )

                    ed_hor = form_field('Horário de funcionamento', value=str(ponto.get('horarioFuncionamento') or ''))

                    with ui.element('div').classes('form-grid cols-2'):
                        ed_log = form_field('Logradouro', value=str(ponto.get('logradouro') or ''))
                        ed_num = form_field('Número', value=str(ponto.get('numero') or ''))

                    with ui.element('div').classes('form-grid cols-2'):
                        ed_bai = form_field('Bairro', value=str(ponto.get('bairro') or ''))
                        ed_cid = form_field('Cidade', value=str(ponto.get('cidade') or ''))

                    with ui.element('div').classes('form-grid cols-2'):
                        ed_uf = form_field('UF', value=str(ponto.get('estado') or 'BA'), props='mask="AA"')
                        ed_cep = form_field(
                            'CEP',
                            value=re.sub(r'\D', '', str(ponto.get('cep') or '')),
                            props='mask="########" inputmode="numeric"',
                        )

                    if mapa_inst_usuario:
                        with ui.element('div'):
                            h('<label class="form-label">Instituição vinculada (opcional)</label>')
                            ed_inst = ui.select(
                                mapa_inst_usuario,
                                value=inst_inicial,
                            ).props('outlined dense hide-bottom-space options-dense clearable').classes('eco-field')
                    else:
                        ed_inst = None

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
                            'Motivo (obrigatório em Manutenção)',
                            value=str(ponto.get('motivoStatus') or ''),
                            placeholder='Ex: Reforma na estrutura',
                        )

                    def salvar_edicao() -> None:
                        if not _obrigatorios(
                            nome=ed_nome,
                            responsável=ed_resp,
                            horário=ed_hor,
                            logradouro=ed_log,
                            número=ed_num,
                            bairro=ed_bai,
                            cidade=ed_cid,
                            UF=ed_uf,
                            CEP=ed_cep,
                        ):
                            return

                        novo_status = str(ed_status.value or status_atual).upper()
                        motivo_novo = (ed_motivo.value or '').strip()
                        if novo_status == 'MANUTENCAO' and not motivo_novo:
                            ui.notify('Informe o motivo ao colocar o ponto em Manutenção.', type='warning')
                            return

                        payload = _montar_payload_ponto_campos(
                            nome=ed_nome.value,
                            responsavel=ed_resp.value,
                            telefone=ed_tel.value,
                            horarios=ed_hor.value,
                            logradouro=ed_log.value,
                            numero=ed_num.value,
                            bairro=ed_bai.value,
                            cidade=ed_cid.value,
                            estado=ed_uf.value,
                            cep=ed_cep.value,
                            materiais_selecionados=ed_mats,
                            instituicao_id=int(ed_inst.value) if ed_inst is not None and ed_inst.value is not None else inst_atual,
                            latitude=float(ponto['latitude']) if ponto.get('latitude') is not None else -16.3766,
                            longitude=float(ponto['longitude']) if ponto.get('longitude') is not None else -39.5808,
                        )
                        if payload is None:
                            return

                        try:
                            atualizar_ponto_coleta(int(ponto_id), payload)
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

        container_principal = grid('65') if pode_gerenciar else ui.element('div').classes('w-full')
        with container_principal:
            with card('Pontos cadastrados'):
                with ui.element('div').classes('form-grid cols-4').style('margin-bottom:14px'):
                    with ui.element('div'):
                        h('<label class="form-label">Status</label>')
                        filtro_status = ui.select(
                            {'': 'Todos', **_STATUS_PONTO_OPCOES},
                            value='',
                        ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')

                    filtro_bairro = form_field('Bairro', placeholder='Filtrar por bairro')

                    with ui.element('div'):
                        h('<label class="form-label">Material</label>')
                        filtro_material = ui.select(
                            {'': 'Todos', **_OPCOES_TIPO_MATERIAL_COLETA},
                            value='',
                        ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')

                    with ui.element('div').classes('cell-end'):
                        with ui.row().classes('gap-2 no-wrap'):
                            eco_button(
                                'Filtrar',
                                lambda: carregar_tabela(notificar_erro=True),
                                cls='dark',
                            )

                            def _limpar_filtros() -> None:
                                filtro_status.value = ''
                                filtro_bairro.value = ''
                                filtro_material.value = ''
                                carregar_tabela(notificar_erro=False)

                            eco_button('Limpar', _limpar_filtros, cls='outline')

                tabela_container = ui.element('div')

                def carregar_tabela(notificar_erro: bool = False) -> None:
                    tabela_container.clear()
                    with tabela_container:
                        try:
                            pontos = listar_pontos_coleta(
                                status=str(filtro_status.value or '') or None,
                                bairro=(filtro_bairro.value or '').strip() or None,
                                material=str(filtro_material.value or '') or None,
                            )
                            if pontos:
                                if pode_gerenciar:
                                    data_table(
                                        _converter_pontos_para_tabela(pontos),
                                        action='Editar',
                                        on_action=lambda idx, _row, pts=pontos: abrir_modal_edicao(pts[idx]),
                                    )
                                else:
                                    data_table(_converter_pontos_para_tabela(pontos))
                            else:
                                h('<div class="muted" style="font-size:13px;padding:10px 0">'
                                  'Nenhum ponto de coleta encontrado para os filtros informados.</div>')
                        except ApiError as exc:
                            if notificar_erro:
                                ui.notify(exc.message, type='warning')
                            h(f'<div class="muted" style="font-size:13px;padding:10px 0">'
                              f'{escape(exc.message)}</div>')

                carregar_tabela_ref[0] = carregar_tabela
                carregar_tabela(notificar_erro=False)

            if pode_gerenciar:
                with card('Cadastrar novo ponto'):
                    with ui.element('div').classes('form-grid'):
                        nome = form_field('Nome do ponto', placeholder='Ex: Ecoponto Centro')
                        with ui.element('div').classes('form-grid cols-2'):
                            responsavel = form_field('Responsável', placeholder='Nome do responsável')
                            telefone = form_field(
                                'Celular / Telefone',
                                placeholder='73999999999',
                                props='mask="###########" inputmode="numeric"',
                            )

                        horarios = form_field('Horário de funcionamento', placeholder='Seg - Sex 08h às 17h')

                        with ui.element('div').classes('form-grid cols-2'):
                            logradouro = form_field('Logradouro', placeholder='Av. Porto Seguro')
                            numero = form_field('Número', placeholder='120')

                        with ui.element('div').classes('form-grid cols-2'):
                            bairro = form_field('Bairro', placeholder='Centro')
                            cidade = form_field('Cidade', placeholder='Eunápolis')

                        with ui.element('div').classes('form-grid cols-2'):
                            uf = form_field('UF', value='BA', placeholder='BA', props='mask="AA"')
                            cep = form_field(
                                'CEP',
                                placeholder='45820000',
                                props='mask="########" inputmode="numeric"',
                            )

                        if mapa_inst_usuario:
                            with ui.element('div'):
                                h('<label class="form-label">Instituição vinculada (opcional)</label>')
                                inst_select = ui.select(
                                    mapa_inst_usuario,
                                    value=None,
                                ).props('outlined dense hide-bottom-space options-dense clearable').classes('eco-field')
                        else:
                            inst_select = None

                        with ui.element('div'):
                            h('<label class="form-label">Materiais aceitos</label>')
                            materiais = chip_select(m.MATERIAIS_ACEITOS, m.MATERIAIS_PRE_SELECIONADOS)

                        def cadastrar() -> None:
                            if not _obrigatorios(
                                nome=nome,
                                responsável=responsavel,
                                horário=horarios,
                                logradouro=logradouro,
                                número=numero,
                                bairro=bairro,
                                cidade=cidade,
                                UF=uf,
                                CEP=cep,
                            ):
                                return

                            payload = _montar_payload_ponto_campos(
                                nome=nome.value,
                                responsavel=responsavel.value,
                                telefone=telefone.value,
                                horarios=horarios.value,
                                logradouro=logradouro.value,
                                numero=numero.value,
                                bairro=bairro.value,
                                cidade=cidade.value,
                                estado=uf.value,
                                cep=cep.value,
                                materiais_selecionados=materiais,
                                instituicao_id=int(inst_select.value) if inst_select is not None and inst_select.value is not None else None,
                            )
                            if payload is None:
                                return

                            try:
                                criado = criar_ponto_coleta(payload)
                                nome_criado = criado.get('nome') or nome.value
                                ui.notify(f'Ponto "{nome_criado}" cadastrado com sucesso!', type='positive')
                                nome.value = ''
                                responsavel.value = ''
                                telefone.value = ''
                                horarios.value = ''
                                logradouro.value = ''
                                numero.value = ''
                                bairro.value = ''
                                cidade.value = ''
                                cep.value = ''
                                if inst_select is not None:
                                    inst_select.value = None
                                if carregar_tabela_ref[0]:
                                    carregar_tabela_ref[0](notificar_erro=True)
                            except ApiError as exc:
                                ui.notify(exc.message, type='negative')

                        eco_button('Cadastrar ponto', cadastrar, cls='dark eco-block')


@ui.page('/agendamentos')
def agendamentos() -> None:
    if not obter_token():
        ui.navigate.to('/')
        return

    atualizar_sessao_atual()
    perfil_atual = (obter_perfil_atual() or '').upper()
    eh_coop = perfil_atual == 'COOPERATIVA'

    vinculos_morador: list[dict[str, Any]] = []
    try:
        vinculos_morador = [
            v for v in listar_meus_vinculos_instituicao()
            if v.get('ativo', True)
            and str(v.get('papel') or '').upper() == 'MORADOR'
            and v.get('instituicaoId') is not None
        ]
    except ApiError:
        vinculos_morador = []

    eh_morador = len(vinculos_morador) > 0
    pode_agendar = perfil_atual in ('INSTITUICAO', 'EMPRESA', 'ADMINISTRADOR') or eh_morador

    with layout('/agendamentos'):
        page_header(
            'Agendamento de retirada',
            'Solicite a coleta seletiva e acompanhe o status da solicitação'
            if pode_agendar
            else 'Acompanhe o histórico e a linha do tempo das solicitações de coleta',
        )

        selecionada_id: dict[str, int | None] = {'id': None}
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
                    elif str(inst.get('status') or '').upper() == 'ATIVO':
                        mapa_instituicoes[inst_id] = nome_inst
        except ApiError:
            pass

        for v in vinculos_morador:
            iid = int(v['instituicaoId'])
            if iid not in mapa_instituicoes:
                mapa_instituicoes[iid] = str(v.get('nomeInstituicao') or f'Instituição #{iid}')

        if not mapa_instituicoes and eh_administrador():
            try:
                for inst in listar_instituicoes(status='ATIVO'):
                    if inst.get('id') is not None and str(inst.get('tipo') or '').upper() != 'COOPERATIVA':
                        inst_id = int(inst['id'])
                        mapa_instituicoes[inst_id] = str(inst.get('nome') or f'Instituição #{inst_id}')
            except ApiError:
                pass

        try:
            for coop in listar_cooperativas(status='ATIVO'):
                if coop.get('id') is not None:
                    coop_id = int(coop['id'])
                    mapa_cooperativas[coop_id] = str(coop.get('nome') or f'Cooperativa #{coop_id}')
        except ApiError:
            pass

        def _buscar_coletas_disponiveis() -> list[dict[str, Any]]:
            if eh_administrador():
                try:
                    return listar_coletas_operacional()
                except ApiError:
                    pass

            if eh_coop:
                try:
                    minhas_coops = {
                        int(i['id']) for i in listar_minhas_instituicoes()
                        if i.get('id') is not None and str(i.get('tipo') or '').upper() == 'COOPERATIVA'
                    }
                    todas = listar_coletas_operacional()
                    return [
                        c for c in todas
                        if c.get('idCooperativa') is not None and int(c['idCooperativa']) in minhas_coops
                    ]
                except ApiError:
                    pass

            try:
                return listar_minhas_coletas()
            except ApiError:
                return []

        with grid('2'):
            if pode_agendar:
                with card('Nova solicitação'):
                    with ui.element('div').classes('form-grid'):
                        with ui.element('div'):
                            h('<label class="form-label">Instituição solicitante</label>')
                            inst_padrao = next(iter(mapa_instituicoes.keys()), None)
                            props_inst = (
                                'outlined dense hide-bottom-space options-dense aria-label="Instituição solicitante"'
                                + (' clearable' if eh_administrador() else '')
                            )
                            instituicao_select = ui.select(
                                mapa_instituicoes,
                                value=inst_padrao,
                            ).props(props_inst).classes('eco-field')

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

                            if not eh_administrador() and instituicao_select.value is None:
                                ui.notify('Selecione a instituição solicitante.', type='warning')
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
            else:
                with card('Solicitação de coleta'):
                    h(
                        '<div class="muted" style="font-size:13px;padding:8px 0">'
                        'O agendamento de novas retiradas está disponível para instituições aprovadas '
                        '(<strong>Condomínio</strong> ou <strong>Empresa</strong>) e seus <strong>moradores vinculados</strong>.</div>'
                    )
                    if eh_coop:
                        eco_button('Ir para Rotas e escala', lambda: ui.navigate.to('/rotas'), cls='dark')
                    else:
                        eco_button('Cadastrar minha instituição', lambda: ui.navigate.to('/minha-instituicao'), cls='outline')

            with card():
                status_card_container = ui.element('div')

                def render_timeline(coleta_id: int | None) -> None:
                    status_card_container.clear()
                    with status_card_container:
                        if not coleta_id:
                            h('<div class="card-head"><div><div class="card-title">'
                              'Status da solicitação</div></div></div>'
                              '<div class="muted" style="font-size:13px;padding:8px 0">'
                              'Nenhuma solicitação selecionada.</div>')
                            return
                        h(f'<div class="card-head"><div><div class="card-title">'
                          f'Status da solicitação # {coleta_id}</div></div></div>')

                        detalhe_coleta: dict[str, Any] = {}
                        resumo_partes: list[str] = []
                        try:
                            detalhe_coleta = buscar_coleta_por_id(coleta_id)
                            if detalhe_coleta.get('nomeInstituicao'):
                                resumo_partes.append(f"Instituição: {detalhe_coleta['nomeInstituicao']}")
                            if detalhe_coleta.get('nomeCooperativa'):
                                resumo_partes.append(f"Cooperativa: {detalhe_coleta['nomeCooperativa']}")
                            if detalhe_coleta.get('logradouro') and detalhe_coleta.get('bairro'):
                                resumo_partes.append(
                                    f"Local: {detalhe_coleta['logradouro']}, {detalhe_coleta.get('numero') or ''} - {detalhe_coleta['bairro']}"
                                )
                        except ApiError:
                            detalhe_coleta = {}

                        if resumo_partes:
                            h(f'<div class="muted" style="font-size:12px;margin-bottom:8px">'
                              f'{escape(" • ".join(resumo_partes))}</div>')

                        try:
                            eventos = buscar_timeline_coleta(coleta_id)
                            etapas = _converter_timeline_para_etapas(eventos)
                            if etapas:
                                timeline(etapas)
                            else:
                                h('<div class="muted" style="font-size:13px;padding:8px 0">'
                                  'Sem eventos registrados para esta solicitação.</div>')
                        except ApiError as exc:
                            if exc.status_code != 404:
                                ui.notify(exc.message, type='warning')
                            h('<div class="muted" style="font-size:13px;padding:8px 0">'
                              'Sem eventos registrados para esta solicitação.</div>')

                        status_detalhe = str(detalhe_coleta.get('status') or '').upper()
                        if pode_agendar and status_detalhe and status_detalhe not in ('CONCLUIDA', 'REALIZADA', 'CANCELADA'):
                            def _cancelar_atual(c_id: int = coleta_id) -> None:
                                try:
                                    cancelar_coleta(int(c_id))
                                    ui.notify(f'Coleta #{c_id} cancelada!', type='positive')
                                    if carregar_coletas_ref[0]:
                                        carregar_coletas_ref[0](notificar_erro=True, focar_id=int(c_id))
                                except ApiError as exc:
                                    ui.notify(exc.message, type='negative')

                            with ui.row().classes('justify-end').style('margin-top:12px'):
                                eco_button('Cancelar coleta', _cancelar_atual, cls='outline').style(
                                    'border-color:#B91C1C !important;color:#B91C1C !important;'
                                )

                render_timeline_ref[0] = render_timeline
                render_timeline(None)

        with ui.element('div').classes('narrow'):
            with card('Históricos de coletas'):
                historico_container = ui.element('div')

                def carregar_coletas(notificar_erro: bool = False, focar_id: int | None = None) -> None:
                    historico_container.clear()
                    with historico_container:
                        try:
                            coletas_raw = _buscar_coletas_disponiveis()
                            coletas = _ordenar_coletas_canceladas_por_ultimo(coletas_raw)
                            if not coletas:
                                h('<div class="muted" style="font-size:13px;padding:8px 0">'
                                  'Nenhuma coleta agendada até o momento.</div>')
                                if render_timeline_ref[0]:
                                    render_timeline_ref[0](None)
                                return

                            alvo_id = focar_id or selecionada_id['id'] or coletas[0].get('id')
                            if alvo_id is not None:
                                selecionada_id['id'] = int(alvo_id)

                            if len(coletas) > 1:
                                def acao_linha(idx: int, _row: tuple, cols: list[dict[str, Any]] = coletas) -> None:
                                    item = cols[idx]
                                    if item.get('id') is not None:
                                        cid_int = int(item['id'])
                                        selecionada_id['id'] = cid_int
                                        if render_timeline_ref[0]:
                                            render_timeline_ref[0](cid_int)
                                        ui.run_javascript('window.scrollTo({top: 0, behavior: "smooth"});')
                                        ui.notify(f'Exibindo timeline da solicitação #{cid_int}.', type='info')

                                data_table(
                                    _converter_coletas_para_tabela(coletas),
                                    action='Acompanhar',
                                    on_action=acao_linha,
                                )
                            else:
                                data_table(_converter_coletas_para_tabela(coletas))

                            if render_timeline_ref[0]:
                                render_timeline_ref[0](selecionada_id['id'])
                        except ApiError as exc:
                            if notificar_erro:
                                ui.notify(exc.message, type='warning')
                            h('<div class="muted" style="font-size:13px;padding:8px 0">'
                              'Nenhuma coleta agendada até o momento.</div>')
                            if render_timeline_ref[0]:
                                render_timeline_ref[0](None)

                carregar_coletas_ref[0] = carregar_coletas
                carregar_coletas(notificar_erro=False)

@ui.page('/materiais')
def materiais() -> None:
    if not obter_token():
        ui.navigate.to('/')
        return

    atualizar_sessao_atual()
    perfil_atual = (obter_perfil_atual() or '').upper()
    eh_coop = perfil_atual == 'COOPERATIVA'

    with layout('/materiais'):
        page_header(
            'Controle de materiais recicláveis',
            'Acompanhamento dos resíduos processados nas coletas e consulta ao catálogo ambiental',
        )

        def _carregar_coletas_materiais() -> list[dict[str, Any]]:
            if eh_administrador():
                try:
                    return listar_coletas_operacional()
                except ApiError:
                    pass

            if eh_coop:
                try:
                    minhas_coops = {
                        int(i['id']) for i in listar_minhas_instituicoes()
                        if i.get('id') is not None and str(i.get('tipo') or '').upper() == 'COOPERATIVA'
                    }
                    todas = listar_coletas_operacional()
                    return [
                        c for c in todas
                        if c.get('idCooperativa') is not None and int(c['idCooperativa']) in minhas_coops
                    ]
                except ApiError:
                    pass

            try:
                return listar_minhas_coletas()
            except ApiError:
                return []

        coletas_reais = _carregar_coletas_materiais()

        # KPIs: soma apenas coletas concluídas/realizadas (usando peso real quando informado)
        totais = {'PAPEL': 0.0, 'PLASTICO': 0.0, 'METAL': 0.0, 'OLEO': 0.0}
        for c in coletas_reais:
            st = str(c.get('status') or '').upper()
            if st not in ('CONCLUIDA', 'REALIZADA'):
                continue
            cod = str(c.get('tipoMaterial') or '').upper()
            peso_val = float(c.get('pesoRealKg') or c.get('quantidadeEstimadaKg') or 0)
            if cod in totais:
                totais[cod] += peso_val

        kpis_reais = [
            _atualizar_kpi(m.KPIS_MATERIAIS[0], 'PAPEL E PAPELÃO', f'{_formatar_numero_pt(totais["PAPEL"])} KG'),
            _atualizar_kpi(m.KPIS_MATERIAIS[1], 'PLÁSTICO', f'{_formatar_numero_pt(totais["PLASTICO"])} KG'),
            _atualizar_kpi(m.KPIS_MATERIAIS[2], 'METAL', f'{_formatar_numero_pt(totais["METAL"])} KG'),
            _atualizar_kpi(m.KPIS_MATERIAIS[3], 'ÓLEO E DERIVADOS', f'{_formatar_numero_pt(totais["OLEO"])} L'),
        ]
        kpi_row(kpis_reais)

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
                    f'<div class="muted" style="font-size:12px;margin-top:4px">{escape(desc_mat)}</div>'
                    if desc_mat
                    else ''
                )
                h(
                    f'<div style="margin-top:14px;padding:12px 14px;border-radius:10px;'
                    f'background:#F5F7F8;border:1px solid #E5E7EB">'
                    f'<div style="font-size:13px;font-weight:600;color:{PRIMARY}">'
                    f'{escape(nome_mat)}</div>'
                    f'{desc_html}'
                    f'<div style="font-size:12px;margin-top:6px">'
                    f'{escape(" • ".join(partes_meta))}</div>'
                    f'</div>'
                )

        with card('Consultar material e estimar impacto (CO₂)'):
            with ui.element('div').style('display:grid;grid-template-columns:2fr 1fr auto;gap:12px;align-items:end'):
                with ui.element('div'):
                    h('<label class="form-label upper">Tipo de material</label>')
                    tipo_sel = ui.select(
                        mapa_opcoes,
                        value=codigo_inicial,
                    ).props(
                        'outlined dense hide-bottom-space options-dense aria-label="Tipo de material"'
                    ).classes('eco-field')

                peso_sim = form_field('Quantidade (kg / L)', value='50', upper=True, props='type=number min=0.1 step=0.1')

                def consultar_material() -> None:
                    cod_escolhido = str(tipo_sel.value or codigo_inicial).strip().upper()
                    try:
                        qtd_num = float(str(peso_sim.value or '0').replace(',', '.'))
                    except ValueError:
                        qtd_num = 0.0

                    try:
                        detalhe = buscar_material_por_codigo(cod_escolhido)
                        _render_detalhe_material(detalhe, qtd_informada=max(0.0, qtd_num))
                    except ApiError as exc:
                        ui.notify(exc.message, type='warning')

                with ui.element('div').classes('cell-end'):
                    eco_button('CONSULTAR IMPACTO', consultar_material, cls='eco-block')

            detalhe_box_ref[0] = ui.element('div').classes('w-full')

        with card('Registros de materiais nas coletas'):
            with ui.element('div').style('display:grid;grid-template-columns:1fr 1fr auto;gap:12px;align-items:end;margin-bottom:14px'):
                with ui.element('div'):
                    h('<label class="form-label">Filtrar por material</label>')
                    filtro_mat = ui.select(
                        {'': 'Todos', **_OPCOES_TIPO_MATERIAL_COLETA},
                        value='',
                    ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')

                with ui.element('div'):
                    h('<label class="form-label">Filtrar por status</label>')
                    filtro_st = ui.select(
                        {'': 'Todos', **_STATUS_COLETA_OPCOES},
                        value='',
                    ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')

                with ui.element('div').classes('cell-end'):
                    with ui.row().classes('gap-2 no-wrap'):
                        eco_button('Filtrar', lambda: render_tabela_materiais.refresh(), cls='dark')

                        def _limpar() -> None:
                            filtro_mat.value = ''
                            filtro_st.value = ''
                            render_tabela_materiais.refresh()

                        eco_button('Limpar', _limpar, cls='outline')

            @ui.refreshable
            def render_tabela_materiais() -> None:
                mat_f = str(filtro_mat.value or '').upper()
                st_f = str(filtro_st.value or '').upper()

                filtradas = [
                    c for c in coletas_reais
                    if (not mat_f or str(c.get('tipoMaterial') or '').upper() == mat_f)
                    and (not st_f or str(c.get('status') or '').upper() == st_f)
                ]

                if not filtradas:
                    h('<div class="muted" style="font-size:13px;padding:10px 0">'
                      'Nenhum registro de material encontrado para os filtros informados.</div>')
                    return

                tone_map = {
                    'CONCLUIDA': 'green',
                    'REALIZADA': 'green',
                    'EM_ROTA': 'blue',
                    'EM_ANDAMENTO': 'blue',
                    'AGENDADA': 'amber',
                    'ENVIADA': 'amber',
                    'CANCELADA': 'gray',
                }

                rows: list[tuple] = []
                for c in filtradas:
                    cod = str(c.get('tipoMaterial') or 'PAPEL').upper()
                    mat_lbl = _ENUM_PARA_CHIP.get(cod, cod.capitalize())
                    unid = 'L' if cod == 'OLEO' else 'kg'

                    peso_real = c.get('pesoRealKg')
                    peso_est = c.get('quantidadeEstimadaKg')
                    if peso_real is not None:
                        peso_fmt = f'{_formatar_numero_pt(float(peso_real))} {unid} (real)'
                    elif peso_est is not None:
                        peso_fmt = f'{_formatar_numero_pt(float(peso_est))} {unid} (est.)'
                    else:
                        peso_fmt = '-'

                    data_ref = str(
                        c.get('dataConclusao')
                        or c.get('dataHoraAgendamento')
                        or c.get('dataCriacao')
                        or ''
                    )
                    data_fmt = _formatar_data_curta(data_ref)
                    origem_lbl = str(
                        c.get('nomeInstituicao')
                        or c.get('nomeSolicitante')
                        or c.get('bairro')
                        or '-'
                    )
                    destino_lbl = str(c.get('nomeCooperativa') or 'Não definida')
                    st_raw = str(c.get('status') or 'ENVIADA').upper()
                    st_lbl = _STATUS_COLETA_OPCOES.get(st_raw, st_raw.replace('_', ' ').capitalize())

                    rows.append((
                        f'#{c.get("id") or "-"} • {mat_lbl}',
                        peso_fmt,
                        data_fmt,
                        origem_lbl,
                        destino_lbl,
                        m.Badge(st_lbl, tone_map.get(st_raw, 'gray')),
                    ))

                data_table(
                    m.Table(
                        columns=('Material', 'Peso / Qtd', 'Data', 'Origem', 'Destino', 'Status'),
                        rows=rows,
                    )
                )

            render_tabela_materiais()


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
                with card('Coletas finalizadas pelo motorista (Aguardando confirmação)'):
                    @ui.refreshable
                    def render_confirmacoes_rotas() -> None:
                        if not _CONFIRMACOES_ROTAS_ADMIN:
                            h('<div class="muted" style="font-size:13px;padding:8px 0">'
                              'Nenhuma coleta finalizada pelo motorista aguardando validação no momento.</div>')
                            return

                        with ui.element('div').classes('list-divided'):
                            for reg in _CONFIRMACOES_ROTAS_ADMIN:
                                st = reg['status']
                                tone = 'green' if st == 'CONFIRMADA' else ('gray' if st == 'NEGADA' else 'amber')
                                st_lbl = (
                                    'Confirmada' if st == 'CONFIRMADA'
                                    else ('Negada' if st == 'NEGADA' else 'Aguardando')
                                )
                                meta = (
                                    f'{reg["material"]} • {reg["volume"]} • '
                                    f'Assinado por {reg["responsavel"]} às {reg["horario"]}'
                                )
                                if reg.get('observacao'):
                                    meta += f' • {reg["observacao"]}'

                                with ui.element('div').classes('item'):
                                    with ui.element('div').style('flex:1;min-width:0'):
                                        h(
                                            f'<div class="item-title" style="font-size:15px;font-weight:600">'
                                            f'Motorista finalizou coleta em {escape(reg["ponto"])}</div>'
                                            f'<div class="item-meta" style="font-size:13px;margin-top:4px">'
                                            f'{escape(meta)}</div>'
                                        )
                                    h(f'<span class="badge b-{tone}" style="margin-right:8px;font-size:12px">{st_lbl}</span>')
                                    if st == 'PENDENTE':
                                        def _decidir(item: dict[str, Any], novo: str) -> None:
                                            item['status'] = novo
                                            ui.notify(
                                                f'Coleta em {item["ponto"]} marcada como {novo.lower()}.',
                                                type='positive' if novo == 'CONFIRMADA' else 'warning',
                                            )
                                            render_confirmacoes_rotas.refresh()

                                        with ui.row().classes('gap-1 items-center'):
                                            ui.button(
                                                'Confirmar',
                                                on_click=lambda _e, r=reg: _decidir(r, 'CONFIRMADA'),
                                            ).props('flat dense no-caps').classes('btn-ghost').style(
                                                f'border-color:{PRIMARY} !important;color:{PRIMARY} !important;'
                                            )
                                            ui.button(
                                                'Negar',
                                                on_click=lambda _e, r=reg: _decidir(r, 'NEGADA'),
                                            ).props('flat dense no-caps').classes('btn-ghost').style(
                                                'border-color:#B91C1C !important;color:#B91C1C !important;'
                                            )

                    render_confirmacoes_rotas()

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


@ui.page('/rotas')
def planejamento_rotas() -> None:
    if not obter_token():
        ui.navigate.to('/')
        return

    atualizar_sessao_atual()
    perfil = (obter_perfil_atual() or '').upper()
    eh_admin = perfil == 'ADMINISTRADOR'
    eh_motorista = perfil == 'MOTORISTA'
    eh_cooperativa = perfil == 'COOPERATIVA'

    with layout('/rotas'):
        page_header(
            'Rotas e entregas',
            'Acompanhe sua fila real de coletas, escale motoristas e registre assinaturas de entrega',
        )

        dialog_assinatura = ui.dialog()
        estado: dict[str, Any] = {'coleta_selecionada_id': None}

        # --- Bloco exclusivo de COOPERATIVA / ADMIN: escalar motoristas ---
        @ui.refreshable
        def secao_escala_motoristas() -> None:
            if not (eh_cooperativa or eh_admin):
                return

            with card('Escalar motorista para coletas'):
                try:
                    if eh_admin:
                        coops = listar_cooperativas(status='ATIVO')
                    else:
                        coops = [
                            i for i in listar_minhas_instituicoes()
                            if str(i.get('tipo') or '').upper() == 'COOPERATIVA'
                            and str(i.get('status') or '').upper() == 'ATIVO'
                        ]
                except ApiError as exc:
                    h(f'<div class="muted" style="font-size:13px">{escape(exc.message)}</div>')
                    return

                if not coops:
                    h('<div class="muted" style="font-size:13px;padding:6px 0">'
                      'Nenhuma cooperativa ativa disponível para escalar motoristas.</div>')
                    return

                motoristas_por_coop: dict[int, dict[int, str]] = {}
                for coop in coops:
                    cid = int(coop['id'])
                    try:
                        membros = listar_membros_instituicao(cid)
                        motoristas_por_coop[cid] = {
                            int(mem['usuarioId']): str(mem.get('nomeUsuario') or f"Motorista #{mem['usuarioId']}")
                            for mem in membros
                            if mem.get('ativo', True)
                            and str(mem.get('papel') or '').upper() == 'MOTORISTA'
                            and mem.get('usuarioId') is not None
                        }
                    except ApiError:
                        motoristas_por_coop[cid] = {}

                try:
                    todas_coletas = listar_coletas_operacional()
                except ApiError as exc:
                    h(f'<div class="muted" style="font-size:13px">{escape(exc.message)}</div>')
                    return

                coop_ids = {int(c['id']) for c in coops}
                coletas_escalaveis = [
                    c for c in todas_coletas
                    if str(c.get('status') or '').upper() in ('ENVIADA', 'AGENDADA', 'EM_ROTA')
                    and c.get('idCooperativa') is not None
                    and int(c['idCooperativa']) in coop_ids
                ]

                if not coletas_escalaveis:
                    h('<div class="muted" style="font-size:13px;padding:6px 0">'
                      'Nenhuma coleta vinculada à cooperativa aguardando escala de motorista no momento.</div>')
                    return

                with ui.element('div').classes('list-divided'):
                    for col in coletas_escalaveis:
                        col_id = int(col['id'])
                        coop_id = int(col['idCooperativa'])
                        st_raw = str(col.get('status') or '').upper()
                        st_lbl = _STATUS_COLETA_OPCOES.get(st_raw, st_raw)
                        mat_raw = str(col.get('tipoMaterial') or '').upper()
                        mat_lbl = _ENUM_PARA_CHIP.get(mat_raw, mat_raw.capitalize())
                        qtd_str = f'{col.get("quantidadeEstimadaKg") or 0} kg'
                        origem_str = str(col.get('nomeInstituicao') or col.get('nomeSolicitante') or 'Coleta avulsa')
                        local_str = f'{col.get("logradouro") or ""}, {col.get("numero") or ""} - {col.get("bairro") or ""}'
                        opcoes_mot = motoristas_por_coop.get(coop_id, {})

                        with ui.element('div').classes('item').style('flex-wrap:wrap;gap:12px'):
                            with ui.element('div').style('flex:1;min-width:220px'):
                                h(
                                    f'<div class="item-title" style="font-size:14px">'
                                    f'Coleta #{col_id} • {escape(origem_str)}</div>'
                                    f'<div class="item-meta" style="font-size:12px">'
                                    f'{escape(mat_lbl)} ({escape(qtd_str)}) • {escape(local_str)} • '
                                    f'Cooperativa: {escape(str(col.get("nomeCooperativa") or ""))}</div>'
                                )

                            h(f'<span class="badge b-blue">{escape(st_lbl)}</span>')

                            if not opcoes_mot:
                                h('<span class="muted" style="font-size:12px">'
                                  'Cadastre um motorista em Minha instituição</span>')
                            else:
                                sel_mot = ui.select(
                                    opcoes_mot,
                                    value=next(iter(opcoes_mot.keys())),
                                ).props('outlined dense hide-bottom-space options-dense').classes('eco-field').style('width:200px')

                                def _escalar(c_id: int, select_ref: ui.select, status_atual: str) -> None:
                                    if select_ref.value is None:
                                        ui.notify('Selecione um motorista.', type='warning')
                                        return
                                    try:
                                        if status_atual == 'ENVIADA':
                                            atualizar_status_coleta(c_id, 'AGENDADA', 'Agendada pela cooperativa')
                                        atribuir_motorista_coleta(c_id, int(select_ref.value))
                                        ui.notify(f'Motorista escalado para a coleta #{c_id}!', type='positive')
                                        secao_escala_motoristas.refresh()
                                        conteudo_rotas.refresh()
                                    except ApiError as exc:
                                        ui.notify(exc.message, type='negative')

                                eco_button(
                                    'Atribuir motorista',
                                    lambda _e=None, cid_a=col_id, sm=sel_mot, sa=st_raw: _escalar(cid_a, sm, sa),
                                    cls='dark',
                                )

        def abrir_modal_assinatura(parada: dict[str, Any]) -> None:
            coleta_id = int(parada['coletaId'])
            dialog_assinatura.clear()
            canvas_id = f'sig-canvas-{coleta_id}-{int(datetime.now().timestamp())}'

            qtd_est = parada.get('quantidadeEstimadaKg')
            peso_inicial = str(qtd_est) if qtd_est is not None else ''
            resp_inicial = str(parada.get('nomeResponsavelLocal') or '')

            with dialog_assinatura, ui.card().style('width:500px;max-width:95vw;border-radius:16px;padding:20px'):
                h(
                    f'<div class="card-title" style="margin-bottom:4px">'
                    f'Concluir entrega • Coleta #{coleta_id}</div>'
                    f'<div class="muted" style="font-size:12px;margin-bottom:14px">'
                    f'<strong>{escape(str(parada.get("localOrigemNome") or "-"))}</strong> • '
                    f'{escape(str(parada.get("nomeMaterial") or "-"))} '
                    f'({escape(str(qtd_est or 0))} kg estimados)</div>'
                )

                with ui.element('div').classes('form-grid'):
                    campo_resp = form_field(
                        'Nome do responsável no local',
                        value=resp_inicial,
                        placeholder='Ex: Maria Silva',
                    )
                    with ui.element('div').classes('form-grid cols-2'):
                        campo_doc = form_field(
                            'CPF do recebedor',
                            placeholder='Somente números (11 dígitos)',
                            props='mask="###########" inputmode="numeric"',
                        )
                        campo_peso = form_field(
                            'Peso real coletado (kg)',
                            value=peso_inicial,
                            props='type=number min=0.1 step=0.1',
                        )

                    campo_obs = form_field(
                        'Observação (opcional)',
                        placeholder='Ex: Volume conferido na retirada',
                    )

                    with ui.element('div'):
                        h(
                            f'<label class="form-label">Assinatura digital do responsável</label>'
                            f'<div style="border:1px dashed #94A3B8;border-radius:10px;background:#F8FAFC;overflow:hidden">'
                            f'  <canvas id="{canvas_id}" width="450" height="135" '
                            f'    style="width:100%;height:135px;display:block;cursor:crosshair;touch-action:none"></canvas>'
                            f'</div>'
                            f'<div style="display:flex;justify-content:space-between;align-items:center;margin-top:4px">'
                            f'  <span class="muted" style="font-size:11px">Assine com o mouse ou dedo no quadro acima</span>'
                            f'  <button type="button" onclick="const c=document.getElementById(\'{canvas_id}\');'
                            f'    if(c){{c.getContext(\'2d\').clearRect(0,0,c.width,c.height);}}'
                            f'    window._ecoSigInk=window._ecoSigInk||{{}};window._ecoSigInk[\'{canvas_id}\']=false;" '
                            f'    style="font-size:12px;color:{PRIMARY};background:none;border:none;cursor:pointer">Limpar</button>'
                            f'</div>'
                        )

                    ui.run_javascript(f'''
                        (() => {{
                            window._ecoSigInk = window._ecoSigInk || {{}};
                            window._ecoSigInk["{canvas_id}"] = false;
                            const c = document.getElementById("{canvas_id}");
                            if (!c) return;
                            const ctx = c.getContext("2d");
                            ctx.lineWidth = 2;
                            ctx.lineCap = "round";
                            ctx.strokeStyle = "#0F172A";
                            let drawing = false;
                            const pos = (e) => {{
                                const r = c.getBoundingClientRect();
                                const t = e.touches ? e.touches[0] : e;
                                return {{
                                    x: (t.clientX - r.left) * (c.width / r.width),
                                    y: (t.clientY - r.top) * (c.height / r.height),
                                }};
                            }};
                            const start = (e) => {{
                                drawing = true;
                                window._ecoSigInk["{canvas_id}"] = true;
                                const p = pos(e);
                                ctx.beginPath();
                                ctx.moveTo(p.x, p.y);
                            }};
                            const move = (e) => {{
                                if (!drawing) return;
                                e.preventDefault();
                                window._ecoSigInk["{canvas_id}"] = true;
                                const p = pos(e);
                                ctx.lineTo(p.x, p.y);
                                ctx.stroke();
                            }};
                            const stop = () => {{ drawing = false; }};
                            c.addEventListener("mousedown", start);
                            c.addEventListener("mousemove", move);
                            window.addEventListener("mouseup", stop);
                            c.addEventListener("touchstart", start, {{ passive: false }});
                            c.addEventListener("touchmove", move, {{ passive: false }});
                            c.addEventListener("touchend", stop);
                        }})();
                    ''')

                    async def confirmar_assinatura() -> None:
                        responsavel = (campo_resp.value or '').strip()
                        if not responsavel:
                            ui.notify('Informe o nome do responsável pela assinatura.', type='warning')
                            return

                        doc_limpo = re.sub(r'\D', '', campo_doc.value or '')
                        if len(doc_limpo) != 11:
                            ui.notify('Informe o CPF do recebedor com 11 dígitos (somente números).', type='warning')
                            return

                        peso_val: float | None = None
                        peso_txt = str(campo_peso.value or '').strip().replace(',', '.')
                        if peso_txt:
                            try:
                                peso_val = float(peso_txt)
                            except ValueError:
                                ui.notify('Peso real inválido.', type='warning')
                                return
                            if peso_val <= 0:
                                ui.notify('O peso real coletado deve ser maior que zero.', type='warning')
                                return

                        sig_result = await ui.run_javascript(f'''
                            (() => {{
                                const c = document.getElementById("{canvas_id}");
                                const hasInk = Boolean(window._ecoSigInk && window._ecoSigInk["{canvas_id}"]);
                                return {{
                                    hasInk,
                                    dataUrl: c ? c.toDataURL("image/png") : "",
                                }};
                            }})()
                        ''')

                        if not isinstance(sig_result, dict) or not sig_result.get('hasInk') or not sig_result.get('dataUrl'):
                            ui.notify('Peça ao responsável para assinar no quadro antes de confirmar.', type='warning')
                            return

                        payload: dict[str, Any] = {
                            'nomeRecebedor': responsavel,
                            'documentoRecebedor': doc_limpo,
                            'assinaturaDigital': str(sig_result['dataUrl']),
                        }
                        if peso_val is not None:
                            payload['pesoRealKg'] = peso_val
                        obs_txt = (campo_obs.value or '').strip()
                        if obs_txt:
                            payload['observacoes'] = obs_txt

                        try:
                            concluir_entrega_motorista(coleta_id, payload)
                            ui.notify(
                                f'Coleta #{coleta_id} concluída com assinatura registrada!',
                                type='positive',
                            )
                            estado['coleta_selecionada_id'] = None
                            dialog_assinatura.close()
                            secao_escala_motoristas.refresh()
                            conteudo_rotas.refresh()
                        except ApiError as exc:
                            ui.notify(exc.message, type='negative')

                    with ui.row().classes('justify-end gap-2').style('margin-top:10px'):
                        eco_button('Cancelar', dialog_assinatura.close, cls='outline')
                        eco_button(
                            'Assinar e concluir coleta',
                            confirmar_assinatura,
                            cls='dark',
                            props='unelevated no-caps text-white',
                        ).style('background-color: #17693A !important; color: #FFFFFF !important;')

            dialog_assinatura.open()

        @ui.refreshable
        def conteudo_rotas() -> None:
            if not (eh_motorista or eh_admin):
                if not eh_cooperativa:
                    with card('Acesso à rota de coletas'):
                        h(
                            '<div class="muted" style="font-size:13px;padding:8px 0">'
                            'A execução de rotas está disponível para usuários com perfil '
                            '<strong>Motorista</strong> (vinculado a uma cooperativa).'
                            '</div>'
                        )
                        eco_button('Ir para Minha instituição', lambda: ui.navigate.to('/minha-instituicao'), cls='outline')
                return

            try:
                paradas = listar_minha_rota_motorista()
            except ApiError as exc:
                with card('Minha rota'):
                    h(f'<div class="muted" style="font-size:13px;padding:8px 0">{escape(exc.message)}</div>')
                return

            total = len(paradas)
            carga_total = sum(float(p.get('quantidadeEstimadaKg') or 0) for p in paradas)
            em_andamento = sum(1 for p in paradas if str(p.get('status') or '').upper() == 'EM_ANDAMENTO')
            em_rota = sum(1 for p in paradas if str(p.get('status') or '').upper() == 'EM_ROTA')

            parada_atual: dict[str, Any] | None = None
            if paradas:
                alvo_id = estado['coleta_selecionada_id']
                if alvo_id is not None:
                    parada_atual = next((p for p in paradas if int(p['coletaId']) == int(alvo_id)), None)
                if parada_atual is None:
                    parada_atual = next(
                        (p for p in paradas if str(p.get('status') or '').upper() == 'EM_ANDAMENTO'),
                        paradas[0],
                    )
                    estado['coleta_selecionada_id'] = int(parada_atual['coletaId'])

            with grid('65'):
                with card('Parada em atendimento'):
                    if parada_atual is None:
                        h(
                            '<div style="padding:20px;border-radius:12px;background:#F5F7F8;'
                            'border:1px dashed #CBD5E1;margin-top:6px">'
                            '  <div style="font-size:15px;font-weight:600">Nenhuma coleta pendente na sua rota</div>'
                            '  <div class="muted" style="font-size:13px;margin-top:4px">'
                            '    Quando a cooperativa atribuir uma coleta ao seu perfil, ela aparecerá aqui automaticamente.'
                            '  </div>'
                            '</div>'
                        )
                    else:
                        cid_atual = int(parada_atual['coletaId'])
                        st_atual = str(parada_atual.get('status') or 'EM_ROTA').upper()
                        st_lbl = _STATUS_COLETA_OPCOES.get(st_atual, st_atual)
                        data_fmt = _formatar_data_curta(str(parada_atual.get('dataHoraAgendamento') or ''))
                        obs_html = (
                            f'<div class="muted" style="font-size:12px;margin-top:8px">'
                            f'<strong>Observações:</strong> {escape(str(parada_atual["observacoes"]))}</div>'
                            if parada_atual.get('observacoes')
                            else ''
                        )

                        h(
                            f'<div style="padding:16px;border-radius:12px;background:#F5F7F8;'
                            f'border:1px solid #E5E7EB;margin-top:6px">'
                            f'  <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:12px;gap:10px">'
                            f'    <div>'
                            f'      <div class="muted" style="font-size:11px;text-transform:uppercase;letter-spacing:.4px">'
                            f'        Coleta #{cid_atual} • {escape(data_fmt)} ({escape(str(parada_atual.get("janelaHorario") or "-"))})'
                            f'      </div>'
                            f'      <div style="font-size:18px;font-weight:600;color:{PRIMARY};margin-top:2px">'
                            f'        {escape(str(parada_atual.get("localOrigemNome") or "-"))}'
                            f'      </div>'
                            f'      <div class="muted" style="font-size:12px;margin-top:4px">'
                            f'        {escape(str(parada_atual.get("enderecoCompleto") or "-"))}'
                            f'      </div>'
                            f'    </div>'
                            f'    <span class="badge b-blue">{escape(st_lbl)}</span>'
                            f'  </div>'
                            f'  <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px">'
                            f'    <div>'
                            f'      <div class="muted" style="font-size:11px">Material</div>'
                            f'      <div style="font-size:14px;font-weight:500">{escape(str(parada_atual.get("nomeMaterial") or "-"))}</div>'
                            f'    </div>'
                            f'    <div>'
                            f'      <div class="muted" style="font-size:11px">Volume estimado</div>'
                            f'      <div style="font-size:14px;font-weight:500">{escape(str(parada_atual.get("quantidadeEstimadaKg") or 0))} kg</div>'
                            f'    </div>'
                            f'    <div>'
                            f'      <div class="muted" style="font-size:11px">Responsável no local</div>'
                            f'      <div style="font-size:14px;font-weight:500">{escape(str(parada_atual.get("nomeResponsavelLocal") or "-"))}</div>'
                            f'      <div class="muted" style="font-size:11px">Tel: {escape(str(parada_atual.get("telefoneResponsavelLocal") or "Não informado"))}</div>'
                            f'    </div>'
                            f'  </div>'
                            f'  {obs_html}'
                            f'</div>'
                        )

                        def iniciar_atendimento(c_id: int) -> None:
                            try:
                                atualizar_status_coleta(c_id, 'EM_ROTA', 'Motorista saiu para a retirada no local')
                                ui.notify(f'Coleta #{c_id} marcada como Em rota.', type='positive')
                                conteudo_rotas.refresh()
                            except ApiError as exc:
                                ui.notify(exc.message, type='negative')

                        with ui.row().classes('justify-end gap-2').style('margin-top:12px'):
                            if st_atual == 'AGENDADA':
                                eco_button(
                                    'Iniciar deslocamento',
                                    lambda _e=None, c=cid_atual: iniciar_atendimento(c),
                                    cls='outline',
                                )
                            eco_button(
                                'Finalizar e assinar',
                                lambda _e=None, p=parada_atual: abrir_modal_assinatura(p),
                                cls='dark',
                            )

                with card('Resumo da minha rota'):
                    h(
                        f'<div>'
                        f'  <div class="summary-item">'
                        f'    <div class="summary-label">Paradas pendentes</div>'
                        f'    <div class="summary-val">{total}</div>'
                        f'  </div>'
                        f'  <div class="summary-item">'
                        f'    <div class="summary-label">Carga prevista total</div>'
                        f'    <div class="summary-val">{_formatar_numero_pt(carga_total)} kg</div>'
                        f'  </div>'
                        f'  <div class="summary-item">'
                        f'    <div class="summary-label">Em rota</div>'
                        f'    <div class="summary-val">{em_rota}</div>'
                        f'  </div>'
                        f'  <div class="summary-item">'
                        f'    <div class="summary-label">Em andamento</div>'
                        f'    <div class="summary-val green">{em_andamento}</div>'
                        f'  </div>'
                        f'</div>'
                    )
                    with ui.element('div').style('margin-top:14px'):
                        eco_button('Atualizar rota', conteudo_rotas.refresh, cls='outline eco-block')

            with card('Sequência de paradas da minha rota'):
                outras_paradas = [
                    p for p in paradas
                    if parada_atual is None or int(p['coletaId']) != int(parada_atual['coletaId'])
                ]
                if not outras_paradas:
                    h('<div class="muted" style="font-size:13px;padding:8px 0">'
                      'Nenhuma outra parada aguardando na fila.</div>')
                else:
                    rows_rota: list[tuple] = []
                    for p in outras_paradas:
                        st_p = str(p.get('status') or 'EM_ROTA').upper()
                        st_p_lbl = _STATUS_COLETA_OPCOES.get(st_p, st_p)
                        rows_rota.append((
                            f"#{p['coletaId']}",
                            str(p.get('localOrigemNome') or '-'),
                            str(p.get('bairro') or '-'),
                            str(p.get('nomeMaterial') or '-'),
                            f"{p.get('quantidadeEstimadaKg') or 0} kg",
                            m.Badge(st_p_lbl),
                            str(p.get('janelaHorario') or '-'),
                        ))

                    def _selecionar_parada(idx: int, _row: tuple, lista: list[dict[str, Any]] = outras_paradas) -> None:
                        estado['coleta_selecionada_id'] = int(lista[idx]['coletaId'])
                        ui.run_javascript('window.scrollTo({top: 0, behavior: "smooth"});')
                        conteudo_rotas.refresh()

                    data_table(
                        m.Table(
                            columns=('Coleta', 'Origem', 'Bairro', 'Material', 'Volume', 'Status', 'Janela'),
                            rows=rows_rota,
                        ),
                        action='Atender',
                        on_action=_selecionar_parada,
                    )

        secao_escala_motoristas()
        conteudo_rotas()

@ui.page('/minha-instituicao')
def minha_instituicao() -> None:
    if not obter_token():
        ui.navigate.to('/')
        return

    # Atualiza perfil/nome na sessão caso o admin tenha aprovado uma instituição
    atualizar_sessao_atual()

    with layout('/minha-instituicao'):
        page_header(
            'Minha instituição',
            'Solicite o cadastro de cooperativa, empresa ou condomínio e gerencie membros vinculados',
        )

        dialog_membros = ui.dialog()

        def _abrir_modal_membros(inst: dict[str, Any]) -> None:
            dialog_membros.clear()
            inst_id = int(inst['id'])
            nome_inst = str(inst.get('nome') or f'Instituição #{inst_id}')
            tipo_inst = str(inst.get('tipo') or '').upper()
            eh_coop = tipo_inst == 'COOPERATIVA'

            papeis_opcoes = (
                {'MOTORISTA': 'Motorista', 'MORADOR': 'Morador'}
                if eh_coop
                else {'MORADOR': 'Morador'}
            )
            papel_padrao = 'MOTORISTA' if eh_coop else 'MORADOR'

            with dialog_membros, ui.card().style('width:560px;max-width:95vw;border-radius:16px;padding:20px'):
                h(
                    f'<div class="card-title" style="margin-bottom:4px">Membros • {escape(nome_inst)}</div>'
                    f'<div class="muted" style="font-size:13px;margin-bottom:14px">'
                    f'Informe CPF e e-mail de um usuário já cadastrado e ativo na plataforma.</div>'
                )

                with ui.element('div').classes('form-grid'):
                    with ui.element('div').classes('form-grid cols-2'):
                        campo_cpf = form_field(
                            'CPF do usuário',
                            placeholder='Somente números (11 dígitos)',
                            props='mask="###########" inputmode="numeric"',
                        )
                        campo_email = form_field('E-mail do usuário', placeholder='usuario@email.com')

                    with ui.element('div'):
                        h('<label class="form-label">Papel na instituição</label>')
                        sel_papel = ui.select(
                            papeis_opcoes,
                            value=papel_padrao,
                        ).props('outlined dense hide-bottom-space options-dense').classes('eco-field')

                    lista_membros_box = ui.element('div').classes('w-full')

                    def carregar_membros() -> None:
                        lista_membros_box.clear()
                        with lista_membros_box:
                            try:
                                membros = listar_membros_instituicao(inst_id)
                                ativos = [m_item for m_item in membros if m_item.get('ativo', True)]
                                if not ativos:
                                    h('<div class="muted" style="font-size:13px;padding:8px 0">'
                                      'Nenhum membro ativo vinculado até o momento.</div>')
                                    return

                                with ui.element('div').classes('list-divided'):
                                    for mem in ativos:
                                        mid = int(mem['id'])
                                        nome_m = str(mem.get('nomeUsuario') or f'Usuário #{mem.get("usuarioId")}')
                                        email_m = str(mem.get('emailUsuario') or '-')
                                        cpf_m = str(mem.get('cpfUsuario') or '-')
                                        papel_m = str(mem.get('papel') or 'MORADOR').upper()
                                        tone_p = 'blue' if papel_m == 'MOTORISTA' else 'green'

                                        def _desvincular(membro_id: int, nome_alvo: str) -> None:
                                            try:
                                                desvincular_membro_instituicao(inst_id, membro_id)
                                                ui.notify(f'{nome_alvo} desvinculado(a).', type='warning')
                                                carregar_membros()
                                            except ApiError as exc:
                                                ui.notify(exc.message, type='negative')

                                        with ui.element('div').classes('item'):
                                            with ui.element('div').style('flex:1;min-width:0'):
                                                h(
                                                    f'<div class="item-title" style="font-size:14px">{escape(nome_m)}</div>'
                                                    f'<div class="item-meta" style="font-size:12px">'
                                                    f'{escape(email_m)} • CPF: {escape(cpf_m)}</div>'
                                                )
                                            h(f'<span class="badge b-{tone_p}" style="margin-right:8px">'
                                              f'{escape(papel_m.capitalize())}</span>')
                                            ui.button(
                                                'Remover',
                                                on_click=lambda _e, m_id=mid, nm=nome_m: _desvincular(m_id, nm),
                                            ).props('flat dense no-caps').classes('btn-ghost').style(
                                                'border-color:#B91C1C !important;color:#B91C1C !important;'
                                            )
                            except ApiError as exc:
                                h(f'<div class="muted" style="font-size:13px;padding:8px 0">'
                                  f'{escape(exc.message)}</div>')

                    def adicionar_membro() -> None:
                        cpf_limpo = re.sub(r'\D', '', campo_cpf.value or '')
                        email_val = (campo_email.value or '').strip().lower()
                        if len(cpf_limpo) != 11:
                            ui.notify('Informe um CPF válido com 11 dígitos (somente números).', type='warning')
                            return
                        if '@' not in email_val:
                            ui.notify('Informe um e-mail válido.', type='warning')
                            return
                        try:
                            vincular_membro_instituicao(
                                inst_id,
                                cpf_limpo,
                                email_val,
                                str(sel_papel.value or papel_padrao),
                            )
                            ui.notify('Membro vinculado com sucesso!', type='positive')
                            campo_cpf.value = ''
                            campo_email.value = ''
                            carregar_membros()
                        except ApiError as exc:
                            ui.notify(exc.message, type='negative')

                    with ui.row().classes('justify-end gap-2'):
                        eco_button(
                            'Vincular membro',
                            adicionar_membro,
                            cls='dark',
                            props='unelevated no-caps text-white',
                        ).style('background-color: #17693A !important; color: #FFFFFF !important;')

                    h('<div style="border-top:1px solid #EFEFEF;margin:6px 0"></div>'
                      '<div style="font-size:13px;font-weight:600">Membros ativos</div>')
                    carregar_membros()

                with ui.row().classes('justify-end').style('margin-top:12px'):
                    eco_button('Fechar', dialog_membros.close, cls='outline')

            dialog_membros.open()

        @ui.refreshable
        def conteudo_instituicao() -> None:
            minhas: list[dict[str, Any]] = []
            vinculos: list[dict[str, Any]] = []
            erro_carregamento = ''

            try:
                minhas = listar_minhas_instituicoes()
            except ApiError as exc:
                erro_carregamento = exc.message

            try:
                vinculos = [v for v in listar_meus_vinculos_instituicao() if v.get('ativo', True)]
            except ApiError:
                vinculos = []

            with grid('65'):
                with ui.element('div').classes('stack'):
                    with card('Minhas instituições cadastradas'):
                        if erro_carregamento:
                            h(f'<div class="muted" style="font-size:13px;padding:8px 0">'
                              f'{escape(erro_carregamento)}</div>')
                        elif not minhas:
                            h('<div class="muted" style="font-size:14px;padding:8px 0">'
                              'Você ainda não possui nenhuma instituição cadastrada. '
                              'Use o formulário ao lado para solicitar o cadastro de uma '
                              '<strong>Cooperativa</strong>, <strong>Empresa</strong> ou <strong>Condomínio</strong>.</div>')
                        else:
                            with ui.element('div').classes('stack'):
                                for inst in minhas:
                                    nome_i = str(inst.get('nome') or f"Instituição #{inst.get('id')}")
                                    tipo_raw = str(inst.get('tipo') or '').upper()
                                    tipo_lbl = _TIPOS_INSTITUICAO_OPCOES.get(tipo_raw, tipo_raw.capitalize())
                                    doc_i = str(inst.get('documento') or '-')
                                    tel_i = str(inst.get('telefone') or '-')
                                    st_raw = str(inst.get('status') or 'AGUARDANDO_ANALISE').upper()
                                    st_lbl = _STATUS_CADASTRO_OPCOES.get(st_raw, st_raw.capitalize())
                                    tone = (
                                        'green' if st_raw == 'ATIVO'
                                        else ('red' if st_raw == 'REJEITADO' else 'amber')
                                    )
                                    end_i = (
                                        f'{inst.get("logradouro") or ""}, {inst.get("numero") or ""} - '
                                        f'{inst.get("bairro") or ""}, {inst.get("cidade") or ""}/{inst.get("estado") or ""} '
                                        f'(CEP: {inst.get("cep") or ""})'
                                    )

                                    with ui.element('div').style(
                                        'padding:16px;border-radius:12px;background:#F8FAFC;'
                                        'border:1px solid #E5E7EB'
                                    ):
                                        h(
                                            f'<div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px">'
                                            f'  <div>'
                                            f'    <div style="font-size:16px;font-weight:600;color:{PRIMARY}">{escape(nome_i)}</div>'
                                            f'    <div class="muted" style="font-size:13px;margin-top:2px">'
                                            f'      {escape(tipo_lbl)} • CNPJ: {escape(doc_i)} • Tel: {escape(tel_i)}'
                                            f'    </div>'
                                            f'    <div class="muted" style="font-size:12px;margin-top:4px">{escape(end_i)}</div>'
                                            f'  </div>'
                                            f'  <span class="badge b-{tone}" style="font-size:12px">{escape(st_lbl)}</span>'
                                            f'</div>'
                                        )

                                        if st_raw == 'AGUARDANDO_ANALISE':
                                            h(
                                                '<div style="margin-top:10px;padding:10px 12px;border-radius:8px;'
                                                'background:#FEF3C7;color:#92400E;font-size:13px">'
                                                'Solicitação em análise pelo administrador da plataforma. '
                                                'Assim que for aprovada, seu perfil e as funções de gestão serão liberados automaticamente.'
                                                '</div>'
                                            )
                                        elif st_raw == 'REJEITADO':
                                            motivo = str(inst.get('motivoRejeicao') or 'Não informado.')
                                            h(
                                                f'<div style="margin-top:10px;padding:10px 12px;border-radius:8px;'
                                                f'background:#FEE2E2;color:#991B1B;font-size:13px">'
                                                f'<strong>Motivo da rejeição:</strong> {escape(motivo)}<br>'
                                                f'<span style="font-size:12px">O CNPJ permanece vinculado a este registro. '
                                                f'Para reabrir a solicitação, peça ao administrador para alterar o status novamente para "Aguardando análise".</span>'
                                                f'</div>'
                                            )
                                        elif st_raw == 'ATIVO':
                                            rotulo_btn_membros = (
                                                'Gerenciar motoristas e moradores'
                                                if tipo_raw == 'COOPERATIVA'
                                                else 'Gerenciar moradores'
                                            )
                                            with ui.row().classes('gap-2').style('margin-top:12px'):
                                                eco_button(
                                                    rotulo_btn_membros,
                                                    lambda _e=None, item=inst: _abrir_modal_membros(item),
                                                    cls='dark',
                                                )
                                                eco_button(
                                                    'Ir para agendamentos',
                                                    lambda: ui.navigate.to('/agendamentos'),
                                                    cls='outline',
                                                )

                    if vinculos:
                        with card('Instituições em que sou membro'):
                            with ui.element('div').classes('list-divided'):
                                for v in vinculos:
                                    nome_v = str(v.get('nomeInstituicao') or f"Instituição #{v.get('instituicaoId')}")
                                    papel_v = str(v.get('papel') or 'MORADOR').upper()
                                    tone_v = 'blue' if papel_v == 'MOTORISTA' else 'green'
                                    with ui.element('div').classes('item'):
                                        with ui.element('div').style('flex:1;min-width:0'):
                                            h(
                                                f'<div class="item-title" style="font-size:15px">{escape(nome_v)}</div>'
                                                f'<div class="item-meta" style="font-size:12px">'
                                                f'Vínculo ativo como {escape(papel_v.lower())}</div>'
                                            )
                                        h(f'<span class="badge b-{tone_v}">{escape(papel_v.capitalize())}</span>')
                                        if papel_v == 'MOTORISTA':
                                            eco_button(
                                                'Ver minha rota',
                                                lambda: ui.navigate.to('/rotas'),
                                                cls='outline',
                                            )

                with card('Solicitar cadastro de instituição'):
                    h(
                        '<div class="muted" style="font-size:12px;margin:-6px 0 12px">'
                        'Após o envio, a solicitação ficará em análise até aprovação do administrador.</div>'
                    )
                    with ui.element('div').classes('form-grid'):
                        with ui.element('div'):
                            h('<label class="form-label">Tipo da instituição</label>')
                            campo_tipo = ui.select(
                                _TIPOS_INSTITUICAO_OPCOES,
                                value='COOPERATIVA',
                            ).props(
                                'outlined dense hide-bottom-space options-dense aria-label="Tipo da instituição"'
                            ).classes('eco-field')

                        campo_nome = form_field('Nome da instituição', placeholder='Ex: Coop. Recicla BA')
                        with ui.element('div').classes('form-grid cols-2'):
                            campo_cnpj = form_field(
                                'CNPJ',
                                placeholder='Somente números (14 dígitos)',
                                props='mask="##############" inputmode="numeric"',
                            )
                            campo_tel = form_field(
                                'Celular / Telefone',
                                placeholder='73999999999',
                                props='mask="###########" inputmode="numeric"',
                            )

                        with ui.element('div').classes('form-grid cols-2'):
                            campo_logradouro = form_field('Logradouro', placeholder='Av. Porto Seguro')
                            campo_numero = form_field('Número', placeholder='120')

                        with ui.element('div').classes('form-grid cols-2'):
                            campo_bairro = form_field('Bairro', placeholder='Centro')
                            campo_cidade = form_field('Cidade', placeholder='Eunápolis')

                        with ui.element('div').classes('form-grid cols-2'):
                            campo_uf = form_field('UF', value='BA', placeholder='BA', props='mask="AA"')
                            campo_cep = form_field(
                                'CEP',
                                placeholder='45820000',
                                props='mask="########" inputmode="numeric"',
                            )

                        def solicitar_cadastro() -> None:
                            if not _obrigatorios(
                                nome=campo_nome,
                                CNPJ=campo_cnpj,
                                telefone=campo_tel,
                                logradouro=campo_logradouro,
                                número=campo_numero,
                                bairro=campo_bairro,
                                cidade=campo_cidade,
                                UF=campo_uf,
                                CEP=campo_cep,
                            ):
                                return

                            nome_val = (campo_nome.value or '').strip()
                            if len(nome_val) < 3 or len(nome_val) > 150:
                                ui.notify('O nome deve conter entre 3 e 150 caracteres.', type='warning')
                                return

                            cnpj_digitos = re.sub(r'\D', '', campo_cnpj.value or '')
                            if len(cnpj_digitos) != 14:
                                ui.notify('Informe um CNPJ válido com 14 dígitos (somente números).', type='warning')
                                return

                            tel_digitos = re.sub(r'\D', '', campo_tel.value or '')
                            if len(tel_digitos) not in (10, 11):
                                ui.notify('Informe o celular/telefone com DDD (ex: 73999999999).', type='warning')
                                return

                            uf_val = (campo_uf.value or '').strip().upper()
                            if len(uf_val) != 2 or not uf_val.isalpha():
                                ui.notify('Informe a UF com 2 letras (ex: BA).', type='warning')
                                return

                            cep_limpo = re.sub(r'\D', '', campo_cep.value or '')
                            if len(cep_limpo) != 8:
                                ui.notify('Informe um CEP válido com 8 dígitos (somente números).', type='warning')
                                return

                            payload = {
                                'nome': nome_val,
                                'documento': cnpj_digitos,
                                'tipo': str(campo_tipo.value or 'COOPERATIVA').upper(),
                                'telefone': tel_digitos,
                                'logradouro': (campo_logradouro.value or '').strip(),
                                'numero': (campo_numero.value or '').strip(),
                                'bairro': (campo_bairro.value or '').strip(),
                                'cidade': (campo_cidade.value or '').strip(),
                                'estado': uf_val,
                                'cep': cep_limpo,
                            }

                            try:
                                criada = cadastrar_instituicao(payload)
                                ui.notify(
                                    f'Solicitação de "{criada.get("nome") or nome_val}" enviada para análise!',
                                    type='positive',
                                )
                                campo_nome.value = ''
                                campo_cnpj.value = ''
                                campo_tel.value = ''
                                campo_logradouro.value = ''
                                campo_numero.value = ''
                                campo_bairro.value = ''
                                campo_cidade.value = ''
                                campo_cep.value = ''
                                conteudo_instituicao.refresh()
                            except ApiError as exc:
                                ui.notify(exc.message, type='negative')

                        eco_button('Solicitar cadastro', solicitar_cadastro, cls='dark eco-block')

        conteudo_instituicao()