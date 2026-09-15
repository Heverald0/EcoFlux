"""Checagem dos helpers de HTML do ui_kit: python test_ui_kit.py"""

import mocks as m
from ui_kit import badge_html, bar_html, donut, sidebar_html


def test_badge_tone() -> None:
    assert 'b-green' in badge_html(m.Badge('Agendada'))          # tom pelo rotulo
    assert 'b-red' in badge_html(m.Badge('Análise', 'red'))      # tom explicito vence
    assert 'b-amber' in badge_html(m.Badge('Análise'))
    assert 'b-gray' in badge_html(m.Badge('Rotulo novo'))        # sem tom conhecido
    assert '&lt;b&gt;' in badge_html(m.Badge('<b>x</b>'))        # texto escapado


def test_bar_width() -> None:
    assert 'flex:0 0' not in bar_html(60)                        # sem largura: flex:1 do CSS
    assert 'flex:0 0 60px' in bar_html(60, width='60px')
    assert 'fill light' in bar_html(60, light=True)


def test_donut_stops_acumulam() -> None:
    html = donut([m.Slice('Papel', 39, '#111'), m.Slice('Plástico', 29, '#222')])
    assert '#111 0% 39%' in html and '#222 39% 68%' in html


def test_sidebar_marca_rota_ativa() -> None:
    html = sidebar_html('/materiais')
    assert html.count('aria-current="page"') == 1
    assert 'href="/materiais" aria-current="page"' in html


if __name__ == '__main__':
    for nome, caso in sorted(globals().items()):
        if nome.startswith('test_'):
            caso()
            print('ok', nome)
