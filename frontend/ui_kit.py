"""Design system do EcoFlux: CSS, layout base e componentes reutilizaveis.

Os blocos estaticos (tabelas, listas, badges, barras, graficos) sao emitidos
como HTML puro - e menos codigo do que aninhar elementos e nao exige nenhuma
biblioteca de graficos: as barras sao divs e as roscas sao conic-gradient.
"""

from __future__ import annotations

from contextlib import contextmanager
from html import escape
from typing import Iterable

from nicegui import ui

import mocks as m

PRIMARY = '#1F8A43'

TITLES = {
    '/dashboard': 'Dashboard',
    '/pontos-de-coleta': 'Pontos de coleta',
    '/agendamentos': 'Agendamentos',
    '/materiais': 'Controle de materiais',
    '/ecoscore': 'EcoScore',
    '/ecoselo': 'EcoSelo',
    '/ecoimpacto': 'EcoImpacto',
    '/relatorios': 'Relatórios',
    '/area-educativa': 'Área Educativa',
    '/administracao': 'Administração',
}

NAV = [
    ('Principal', [
        ('Dashboard', '/dashboard', 'space_dashboard'),
    ]),
    ('Coleta', [
        ('Pontos de coleta', '/pontos-de-coleta', 'location_on'),
        ('Agendamentos', '/agendamentos', 'calendar_month'),
        ('Materiais', '/materiais', 'inventory_2'),
    ]),
    ('Desempenho', [
        ('EcoScore', '/ecoscore', 'emoji_events'),
        ('EcoSelo', '/ecoselo', 'star'),
        ('EcoImpacto', '/ecoimpacto', 'bar_chart'),
    ]),
    ('Gestão', [
        ('Relatórios', '/relatorios', 'description'),
        ('Área Educativa', '/area-educativa', 'school'),
    ]),
]

TONES = {
    'agendada': 'green', 'ativo': 'green', 'enviado': 'green', 'ouro': 'green',
    'realizada': 'solid', 'realizado': 'solid', 'comercializado': 'solid',
    'em análise': 'amber', 'análise': 'amber', 'analise': 'amber', 'analisar': 'amber',
    'triagem': 'amber', 'manutenção': 'amber', 'pendente': 'amber', 'bronze': 'amber',
    'enviada': 'blue', 'coletado': 'blue', 'pronto': 'blue', 'prata': 'blue',
    'cancelada': 'red', 'bloqueado': 'red',
    'inicial': 'gray',
}

DOT_COLORS = {
    'green': '#DCFCE7', 'blue': '#DBEAFE', 'amber': '#FEF3C7',
    'red': '#FEE2E2', 'muted': '#EDEDED',
}
ICON_COLORS = {
    'green': PRIMARY, 'blue': '#1D4ED8', 'amber': '#F0B429',
    'red': '#B91C1C', 'muted': '#8A8A8A',
}

STYLES = '''
<style>
  .eco-app {
    --primary: #1F8A43;
    --primary-dark: #17693A;
    --primary-light: #4ADE80;
    --primary-soft: #DCFCE7;
    --bg-app: #F5F7F8;
    --bg-sidebar: #D9DDE0;
    --surface: #fff;
    --input-bg: #EFEAEA;
    --table-head: #E5E5E5;
    --track: #D9D9D9;
    --text: #1A1A1A;
    --muted: #8A8A8A;
    --line: #EFEFEF;
    display: flex;
    width: 100%;
    min-height: 100vh;
    color: var(--text);
    line-height: 1.4;
    background: var(--bg-app);
    font-family: Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif;
  }
  .eco-app * { box-sizing: border-box; }

  /* --- sidebar --- */
  .sidebar {
    position: fixed;
    z-index: 30;
    top: 0;
    bottom: 0;
    left: 0;
    display: flex;
    width: 250px;
    flex-direction: column;
    padding: 22px 16px 16px;
    background: var(--bg-sidebar);
    overflow-y: auto;
  }
  .brand {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 6px 18px;
    color: var(--primary-dark);
    font-size: 20px;
    font-weight: 700;
  }
  .brand .material-symbols-outlined { font-size: 26px; color: var(--primary); }
  .nav-group { padding: 12px 0; }
  .nav-group + .nav-group { border-top: 1px solid #A8ADB0; }
  .nav-title { padding: 0 6px 6px; color: var(--text); font-size: 11px; }
  .nav-item {
    display: flex;
    height: 28px;
    align-items: center;
    gap: 10px;
    padding: 0 6px;
    border-radius: 6px;
    color: var(--text);
    font-size: 11px;
    text-decoration: none;
  }
  .nav-item .material-symbols-outlined { font-size: 16px; }
  .nav-item:hover { background: rgba(255, 255, 255, .45); }
  .nav-item[aria-current="page"] {
    background: rgba(255, 255, 255, .7);
    box-shadow: inset 2px 0 0 var(--primary);
    font-weight: 500;
  }
  .sidebar-footer { margin-top: auto; padding-top: 14px; border-top: 1px solid #A8ADB0; }
  .user-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 6px;
    border-radius: 10px;
    color: var(--text);
    text-decoration: none;
  }
  .user-card:hover { background: rgba(255, 255, 255, .5); }
  .user-card[aria-current="page"] { background: rgba(255, 255, 255, .7); }
  .user-name { font-size: 12px; font-weight: 500; }
  .user-role { color: var(--text); font-size: 10px; }
  .avatar {
    display: grid;
    width: 32px;
    height: 32px;
    flex: 0 0 32px;
    place-items: center;
    border: none;
    border-radius: 50%;
    color: var(--primary);
    background: #E5E5E5;
  }
  .avatar .material-symbols-outlined { font-size: 18px; }

  /* --- topbar + conteudo --- */
  .main { display: flex; min-width: 0; flex: 1; flex-direction: column; margin-left: 250px; }
  .topbar {
    position: sticky;
    z-index: 20;
    top: 0;
    display: flex;
    height: 52px;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    background: var(--surface);
  }
  .topbar-left { display: flex; align-items: center; gap: 10px; font-size: 12px; }
  .topbar-right { display: flex; align-items: center; gap: 12px; }
  .score-badge {
    padding: 4px 12px;
    border-radius: 999px;
    color: #14532D;
    background: var(--primary-light);
    font-size: 10px;
    font-weight: 500;
  }
  .avatar-btn { width: 36px; height: 36px; flex: 0 0 36px; cursor: pointer; }
  .hamburger {
    display: none;
    padding: 4px;
    border: none;
    border-radius: 6px;
    background: transparent;
    cursor: pointer;
  }
  .content { display: flex; flex-direction: column; gap: 20px; padding: 20px 24px 32px; }
  .page-title { margin: 0; font-size: 22px; font-weight: 500; line-height: 1.3; }
  .page-subtitle { margin-top: 4px; font-size: 12px; }

  /* --- grids --- */
  .grid-2, .grid-3, .grid-4, .grid-65 { display: grid; gap: 20px; }
  .grid-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .grid-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .grid-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
  .grid-65 { grid-template-columns: 1.85fr 1fr; }
  .narrow { width: 65%; margin: 0 auto; }
  .narrow-45 { width: 46%; margin: 0 auto; }
  .stack { display: flex; flex-direction: column; gap: 20px; }

  /* --- cards --- */
  .card {
    padding: 20px 22px;
    border-radius: 20px;
    background: var(--surface);
    box-shadow: 0 4px 8px rgba(0, 0, 0, .18);
  }
  .card-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
  }
  .card-title { font-size: 14px; font-weight: 500; }
  .card.card-sm .card-title { font-size: 11px; }
  .card-subtitle { margin-top: 3px; color: var(--muted); font-size: 8px; }
  .link-more { color: var(--primary); font-size: 10px; text-decoration: none; }
  .link-more:hover { text-decoration: underline; }

  /* --- kpi --- */
  .kpi { display: flex; min-height: 80px; flex-direction: column; justify-content: center; gap: 6px; }
  .kpi-label { font-size: 11px; text-transform: uppercase; }
  .kpi-value { font-size: 23px; font-weight: 600; }
  .kpi-hint { color: var(--primary); font-size: 11px; }
  .kpi-hint.plain { color: var(--text); }
  .kpi.is-high { color: #fff; background: var(--primary); }
  .kpi.is-high .kpi-hint, .kpi.is-high .kpi-hint.plain { color: rgba(255, 255, 255, .92); }

  /* --- tabelas --- */
  .tbl-wrap { overflow-x: auto; }
  .tbl { width: 100%; border-collapse: separate; border-spacing: 0; font-size: 12px; }
  .tbl th {
    padding: 7px 12px;
    background: var(--table-head);
    font-size: 10px;
    font-weight: 500;
    text-align: left;
    white-space: nowrap;
  }
  .tbl th:first-child { border-radius: 8px 0 0 8px; }
  .tbl th:last-child { border-radius: 0 8px 8px 0; }
  .tbl td { padding: 10px 12px; vertical-align: middle; }
  .tbl.divided tbody tr + tr td { border-top: 1px solid var(--line); }
  .tbl.tall td { padding: 14px 12px; }
  .tbl td.act { text-align: right; }

  /* --- badges --- */
  .badge {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 999px;
    font-size: 10px;
    font-weight: 500;
    white-space: nowrap;
  }
  .b-green { color: #15803D; background: #DCFCE7; }
  .b-solid { color: #fff; background: #15803D; }
  .b-amber { color: #B45309; background: #FEF3C7; }
  .b-blue { color: #1D4ED8; background: #DBEAFE; }
  .b-red { color: #B91C1C; background: #FEE2E2; }
  .b-gray { color: #374151; background: #E5E7EB; }
  .b-gold { color: #1A1A1A; background: #FBBF24; }

  /* --- barras / graficos --- */
  .track { overflow: hidden; height: 6px; flex: 1; border-radius: 999px; background: var(--track); }
  .fill { height: 100%; border-radius: 999px; background: var(--primary); }
  .fill.light { background: var(--primary-light); }
  .chart { display: flex; height: 150px; align-items: flex-end; gap: 10px; }
  .chart > div { flex: 1; background: var(--primary); }
  .chart > div.light { background: var(--primary-light); }
  .chart-x { display: flex; gap: 10px; margin-top: 8px; }
  .chart-x > span { flex: 1; font-size: 11px; text-align: center; }
  .donut { position: relative; flex: 0 0 auto; border-radius: 50%; }
  .donut-hole { position: absolute; border-radius: 50%; background: var(--surface); }
  .legend { display: flex; flex: 1; flex-direction: column; gap: 10px; font-size: 11px; }
  .legend-row { display: flex; align-items: center; gap: 8px; }
  .legend-chip { width: 10px; height: 10px; flex: 0 0 10px; border-radius: 3px; }
  .legend-row span:nth-child(2) { flex: 1; }

  /* --- listas --- */
  .rank-row { display: flex; align-items: center; gap: 10px; padding: 7px 0; }
  .list-divided .rank-row + .rank-row, .list-divided .item + .item { border-top: 1px solid var(--line); }
  .rank-num { width: 10px; color: var(--muted); font-size: 9px; }
  .rank-num.top { color: #E8892B; }
  .rank-body { min-width: 0; flex: 1; }
  .rank-name { margin-bottom: 4px; font-size: 8px; }
  .rank-value { width: 46px; font-size: 10px; text-align: right; }
  .rank-value.green { color: var(--primary); }
  .metric-row { display: flex; align-items: center; gap: 10px; padding: 5px 0; font-size: 9px; }
  .metric-row .label { width: 30%; }
  .metric-row .value { width: 58px; text-align: right; }
  .metric-stack { padding: 6px 0; font-size: 9px; }
  .metric-stack .top-line { display: flex; justify-content: space-between; margin-bottom: 4px; }

  .item { display: flex; align-items: center; gap: 10px; padding: 10px 0; }
  .item-dot { width: 20px; height: 20px; flex: 0 0 20px; border-radius: 50%; }
  .item-ico { display: grid; width: 22px; height: 22px; flex: 0 0 22px; place-items: center; }
  .item-ico .material-symbols-outlined { font-size: 17px; }
  .item-ext {
    display: grid;
    width: 26px;
    height: 26px;
    flex: 0 0 26px;
    place-items: center;
    border-radius: 5px;
    color: var(--primary);
    background: #EDEDED;
    font-size: 7px;
    font-weight: 600;
  }
  .item-title { font-size: 11px; font-weight: 500; }
  .item-title.big { color: var(--primary); font-size: 14px; font-weight: 600; }
  .item-meta { margin-top: 2px; color: var(--muted); font-size: 8px; }
  .item-right { margin-left: auto; color: var(--muted); font-size: 8px; }

  .content-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; }
  .thumb { width: 40px; height: 40px; flex: 0 0 40px; border-radius: 6px; }

  /* --- timeline / stepper --- */
  .tl-step { display: flex; align-items: center; gap: 10px; }
  .tl-dot { display: grid; width: 16px; height: 16px; flex: 0 0 16px; place-items: center; border-radius: 50%; }
  .tl-dot.done { border: 1.5px solid var(--primary); color: var(--primary); }
  .tl-dot.todo { border: 1.5px solid #C9CDD1; }
  .tl-dot .material-symbols-outlined { font-size: 11px; }
  .tl-title { font-size: 11px; }
  .tl-meta { margin-top: 2px; color: var(--muted); font-size: 7px; }
  .tl-arrow { padding: 4px 0 4px 6px; font-size: 13px; line-height: 1; }
  .tl-arrow.todo { color: #C9CDD1; }
  .step-num {
    display: grid;
    width: 18px;
    height: 18px;
    flex: 0 0 18px;
    place-items: center;
    border-radius: 50%;
    color: #fff;
    background: var(--primary);
    font-size: 9px;
  }
  .step-num.todo { color: var(--muted); background: #E5E5E5; }

  /* --- mapa (placeholder) --- */
  .map-box {
    display: flex;
    min-height: 240px;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    text-align: center;
  }
  .map-box .material-symbols-outlined { font-size: 46px; color: #9AA0A6; }

  /* --- selo --- */
  .seal {
    display: grid;
    width: 80px;
    height: 80px;
    flex: 0 0 80px;
    place-items: center;
    border: 7px solid var(--primary-light);
    border-radius: 50%;
  }
  .seal-inner {
    display: grid;
    width: 52px;
    height: 52px;
    place-items: center;
    border: 1px solid var(--primary);
    border-radius: 50%;
    color: var(--primary);
    background: var(--primary-soft);
    font-size: 11px;
  }

  /* --- formularios --- */
  .form-grid { display: grid; gap: 14px; }
  .form-grid.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .form-grid.cols-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
  .form-grid .cell-end { align-self: end; }
  .form-label { margin-bottom: 4px; font-size: 11px; }
  .form-label.upper { font-size: 9px; text-transform: uppercase; }
  .eco-field { width: 100%; margin: 0; }
  .eco-field .q-field__control,
  .eco-field .q-field__native,
  .eco-field .q-field__marginal { height: 28px !important; min-height: 28px !important; }
  .eco-field .q-field__control {
    padding: 0 10px;
    border-radius: 6px !important;
    background: var(--input-bg);
  }
  .eco-field .q-field__control::before,
  .eco-field .q-field__control::after { border: none !important; }
  .eco-field.white .q-field__control { border: 1px solid #CFCFCF !important; background: #fff; }
  .eco-field .q-field__native, .eco-field .q-field__input {
    padding: 0 !important;
    font-size: 11px !important;
  }
  .eco-field .q-field__native::placeholder { color: var(--muted); opacity: 1; }
  .eco-field .q-field__bottom { display: none; }
  .eco-field.q-field--focused .q-field__control { box-shadow: 0 0 0 3px rgba(31, 138, 67, .18); }
  .suffix { color: var(--muted); font-size: 10px; }

  .q-btn.eco-btn {
    height: 28px;
    min-height: 28px !important;
    padding: 0 16px;
    border-radius: 6px !important;
    color: #fff !important;
    background: var(--primary) !important;
    box-shadow: none !important;
    font-size: 11px !important;
    font-weight: 500 !important;
    text-transform: none !important;
  }
  .q-btn.eco-btn.dark { background: var(--primary-dark) !important; }
  .q-btn.eco-btn.eco-block { width: 100%; height: 32px; min-height: 32px !important; }
  .q-btn.eco-btn.outline {
    color: var(--text) !important;
    background: #fff !important;
    border: 1px solid #CFCFCF;
  }
  .q-btn.eco-btn.outline.on {
    color: #fff !important;
    background: var(--primary) !important;
    border-color: var(--primary);
  }
  .chip {
    padding: 3px 12px;
    border: none;
    border-radius: 999px;
    background: #F0F0F0;
    color: var(--text);
    font-size: 10px;
    cursor: pointer;
  }
  .chip.on { color: #15803D; background: var(--primary-soft); }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .btn-ghost {
    padding: 2px 12px;
    border: 1px solid var(--primary-dark);
    border-radius: 999px;
    background: #fff;
    color: var(--text);
    font-size: 9px;
    cursor: pointer;
  }
  .quiz-box {
    display: grid;
    min-height: 50px;
    place-items: center;
    padding: 12px;
    border-radius: 10px;
    color: #15803D;
    background: var(--primary-soft);
    font-size: 12px;
    text-align: center;
  }

  .muted { color: var(--muted); }
  .eco-app a:focus-visible,
  .eco-app button:focus-visible,
  .eco-app .q-btn:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }

  @media (max-width: 1024px) {
    .sidebar { transform: translateX(-100%); transition: transform .2s ease; }
    .sidebar.open { transform: none; box-shadow: 0 0 24px rgba(0, 0, 0, .25); }
    .main { margin-left: 0; }
    .hamburger { display: inline-flex; }
    .grid-4 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .grid-2, .grid-3, .grid-65 { grid-template-columns: minmax(0, 1fr); }
    .narrow, .narrow-45 { width: 100%; }
    .form-grid.cols-4 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .tbl { min-width: 620px; }  /* forca o scroll horizontal em .tbl-wrap */
  }
  @media (max-width: 640px) {
    .grid-4 { grid-template-columns: minmax(0, 1fr); }
    .content { padding: 16px 14px 28px; }
    .score-badge { display: none; }
    .form-grid.cols-2, .form-grid.cols-4 { grid-template-columns: minmax(0, 1fr); }
  }
</style>
<script>
  function ecoToast(message) {
    Quasar.Notify.create({message: message, position: 'bottom', timeout: 2500});
  }
</script>
'''


def h(markup: str):
    """Insere HTML bruto (os blocos estaticos do design system)."""
    return ui.html(markup, sanitize=False)


def icon(name: str, style: str = '') -> str:
    attr = f' style="{style}"' if style else ''
    return f'<span class="material-symbols-outlined"{attr}>{name}</span>'


def badge_html(badge: m.Badge) -> str:
    tone = badge.tone or TONES.get(badge.text.lower(), 'gray')
    return f'<span class="badge b-{tone}">{escape(badge.text)}</span>'


def bar_html(pct: int, light: bool = False, width: str = '') -> str:
    style = f' style="flex:0 0 {width};width:{width}"' if width else ''
    return (f'<div class="track"{style}><div class="fill{" light" if light else ""}" '
            f'style="width:{pct}%"></div></div>')


# --- layout ------------------------------------------------------------------
def sidebar_html(route: str) -> str:
    groups = []
    for title, items in NAV:
        links = ''.join(
            f'<a class="nav-item" href="{href}"'
            f'{" aria-current=\"page\"" if href == route else ""}>'
            f'{icon(ico)}{escape(label)}</a>'
            for label, href, ico in items
        )
        groups.append(f'<div class="nav-group"><div class="nav-title">{title}</div>{links}</div>')
    current = ' aria-current="page"' if route == '/administracao' else ''
    return f'''
      <aside class="sidebar">
        <div class="brand">{icon('eco')}EcoFlux</div>
        {''.join(groups)}
        <div class="sidebar-footer">
          <a class="user-card" href="/administracao"{current}>
            <span class="avatar">{icon('eco')}</span>
            <span>
              <span class="user-name">{m.USER_NAME}</span><br>
              <span class="user-role">{m.USER_ROLE}</span>
            </span>
          </a>
        </div>
      </aside>
    '''


@contextmanager
def layout(route: str):
    """Sidebar + topbar + area de conteudo; o corpo da pagina entra no `with`."""
    ui.colors(primary=PRIMARY)
    ui.page_title(f'EcoFlux | {TITLES[route]}')
    ui.add_head_html(STYLES, shared=True)

    with ui.element('div').classes('eco-app'):
        h(sidebar_html(route))
        with ui.element('div').classes('main'):
            with ui.element('header').classes('topbar'):
                h(f'''<div class="topbar-left">
                        <button class="hamburger" aria-label="Abrir menu" onclick="
                          document.querySelector('.sidebar').classList.toggle('open')">
                          {icon('menu')}</button>
                        <span>{TITLES[route]}</span>
                      </div>''')
                with ui.element('div').classes('topbar-right'):
                    h(f'<span class="score-badge">{m.USER_SCORE}</span>')
                    with ui.element('button').classes('avatar avatar-btn').props(
                        'aria-label="Conta e permissões"'
                    ):
                        h(icon('eco'))
                        with ui.menu():
                            ui.menu_item('Perfil', lambda: ui.notify('Perfil em breve.'))
                            ui.menu_item('Administração',
                                         lambda: ui.navigate.to('/administracao'))
                            ui.separator()
                            ui.menu_item('Sair', lambda: ui.navigate.to('/'))
            with ui.element('main').classes('content') as content:
                yield content


def page_header(title: str, subtitle: str) -> None:
    h(f'<div><h1 class="page-title">{escape(title)}</h1>'
      f'<div class="page-subtitle">{escape(subtitle)}</div></div>')


@contextmanager
def grid(kind: str = '2', extra: str = ''):
    with ui.element('div').classes(f'grid-{kind} {extra}'.strip()) as element:
        yield element


@contextmanager
def card(title: str = '', more: str = '', subtitle: str = '', cls: str = ''):
    with ui.element('div').classes(f'card {cls}'.strip()) as element:
        if title:
            right = f'<a class="link-more" href="#">{escape(more)} →</a>' if more else ''
            sub = f'<div class="card-subtitle">{escape(subtitle)}</div>' if subtitle else ''
            h(f'<div class="card-head"><div><div class="card-title">{escape(title)}</div>'
              f'{sub}</div>{right}</div>')
        yield element


def kpi_row(kpis: Iterable[m.Kpi]) -> None:
    cards = []
    for kpi in kpis:
        hint = (f'<div class="kpi-hint{" plain" if kpi.hint_plain else ""}">'
                f'{escape(kpi.hint)}</div>') if kpi.hint else ''
        cards.append(
            f'<div class="card kpi{" is-high" if kpi.highlighted else ""}">'
            f'<div class="kpi-label">{escape(kpi.label)}</div>'
            f'<div class="kpi-value">{escape(kpi.value)}</div>{hint}</div>'
        )
    h(f'<div class="grid-4">{"".join(cards)}</div>')


def data_table(table: m.Table, divided: bool = False, tall: bool = False,
               action: str = '') -> None:
    """Tabela estatica. `action` adiciona um botao por linha na ultima coluna."""
    head = ''.join(f'<th>{escape(col)}</th>' for col in table.columns)
    body = []
    for row in table.rows:
        cells = ''.join(
            f'<td>{badge_html(cell) if isinstance(cell, m.Badge) else escape(cell)}</td>'
            for cell in row
        )
        if action:
            label = escape(action)
            target = escape(str(row[0])).replace("'", '&#39;')
            cells += (f'<td class="act"><button class="btn-ghost" '
                      f'onclick="ecoToast(\'{label} {target}: em breve\')">{label}</button></td>')
        body.append(f'<tr>{cells}</tr>')
    classes = 'tbl' + (' divided' if divided else '') + (' tall' if tall else '')
    h(f'<div class="tbl-wrap"><table class="{classes}"><thead><tr>{head}</tr></thead>'
      f'<tbody>{"".join(body)}</tbody></table></div>')


def bar_chart(bars: Iterable[m.Bar]) -> None:
    bars = list(bars)
    cols = ''.join(
        f'<div class="{"light" if i % 2 else ""}" style="height:{bar.pct}%"></div>'
        for i, bar in enumerate(bars)
    )
    ticks = ''.join(f'<span>{escape(bar.label)}</span>' for bar in bars)
    h(f'<div><div class="chart">{cols}</div><div class="chart-x">{ticks}</div></div>')


def donut(slices: Iterable[m.Slice], size: int = 110, thickness: int = 16) -> str:
    stops, start = [], 0
    for piece in slices:
        end = start + piece.pct
        stops.append(f'{piece.color} {start}% {end}%')
        start = end
    return (f'<div class="donut" style="width:{size}px;height:{size}px;'
            f'background:conic-gradient({",".join(stops)})">'
            f'<div class="donut-hole" style="inset:{thickness}px"></div></div>')


def donut_with_legend(slices: list[m.Slice]) -> None:
    rows = ''.join(
        f'<div class="legend-row"><span class="legend-chip" style="background:{s.color}"></span>'
        f'<span>{escape(s.label)}</span><span>{s.pct}%</span></div>'
        for s in slices
    )
    h(f'<div style="display:flex;align-items:center;gap:24px">{donut(slices)}'
      f'<div class="legend">{rows}</div></div>')


def rank_list(metrics: Iterable[m.Metric], divided: bool = False, top_colored: int = 0,
              green_value: bool = False, footer: str = '') -> None:
    rows = []
    for i, metric in enumerate(metrics, start=1):
        num_cls = ' top' if i <= top_colored else ''
        rows.append(
            f'<div class="rank-row"><span class="rank-num{num_cls}">{i}</span>'
            f'<div class="rank-body"><div class="rank-name">{escape(metric.label)}</div>'
            f'{bar_html(metric.pct)}</div>'
            f'<span class="rank-value{" green" if green_value else ""}">'
            f'{escape(metric.value)}</span></div>'
        )
    note = f'<div class="muted" style="margin-top:10px;font-size:8px">{escape(footer)}</div>' \
        if footer else ''
    h(f'<div class="{"list-divided" if divided else ""}">{"".join(rows)}</div>{note}')


def metric_list(metrics: Iterable[m.Metric], alternate: bool = False,
                bar_width: str = '') -> None:
    """Linhas 'rotulo | barra | valor'."""
    rows = ''.join(
        f'<div class="metric-row"><span class="label">{escape(metric.label)}</span>'
        f'{bar_html(metric.pct, light=alternate and bool(i % 2), width=bar_width)}'
        f'<span class="value">{escape(metric.value)}</span></div>'
        for i, metric in enumerate(metrics)
    )
    h(f'<div>{rows}</div>')


def stacked_metric_list(metrics: Iterable[m.Metric]) -> None:
    """Rotulo e valor acima, barra abaixo (Metas ESG, Perfis de acesso)."""
    rows = ''.join(
        f'<div class="metric-stack"><div class="top-line"><span>{escape(metric.label)}</span>'
        f'<span>{escape(metric.value)}</span></div>'
        f'{bar_html(metric.pct, light=bool(i % 2))}</div>'
        for i, metric in enumerate(metrics)
    )
    h(f'<div>{rows}</div>')


def item_list(items: Iterable[m.Item], divided: bool = True, big_title: bool = False) -> None:
    rows = []
    for item in items:
        if item.icon:
            lead = (f'<span class="item-ico" style="color:{ICON_COLORS.get(item.tone, PRIMARY)}">'
                    f'{icon(item.icon)}</span>')
        elif item.ext:
            lead = f'<span class="item-ext">{escape(item.ext)}</span>'
        else:
            lead = f'<span class="item-dot" style="background:{DOT_COLORS.get(item.tone)}"></span>'
        rows.append(
            f'<div class="item">{lead}<div>'
            f'<div class="item-title{" big" if big_title else ""}">{escape(item.title)}</div>'
            f'<div class="item-meta">{escape(item.meta)}</div></div></div>'
        )
    h(f'<div class="{"list-divided" if divided else ""}">{"".join(rows)}</div>')


def timeline(steps: Iterable[m.Step]) -> None:
    steps = list(steps)
    blocks = []
    for i, step in enumerate(steps):
        dot = (f'<span class="tl-dot done">{icon("check")}</span>' if step.done
               else '<span class="tl-dot todo"></span>')
        blocks.append(
            f'<div class="tl-step">{dot}<div><div class="tl-title">{escape(step.title)}</div>'
            f'<div class="tl-meta">{escape(step.meta)}</div></div></div>'
        )
        if i < len(steps) - 1:
            todo = '' if step.done else ' todo'
            blocks.append(f'<div class="tl-arrow{todo}">↓</div>')
    h(f'<div>{"".join(blocks)}</div>')


def dot_steps(steps: Iterable[m.Step]) -> None:
    """Etapas com bolinha + status a direita (Etapas de certificacao)."""
    rows = ''.join(
        f'<div class="item"><span class="item-dot" style="width:9px;height:9px;flex:0 0 9px;'
        f'background:{PRIMARY if step.done else "#D9D9D9"}"></span>'
        f'<span class="item-title">{escape(step.title)}</span>'
        f'<span class="item-right">{escape(step.meta)}</span></div>'
        for step in steps
    )
    h(f'<div>{rows}</div>')


def numbered_steps(steps: Iterable[m.Step]) -> None:
    """Stepper numerado com barra a direita (Trilha recomendada)."""
    rows = ''.join(
        f'<div class="item"><span class="step-num{"" if step.done else " todo"}">{i}</span>'
        f'<span class="item-title" style="flex:0 0 88px">{escape(step.title)}</span>'
        f'{bar_html(100 if step.done else 35, light=step.partial)}</div>'
        for i, step in enumerate(steps, start=1)
    )
    h(f'<div>{rows}</div>')


def progress_footer(label: str, pct: int) -> None:
    h(f'<div class="metric-row" style="font-size:10px"><span class="label">{escape(label)}</span>'
      f'{bar_html(pct)}<span class="value" style="color:{PRIMARY}">{pct}%</span></div>')


# --- formularios -------------------------------------------------------------
def form_field(label: str, *, placeholder: str = '', value: str = '',
               options: list[str] | None = None, white: bool = False, upper: bool = False,
               span: str = '', props: str = ''):
    """Rotulo + campo. `options` produz um select; caso contrario, um input."""
    with ui.element('div') as box:
        if span:
            box.style(f'grid-column: {span}')
        h(f'<label class="form-label{" upper" if upper else ""}">{escape(label)}</label>')
        classes = f'eco-field{" white" if white else ""}'
        base = f'outlined dense hide-bottom-space aria-label="{escape(label)}"'
        if options is not None:
            element = ui.select(options, value=value).props(f'{base} options-dense {props}')
        else:
            element = ui.input(placeholder=placeholder, value=value).props(f'{base} {props}')
        element.classes(classes)
    return element


def eco_button(label: str, on_click=None, cls: str = '', props: str = 'unelevated no-caps'):
    """Botao do design system (color=None: sem as classes de cor do Quasar)."""
    return ui.button(label, on_click=on_click, color=None).props(props).classes(
        f'eco-btn {cls}'.strip())


def chip_select(options: list[str], selected: Iterable[str]) -> set[str]:
    """Chips alternaveis; o `set` devolvido reflete a selecao atual."""
    chosen = set(selected)

    def toggle(option: str, button: ui.button) -> None:
        if option in chosen:
            chosen.discard(option)
            button.classes(remove='on')
            button.props('aria-pressed=false')
        else:
            chosen.add(option)
            button.classes(add='on')
            button.props('aria-pressed=true')

    with ui.element('div').classes('chips'):
        for option in options:
            on = option in chosen
            button = ui.button(option, color=None).props(
                f'flat dense no-caps aria-pressed={str(on).lower()}'
            ).classes(f'chip{" on" if on else ""}')
            button.on_click(lambda o=option, b=button: toggle(o, b))
    return chosen
