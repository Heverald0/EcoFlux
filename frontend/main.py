from pathlib import Path

from nicegui import app, ui


ASSETS_DIR = Path(__file__).parent / 'assets'
app.add_static_file(
    local_file=ASSETS_DIR / 'ecoflux-logo.jpg',
    url_path='/assets/ecoflux-logo.jpg',
)


ui.add_head_html('''
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  :root {
    --brand-green-700: #15703c;
    --brand-green-800: #115c31;
    --brand-mint: #d9f7e5;
    --surface-page: #f1f3f5;
    --surface-card: #ffffff;
    --border-input: #d5d9de;
    --text-primary: #111827;
    --text-secondary: #6b7280;
  }

  * { box-sizing: border-box; }
  html, body, #q-app { width: 100%; min-height: 100%; margin: 0; }
  body {
    color: var(--text-primary);
    background: var(--surface-page);
    font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont,
      "Segoe UI", sans-serif;
  }
  .nicegui-content { padding: 0 !important; }

  .login-page {
    display: grid;
    grid-template-columns: minmax(420px, 40%) 1fr;
    width: 100%;
    min-height: 100vh;
    background: var(--surface-page);
  }

  .brand-panel {
    display: flex;
    min-height: 100vh;
    align-items: center;
    justify-content: flex-start;
    padding: 64px clamp(56px, 6.5vw, 100px);
    color: #fff;
    background: var(--brand-green-700);
  }

  .brand-content { width: min(400px, 100%); }
  .logo-card {
    display: flex;
    width: 192px;
    height: 192px;
    align-items: center;
    justify-content: center;
    margin-bottom: 32px;
    overflow: hidden;
    border-radius: 24px;
    background: #fff;
    box-shadow: 0 2px 8px rgba(7, 53, 28, .08);
  }
  .brand-logo {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .brand-title {
    margin: 0 0 16px;
    font-size: 40px;
    font-weight: 700;
    line-height: 1.15;
    letter-spacing: -.6px;
  }
  .brand-copy {
    max-width: 400px;
    margin: 0;
    color: rgba(255, 255, 255, .96);
    font-size: 16px;
    font-weight: 400;
    line-height: 1.5;
  }

  .benefits {
    display: flex;
    flex-direction: column;
    gap: 24px;
    margin-top: 48px;
  }
  .benefit {
    display: flex;
    min-height: 48px;
    align-items: center;
    gap: 16px;
  }
  .benefit-icon {
    display: flex;
    width: 48px;
    height: 48px;
    flex: 0 0 48px;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: var(--brand-green-800);
    background: var(--brand-mint);
  }
  .benefit-icon .q-icon { font-size: 24px; }
  .benefit-title {
    margin-bottom: 4px;
    color: #fff;
    font-size: 14px;
    font-weight: 700;
    line-height: 1.4;
  }
  .benefit-copy {
    color: rgba(255, 255, 255, .88);
    font-size: 12px;
    font-weight: 400;
    line-height: 1.4;
  }

  .form-panel {
    display: flex;
    min-height: 100vh;
    align-items: center;
    justify-content: center;
    padding: 64px;
    background: var(--surface-page);
  }
  .form-wrap {
    width: min(432px, 100%);
    transform: translateY(-26px);
  }
  .login-card {
    width: 100%;
    padding: 32px;
    border-radius: 16px;
    background: var(--surface-card);
    box-shadow: 0 12px 32px rgba(17, 24, 39, .09), 0 2px 6px rgba(17, 24, 39, .08);
  }
  .form-title {
    margin: 0 0 8px;
    color: var(--text-primary);
    font-size: 24px;
    font-weight: 700;
    line-height: 1.3;
  }
  .form-subtitle {
    margin-bottom: 32px;
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 400;
    line-height: 1.4;
  }
  .field-group { width: 100%; margin-bottom: 24px; }
  .field-group.password-group { margin-bottom: 32px; }
  .field-label {
    margin-bottom: 8px;
    color: var(--text-primary);
    font-size: 14px;
    font-weight: 600;
    line-height: 1.4;
  }

  .eco-input { width: 100%; margin: 0; }
  .eco-input .q-field__control,
  .eco-input .q-field__native {
    height: 48px !important;
    min-height: 48px !important;
  }
  .eco-input .q-field__control {
    border-radius: 8px !important;
    background: #fff;
    color: var(--border-input);
  }
  .eco-input .q-field__control::before { border: 1px solid var(--border-input) !important; }
  .eco-input .q-field__control:hover::before { border-color: #9ca3af !important; }
  .eco-input.q-field--focused .q-field__control {
    box-shadow: 0 0 0 3px rgba(21, 112, 60, .15);
  }
  .eco-input.q-field--focused .q-field__control::after {
    border: 2px solid var(--brand-green-700) !important;
  }
  .eco-input .q-field__native {
    padding: 0 14px !important;
    color: var(--text-primary);
    font-size: 14px !important;
  }
  .eco-input .q-field__native::placeholder { color: #9ca3af; opacity: 1; }
  .eco-input .q-field__bottom { display: none; }
  .eco-input .q-field__append { height: 48px; padding-right: 12px; }

  .q-btn.login-button,
  .q-btn.login-button.bg-primary {
    width: 100%;
    height: 48px;
    min-height: 48px !important;
    border-radius: 8px !important;
    color: #fff !important;
    background: var(--brand-green-700) !important;
    box-shadow: none !important;
    font-size: 14px !important;
    font-weight: 700 !important;
    text-transform: none !important;
    transition: background .18s ease, transform .08s ease;
  }
  .q-btn.login-button:hover { background: var(--brand-green-800) !important; }
  .login-button:active { transform: translateY(1px); }

  .signup-copy {
    display: flex;
    justify-content: center;
    margin-top: 24px;
    color: var(--text-secondary);
    font-size: 14px;
    line-height: 1.4;
  }
  .q-btn.signup-button,
  .q-btn.signup-button.text-primary {
    min-height: auto !important;
    margin: 0 0 0 5px;
    padding: 0 !important;
    color: var(--brand-green-700) !important;
    background: transparent !important;
    box-shadow: none !important;
    font-size: 14px !important;
    font-weight: 700 !important;
    line-height: 1.4 !important;
    text-decoration: none;
    text-transform: none !important;
  }
  .signup-button:hover { color: var(--brand-green-800) !important; text-decoration: underline; }

  .registration-wrap {
    width: min(560px, 100%);
    transform: translateY(-12px);
  }
  .registration-card .form-title,
  .registration-card .form-subtitle { text-align: center; }
  .registration-card .form-subtitle { margin-bottom: 24px; }
  .registration-form {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px 16px;
    width: 100%;
  }
  .registration-form .field-group { margin: 0; }
  .field-span-2 { grid-column: 1 / -1; }
  .required { color: #b42318; }
  .register-button { margin-top: 4px; }
  .terms-row {
    grid-column: 1 / -1;
    margin-top: -4px;
    color: var(--text-secondary);
    font-size: 12px;
    line-height: 1.4;
  }
  .terms-row .q-checkbox__label { padding-left: 4px; }

  @media (max-width: 1080px) {
    .login-page { grid-template-columns: minmax(390px, 40%) 1fr; }
    .brand-panel { padding-right: 48px; padding-left: 64px; }
    .brand-content { width: 330px; }
    .brand-copy { max-width: 330px; }
    .form-panel { padding: 48px; }
  }

  @media (max-width: 900px) {
    .login-page { display: block; background: var(--brand-green-700); }
    .brand-panel {
      min-height: auto;
      padding: 28px 24px 36px;
    }
    .brand-content { width: min(100%, 432px); margin: 0 auto; }
    .logo-card { width: 112px; height: 112px; margin-bottom: 16px; border-radius: 20px; }
    .brand-title { margin-bottom: 8px; font-size: 32px; }
    .brand-copy { max-width: 360px; font-size: 15px; }
    .benefits { display: none; }
    .form-panel {
      min-height: calc(100vh - 300px);
      align-items: flex-start;
      padding: 32px 24px 56px;
      border-radius: 28px 28px 0 0;
    }
    .form-wrap, .registration-wrap { transform: none; }
  }

  @media (max-width: 480px) {
    .brand-panel { padding: 24px 20px 32px; }
    .logo-card { width: 88px; height: 88px; border-radius: 16px; }
    .brand-title { font-size: 28px; }
    .brand-copy { font-size: 14px; }
    .form-panel { padding: 24px 16px 48px; }
    .login-card { padding: 24px; }
    .form-title { font-size: 22px; }
    .form-subtitle { margin-bottom: 24px; }
    .field-group { margin-bottom: 20px; }
    .field-group.password-group { margin-bottom: 24px; }
    .signup-copy { flex-wrap: wrap; }
    .registration-card { padding: 24px; }
    .registration-form { grid-template-columns: 1fr; gap: 20px; }
    .registration-form .field-span-2,
    .registration-form .terms-row { grid-column: 1; }
  }
</style>
''', shared=True)


def brand_panel() -> None:
    """Renderiza a coluna de marca compartilhada entre login e cadastro."""
    with ui.element('section').classes('brand-panel'):
        with ui.element('div').classes('brand-content'):
            with ui.element('div').classes('logo-card'):
                ui.html(
                    '<img class="brand-logo" src="/assets/ecoflux-logo.jpg" alt="EcoFlux">',
                    sanitize=False,
                )

            ui.html('<h1 class="brand-title">EcoFlux</h1>', sanitize=False)
            ui.html(
                '<p class="brand-copy">Gestão inteligente da coleta seletiva, '
                'com engajamento e impacto mensurável.</p>',
                sanitize=False,
            )

            with ui.element('div').classes('benefits'):
                for icon, title, description in (
                    ('event_available', 'Agende coletas', 'Rotas e pontos integrados'),
                    ('workspace_premium', 'Pontue descartes', 'EcoScore para engajar usuários'),
                    ('verified', 'Comprove resultados', 'EcoSelo e EcoImpacto'),
                ):
                    with ui.element('div').classes('benefit'):
                        with ui.element('div').classes('benefit-icon'):
                            ui.icon(icon)
                        ui.html(
                            f'<div><div class="benefit-title">{title}</div>'
                            f'<div class="benefit-copy">{description}</div></div>',
                            sanitize=False,
                        )


def field_label(text: str, required: bool = True) -> None:
    marker = ' <span class="required">*</span>' if required else ''
    ui.html(f'<div class="field-label">{text}{marker}</div>', sanitize=False)


@ui.page('/')
def login_page() -> None:
    ui.colors(primary='#15703c')
    ui.page_title('EcoFlux | Login')

    def attempt_login() -> None:
        if not email.value or not password.value:
            ui.notify('Preencha o e-mail e a senha.', type='warning')
            return
        ui.notify('Login realizado com sucesso!', type='positive')

    with ui.element('main').classes('login-page'):
        brand_panel()

        with ui.element('section').classes('form-panel'):
            with ui.element('div').classes('form-wrap'):
                with ui.element('div').classes('login-card'):
                    ui.html('<h2 class="form-title">Entrar na plataforma</h2>', sanitize=False)
                    ui.html('<div class="form-subtitle">Acesse sua conta EcoFlux</div>', sanitize=False)

                    with ui.element('div').classes('field-group'):
                        field_label('E-mail', required=False)
                        email = ui.input(placeholder='usuario@instituicao.com').props(
                            'outlined hide-bottom-space type=email autocomplete=username aria-label="E-mail"'
                        ).classes('eco-input')

                    with ui.element('div').classes('field-group password-group'):
                        field_label('Senha', required=False)
                        password = ui.input(
                            placeholder='Digite sua senha',
                            password=True,
                            password_toggle_button=True,
                        ).props(
                            'outlined hide-bottom-space autocomplete=current-password aria-label="Senha"'
                        ).classes('eco-input')
                        password.on('keydown.enter', attempt_login)

                    ui.button('Entrar', on_click=attempt_login).props(
                        'unelevated no-caps'
                    ).classes('login-button')

                with ui.element('div').classes('signup-copy'):
                    ui.label('Não tem conta?')
                    ui.button(
                        'Criar nova conta',
                        on_click=lambda: ui.navigate.to('/cadastro'),
                    ).props('flat dense no-caps').classes('signup-button')


@ui.page('/cadastro')
def registration_page() -> None:
    ui.colors(primary='#15703c')
    ui.page_title('EcoFlux | Cadastro')

    def create_registration() -> None:
        required_fields = (
            name,
            account_type,
            responsible,
            document,
            address,
            email,
            password,
            password_confirmation,
        )
        if any(not field.value for field in required_fields):
            ui.notify('Preencha todos os campos obrigatórios.', type='warning')
            return
        if password.value != password_confirmation.value:
            ui.notify('As senhas informadas não coincidem.', type='negative')
            return
        if len(password.value) < 8:
            ui.notify('A senha deve ter pelo menos 8 caracteres.', type='warning')
            return
        if not terms.value:
            ui.notify('Você precisa aceitar os termos e a política de privacidade.', type='warning')
            return
        ui.notify('Cadastro criado com sucesso!', type='positive')

    with ui.element('main').classes('login-page'):
        brand_panel()

        with ui.element('section').classes('form-panel'):
            with ui.element('div').classes('registration-wrap'):
                with ui.element('div').classes('login-card registration-card'):
                    ui.html('<h2 class="form-title">Cadastro</h2>', sanitize=False)
                    ui.html(
                        '<div class="form-subtitle">Usuário, cooperativa, empresa ou condomínio</div>',
                        sanitize=False,
                    )

                    with ui.element('div').classes('registration-form'):
                        with ui.element('div').classes('field-group'):
                            field_label('Nome')
                            name = ui.input(placeholder='Cond. Parque Verde').props(
                                'outlined hide-bottom-space autocomplete=organization aria-label="Nome"'
                            ).classes('eco-input')

                        with ui.element('div').classes('field-group'):
                            field_label('Tipo')
                            account_type = ui.select(
                                ['Usuário', 'Cooperativa', 'Empresa', 'Condomínio'],
                                value='Condomínio',
                            ).props(
                                'outlined hide-bottom-space options-dense aria-label="Tipo"'
                            ).classes('eco-input')

                        with ui.element('div').classes('field-group'):
                            field_label('Responsável')
                            responsible = ui.input(placeholder='Luca Soares').props(
                                'outlined hide-bottom-space autocomplete=name aria-label="Responsável"'
                            ).classes('eco-input')

                        with ui.element('div').classes('field-group'):
                            field_label('CNPJ/CPF')
                            document = ui.input(placeholder='00.000.000/0001-00').props(
                                'outlined hide-bottom-space inputmode=numeric aria-label="CNPJ ou CPF"'
                            ).classes('eco-input')

                        with ui.element('div').classes('field-group field-span-2'):
                            field_label('Endereço')
                            address = ui.input(
                                placeholder='Rua das Araucárias, 48, Centro — Porto Seguro — 45810-000'
                            ).props(
                                'outlined hide-bottom-space autocomplete=street-address aria-label="Endereço"'
                            ).classes('eco-input')

                        with ui.element('div').classes('field-group field-span-2'):
                            field_label('E-mail')
                            email = ui.input(placeholder='usuario@instituicao.com').props(
                                'outlined hide-bottom-space type=email autocomplete=email aria-label="E-mail"'
                            ).classes('eco-input')

                        with ui.element('div').classes('field-group'):
                            field_label('Senha')
                            password = ui.input(
                                placeholder='Mínimo de 8 caracteres',
                                password=True,
                                password_toggle_button=True,
                            ).props(
                                'outlined hide-bottom-space autocomplete=new-password aria-label="Senha"'
                            ).classes('eco-input')

                        with ui.element('div').classes('field-group'):
                            field_label('Confirmar senha')
                            password_confirmation = ui.input(
                                placeholder='Repita sua senha',
                                password=True,
                                password_toggle_button=True,
                            ).props(
                                'outlined hide-bottom-space autocomplete=new-password aria-label="Confirmar senha"'
                            ).classes('eco-input')
                            password_confirmation.on('keydown.enter', create_registration)

                        terms = ui.checkbox(
                            'Li e aceito os Termos de Uso e a Política de Privacidade (LGPD).'
                        ).classes('terms-row')

                        ui.button(
                            'Criar cadastro',
                            on_click=create_registration,
                        ).props('unelevated no-caps').classes(
                            'login-button register-button field-span-2'
                        )

                with ui.element('div').classes('signup-copy'):
                    ui.label('Já tem uma conta?')
                    ui.button(
                        'Entrar',
                        on_click=lambda: ui.navigate.to('/'),
                    ).props('flat dense no-caps').classes('signup-button')


ui.run(title='EcoFlux | Login', port=8080, reload=False, show=False, favicon='🌿')
