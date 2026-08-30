from nicegui import ui
from pathlib import Path

CAMINHO_LOGO = Path(__file__).parent.parent / 'imagens' / 'logoEcoFlux.png'

API_BASE_URL = "http://localhost:8080/api" # temporario
PRIMARY_GREEN = '#166534'
ui.colors(primary=PRIMARY_GREEN, positive=PRIMARY_GREEN)

async def enviar_solicitacao():
    ui.notify('Solicitação enviada! (Simulação)', color='primary')

ui.query('body').classes('bg-[#f8f9fa] font-sans')
ui.query('.q-field--filled .q-field--control').classes('rounded-lg bg-[#f0f2f4]')
ui.query('.q-table__card').classes('shadow-none border-none')

with ui.left_drawer(value=True, fixed=True).props('width=280').classes('bg-[#eceef0] text-gray-800 border-r border-gray-200 shadow-lg p-0'):
    
    with ui.column().classes('w-full h-full justify-between no-wrap gap-0'):
        
        with ui.column().classes('w-full px-6 py-6 gap-2 overflow-y-auto'):
            
            ui.image(CAMINHO_LOGO).classes('mx-auto mb-6').style('width: 200px;')
            
            ui.label('Principal').classes('text-xs text-gray-500 font-bold ml-2 mt-2')
  
            ui.button('Dashboard', icon='grid_view').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')
            
            ui.label('Coleta').classes('text-xs text-gray-500 font-bold ml-2 mt-4')
            ui.button('Pontos de coleta', icon='location_on').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')
            # Botão ativo continua com a cor primária de fundo
            ui.button('Agendamentos', icon='calendar_today', color='primary').classes('w-full justify-start text-white font-semibold flat-btn text-sm rounded-lg shadow-sm').props('unelevated')
            ui.button('Materiais', icon='inventory_2').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')
            
            ui.label('Desempenho').classes('text-xs text-gray-500 font-bold ml-2 mt-4')
            ui.button('EcoScore', icon='emoji_events').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')
            ui.button('EcoSelo', icon='star_border').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')
            ui.button('EcoImpacto', icon='bar_chart').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')
            
            ui.label('Gestão').classes('text-xs text-gray-500 font-bold ml-2 mt-4')
            ui.button('Relatórios', icon='receipt_long').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')
            ui.button('Área Educativa', icon='home').classes('w-full justify-start text-gray-700 hover:bg-gray-200 flat-btn text-sm rounded-lg').props('flat text-color="grey-9"')

        with ui.row().classes('w-full px-6 py-5 items-center gap-3 no-wrap border-t border-gray-300 mt-auto bg-gray-100/50 shrink-0'):
            ui.image('https://randomuser.me/api/portraits/men/32.jpg').classes('w-10 h-10 rounded-full shadow-md shrink-0')
            with ui.column().classes('gap-0 overflow-hidden'):
                ui.label('Luca Soares').classes('font-semibold text-sm text-gray-900 truncate')
                ui.label('administrador').classes('text-xs text-gray-600 truncate')


with ui.header().classes('bg-white text-black flex items-center justify-between px-10 py-4 border-b border-gray-200 shadow-none'):
    ui.label('Agendamentos').classes('text-lg font-medium text-gray-900')
    with ui.row().classes('items-center gap-5'):
        ui.label('EcoScore: 1.840 pontos').classes('text-white px-5 py-2 rounded-full text-sm font-bold shadow-sm').style(f'background-color: {PRIMARY_GREEN};')
        ui.button(icon='eco', color='primary').props('round flat').classes('bg-gray-200 text-gray-700')


with ui.column().classes('w-full max-w-7xl mx-auto p-10 gap-8'):
    
    with ui.column().classes('gap-1.5 mb-2'):
        ui.label('Agendamento de retirada').classes('text-3xl font-semibold text-gray-950')
        ui.label('Solicite a coleta seletiva e acompanhe o status da solicitação').classes('text-gray-700 text-base')
    
    with ui.row().classes('w-full gap-8 items-stretch'):
        
        with ui.card().classes('flex-1 p-10 rounded-3xl shadow-xl border border-gray-100 bg-white'):
            ui.label('Nova solicitação').classes('text-xl font-semibold mb-6 text-gray-900')
            
            input_props = 'filled dense borderless'
            
            select_inst = ui.select(['Cond. Parque Verde'], value='Cond. Parque Verde', label='Instituição solicitante').props(input_props).classes('w-full mb-3')
            input_tipo = ui.input(value='Papel e papelão', label='Tipo do material').props(input_props).classes('w-full mb-3')
            
            with ui.row().classes('w-full gap-5 mb-3'):
                input_data = ui.input(placeholder='dd/mm/aaaa', label='Data preferencial').props(input_props).classes('flex-1')
                with input_data.add_slot('append'):
                    ui.icon('calendar_today').classes('cursor-pointer text-gray-500').on('click', lambda: date_picker.toggle())
                    with ui.dialog() as date_picker, ui.date().bind_value(input_data):
                        with ui.card():
                            ui.date()
                
                select_hora = ui.select(['08:00 - 10:00'], value='08:00 - 10:00', label='Horários').props(input_props).classes('flex-1')
                
            with ui.row().classes('w-full gap-5 mb-3'):
                input_qtd = ui.input(value='50', label='Quantidade estimada').props(input_props).classes('flex-1')
                with input_qtd.add_slot('append'):
                    ui.label('KG').classes('text-gray-500 text-sm font-medium')
                select_coop = ui.select(['Qualquer disponível'], value='Qualquer disponível', label='Cooperativa').props(input_props).classes('flex-1')
                
            input_obs = ui.input(placeholder='Informações disponíveis...', label='Observações').props(input_props).classes('w-full mb-6')
            
            ui.button('Enviar Solicitação', on_click=enviar_solicitacao, color='primary').classes('w-full text-white py-4 rounded-xl shadow-lg capitalize font-semibold text-base').props('unelevated')

        with ui.card().classes('w-[35%] p-10 rounded-3xl shadow-xl border border-gray-100 bg-white'):
            ui.label('Status da solicitação # 2041').classes('text-xl font-semibold mb-8 text-gray-900')
            
            with ui.timeline(color='primary').classes('w-full').props('layout="dense"'):
                ui.timeline_entry('Enviada', subtitle='08 abr - 09:14', icon='check_circle').classes('text-gray-900')
                ui.timeline_entry('Em análise', subtitle='08 abr - 14:30', icon='check_circle').classes('text-gray-900')
                ui.timeline_entry('Agendada', subtitle='10 abr - Coletada em 11 de abr', icon='check_circle').classes('text-gray-900')
                ui.timeline_entry('Aguardando', subtitle='Aguardando...', icon='radio_button_unchecked', color='grey-4').classes('opacity-60 text-gray-600')
                ui.timeline_entry('Enviada', subtitle='Aguardando...', icon='radio_button_unchecked', color='grey-4').classes('opacity-60 text-gray-600')

    with ui.card().classes('w-full p-10 rounded-3xl shadow-xl border border-gray-100 bg-white mt-4'):
        ui.label('Históricos de coletas').classes('text-xl font-semibold mb-6 text-gray-900')
        
        colunas = [
            {'name': 'data', 'label': 'Data', 'field': 'data', 'align': 'left', 'classes': 'text-gray-700 text-sm font-medium'},
            {'name': 'material', 'label': 'Material', 'field': 'material', 'align': 'left', 'classes': 'text-gray-700 text-sm font-medium'},
            {'name': 'quantidade', 'label': 'Quantidade', 'field': 'quantidade', 'align': 'left', 'classes': 'text-gray-700 text-sm font-medium'},
            {'name': 'status', 'label': 'Status', 'field': 'status', 'align': 'right'},
        ]
        
        linhas = [
            {'data': '02 Abr', 'material': 'Papel', 'quantidade': '80 kg', 'status': 'Realizada'},
            {'data': '18 Mar', 'material': 'Plástico', 'quantidade': '45 kg', 'status': 'Realizada'},
            {'data': '05 Mar', 'material': 'Metal', 'quantidade': '30 kg', 'status': 'Realizada'},
            {'data': '12 Fev', 'material': 'Óleo', 'quantidade': '150 L', 'status': 'Realizada'},
        ]
        
        table_historico = ui.table(columns=colunas, rows=linhas, row_key='data').classes('w-full flat')
        
        table_historico.add_slot('header', '''
            <q-tr :props="props" class="bg-[#f0f2f4] rounded-lg">
                <q-th v-for="col in props.cols" :key="col.name" :props="props" class="text-gray-800 text-sm font-bold border-none">
                    {{ col.label }}
                </q-th>
            </q-tr>
        ''')
        
        table_historico.add_slot('body-cell-status', '''
            <q-td :props="props">
                <q-badge color="primary" class="px-4 py-1.5 rounded-full text-xs font-bold shadow-sm" v-if="props.value == 'Realizada'">
                    {{ props.value }}
                </q-badge>
            </q-td>
        ''')

ui.add_head_html('''
    <style>
        .flat-btn .q-btn__content {
            justify-content: flex-start;
            text-transform: none;
            letter-spacing: normal;
        }
    </style>
''')

ui.run(title='EcoFlux - Agendamentos')