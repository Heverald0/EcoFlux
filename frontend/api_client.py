from __future__ import annotations

from dataclasses import dataclass
from typing import Any
import base64
import json
import httpx

API_BASE_URL = 'http://localhost:8080'


class ApiError(Exception):
    """Erro retornado pela API ou falha de conexão com o back-end."""

    def __init__(self, message: str, status_code: int | None = None) -> None:
        super().__init__(message)
        self.message = message
        self.status_code = status_code


@dataclass(frozen=True)
class UsuarioCriado:
    id: int
    nome: str
    email: str
    perfil: str
    data_criacao: str


@dataclass(frozen=True)
class SessaoAuth:
    token: str
    tipo: str
    expira_em_segundos: int


def auth_headers(token: str) -> dict[str, str]:
    """Monta o cabeçalho Authorization Bearer para endpoints protegidos."""
    return {'Authorization': f'Bearer {token}'}


def _extrair_erro(response: httpx.Response, fallback: str) -> str:
    try:
        body = response.json()
        if isinstance(body, dict):
            for chave in ('message', 'mensagem', 'error', 'detail'):
                valor = body.get(chave)
                if valor:
                    return str(valor)
    except ValueError:
        texto = response.text.strip()
        if texto:
            return texto
    return fallback


def _normalizar_perfil(perfil: str | None) -> str | None:
    if not perfil or not str(perfil).strip():
        return None
    limpo = str(perfil).strip().upper()
    if limpo.startswith('ROLE_'):
        limpo = limpo[5:]
    return limpo or None


def _extrair_perfil_jwt(token: str) -> str | None:
    try:
        partes = token.split('.')
        if len(partes) < 2:
            return None
        payload_b64 = partes[1] + '=' * (-len(partes[1]) % 4)
        dados = json.loads(
            base64.urlsafe_b64decode(payload_b64.encode('utf-8')).decode('utf-8')
        )
        if isinstance(dados, dict):
            valor = dados.get('perfil') or dados.get('role') or dados.get('authorities')
            if isinstance(valor, list) and valor:
                valor = valor[0]
            if isinstance(valor, str):
                return _normalizar_perfil(valor)
    except Exception:
        return None
    return None


def cadastrar_usuario(
    nome: str,
    email: str,
    senha: str,
    *,
    documento: str | None = None,
    telefone: str | None = None,
    perfil: str | None = None,
) -> UsuarioCriado:
    """Envia POST /api/v1/usuarios para o Spring Boot."""
    payload: dict[str, Any] = {
        'nome': nome.strip(),
        'email': email.strip().lower(),
        'senha': senha,
    }
    if documento and documento.strip():
        payload['documento'] = documento.strip()[:20]
    if telefone and telefone.strip():
        payload['telefone'] = telefone.strip()[:20]
    if perfil and perfil.strip():
        payload['perfil'] = perfil.strip().upper()

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            response = client.post('/api/v1/usuarios', json=payload)
    except httpx.RequestError as exc:
        raise ApiError(
            'Não foi possível conectar ao back-end em http://localhost:8080. '
            'Confira se o Spring Boot está rodando.'
        ) from exc

    if response.status_code not in (200, 201):
        raise ApiError(
            _extrair_erro(response, f'Erro ao cadastrar usuário (HTTP {response.status_code}).'),
            status_code=response.status_code,
        )

    data = response.json()
    return UsuarioCriado(
        id=data['id'],
        nome=data['nome'],
        email=data['email'],
        perfil=str(data.get('perfil', '')),
        data_criacao=str(data.get('dataCriacao', '')),
    )


def fazer_login(email: str, senha: str) -> SessaoAuth:
    """Envia POST /api/v1/auth/login e retorna os dados do token JWT."""
    payload = {
        'email': email.strip().lower(),
        'senha': senha,
    }

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            response = client.post('/api/v1/auth/login', json=payload)
    except httpx.RequestError as exc:
        raise ApiError(
            'Não foi possível conectar ao back-end em http://localhost:8080. '
            'Confira se o Spring Boot está rodando.'
        ) from exc

    detalhe = _extrair_erro(response, '').strip()
    detalhe_lower = detalhe.lower()

    if response.status_code == 403 or any(
        p in detalhe_lower
        for p in ('desabilit', 'aguardando', 'análise', 'analise', 'aprovação', 'aprovacao')
    ):
        raise ApiError(
            detalhe or 'Sua conta está aguardando aprovação para liberar o acesso.',
            status_code=response.status_code,
        )

    if response.status_code == 401:
        raise ApiError('E-mail ou senha inválidos.', status_code=401)

    if response.status_code != 200:
        raise ApiError(
            detalhe or f'Falha ao autenticar (HTTP {response.status_code}).',
            status_code=response.status_code,
        )

    data = response.json()
    token = str(data.get('token') or '').strip()
    if not token:
        raise ApiError('A API não retornou um token JWT válido.', status_code=response.status_code)

    return SessaoAuth(
        token=token,
        tipo=str(data.get('tipo') or 'Bearer'),
        expira_em_segundos=int(data.get('expiraEmSegundos') or 0),
    )


SESSAO_ATUAL: dict[str, str | None] = {
    'token': None,
    'email': None,
    'perfil': None,
    'nome': None,
}


def buscar_perfil_atual(token: str | None = None) -> dict[str, Any]:
    """GET /api/v1/usuarios/me para obter os dados e perfil do usuário autenticado."""
    tok = token or obter_token()
    if not tok:
        raise ApiError('Sessão expirada ou não autenticada. Faça login novamente.', status_code=401)

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get('/api/v1/usuarios/me', headers=auth_headers(tok))
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao consultar perfil atual (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, dict) else {}


def definir_sessao(token: str, email: str, perfil: str | None = None) -> None:
    email_limpo = email.strip().lower()
    SESSAO_ATUAL['token'] = token
    SESSAO_ATUAL['email'] = email_limpo
    SESSAO_ATUAL['nome'] = None

    perfil_resolvido = _normalizar_perfil(perfil) or _extrair_perfil_jwt(token)

    try:
        me = buscar_perfil_atual(token)
        perfil_me = _normalizar_perfil(me.get('perfil'))
        if perfil_me:
            perfil_resolvido = perfil_me
        if me.get('nome'):
            SESSAO_ATUAL['nome'] = str(me['nome']).strip()
        if me.get('email'):
            SESSAO_ATUAL['email'] = str(me['email']).strip().lower()
    except ApiError:
        pass

    SESSAO_ATUAL['perfil'] = perfil_resolvido


def limpar_sessao() -> None:
    SESSAO_ATUAL['token'] = None
    SESSAO_ATUAL['email'] = None
    SESSAO_ATUAL['perfil'] = None
    SESSAO_ATUAL['nome'] = None


def obter_token() -> str | None:
    return SESSAO_ATUAL['token']


def obter_email_atual() -> str | None:
    return SESSAO_ATUAL['email']


def obter_nome_atual() -> str | None:
    nome = SESSAO_ATUAL.get('nome')
    if nome and str(nome).strip():
        return str(nome).strip()
    token = obter_token()
    if token:
        try:
            me = buscar_perfil_atual(token)
            if me.get('nome'):
                nome_me = str(me['nome']).strip()
                SESSAO_ATUAL['nome'] = nome_me
                return nome_me
        except ApiError:
            pass
    email = obter_email_atual()
    if email:
        return email.split('@')[0]
    return None


def obter_perfil_atual() -> str | None:
    perfil = _normalizar_perfil(SESSAO_ATUAL.get('perfil'))
    if perfil:
        return perfil
    token = obter_token()
    if token:
        perfil_jwt = _extrair_perfil_jwt(token)
        if perfil_jwt:
            SESSAO_ATUAL['perfil'] = perfil_jwt
            return perfil_jwt
        try:
            me = buscar_perfil_atual(token)
            perfil_me = _normalizar_perfil(me.get('perfil'))
            if perfil_me:
                SESSAO_ATUAL['perfil'] = perfil_me
                return perfil_me
        except ApiError:
            pass
    return None


def eh_administrador() -> bool:
    return obter_perfil_atual() == 'ADMINISTRADOR'


def _auth_headers_obrigatorio() -> dict[str, str]:
    token = obter_token()
    if not token:
        raise ApiError('Sessão expirada ou não autenticada. Faça login novamente.', status_code=401)
    return auth_headers(token)


def listar_materiais() -> list[dict[str, Any]]:
    """GET /api/v1/materiais para listar os tipos de materiais disponíveis."""
    headers = auth_headers(obter_token() or '') if obter_token() else None
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get('/api/v1/materiais', headers=headers)
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar materiais (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def buscar_material_por_codigo(codigo: str) -> dict[str, Any]:
    """GET /api/v1/materiais/{codigo} para buscar um material pelo código."""
    cod_limpo = (codigo or '').strip().upper()
    headers = auth_headers(obter_token() or '') if obter_token() else None
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(f'/api/v1/materiais/{cod_limpo}', headers=headers)
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao buscar material (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, dict) else {}


def listar_usuarios(status: str | None = None) -> list[dict[str, Any]]:
    params: dict[str, str] = {}
    if status and status.strip():
        params['status'] = status.strip().upper()

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/usuarios',
                params=params or None,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar usuários (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def atualizar_status_usuario(
    usuario_id: int,
    novo_status: str,
    motivo: str | None = None,
) -> dict[str, Any]:
    """PATCH /api/v1/usuarios/{id}/status."""
    payload: dict[str, Any] = {'status': novo_status.strip().upper()}
    if motivo and motivo.strip():
        payload['motivo'] = motivo.strip()[:255]

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/usuarios/{usuario_id}/status',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao atualizar status do usuário (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def listar_pontos_coleta(
    *,
    status: str | None = None,
    bairro: str | None = None,
    material: str | None = None,
) -> list[dict[str, Any]]:
    """Lista pontos de coleta em GET /api/v1/pontos-coleta."""
    params: dict[str, str] = {}
    if status:
        params['status'] = status.strip()
    if bairro:
        params['bairro'] = bairro.strip()
    if material:
        params['material'] = material.strip()

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            response = client.get(
                '/api/v1/pontos-coleta',
                params=params or None,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(
            'Não foi possível conectar ao back-end em http://localhost:8080.'
        ) from exc

    if response.status_code != 200:
        raise ApiError(
            _extrair_erro(response, f'Erro ao listar pontos de coleta (HTTP {response.status_code}).'),
            status_code=response.status_code,
        )

    dados = response.json()
    return dados if isinstance(dados, list) else []


def criar_ponto_coleta(payload: dict[str, Any]) -> dict[str, Any]:
    """Cadastra ponto de coleta em POST /api/v1/pontos-coleta."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            response = client.post(
                '/api/v1/pontos-coleta',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(
            'Não foi possível conectar ao back-end em http://localhost:8080.'
        ) from exc

    if response.status_code not in (200, 201):
        raise ApiError(
            _extrair_erro(response, f'Erro ao cadastrar ponto (HTTP {response.status_code}).'),
            status_code=response.status_code,
        )

    return response.json()


def atualizar_ponto_coleta(ponto_id: int, payload: dict[str, Any]) -> dict[str, Any]:
    """PUT /api/v1/pontos-coleta/{id}."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.put(
                f'/api/v1/pontos-coleta/{ponto_id}',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao atualizar ponto de coleta (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def alterar_status_ponto_coleta(
    ponto_id: int,
    status: str,
    motivo: str | None = None,
) -> dict[str, Any]:
    """PATCH /api/v1/pontos-coleta/{id}/status."""
    payload: dict[str, Any] = {'status': status}
    if motivo and motivo.strip():
        payload['motivo'] = motivo.strip()[:255]

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/pontos-coleta/{ponto_id}/status',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao alterar status do ponto (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def agendar_coleta(payload: dict[str, Any]) -> dict[str, Any]:
    """POST /api/v1/coletas."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.post(
                '/api/v1/coletas',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code not in (200, 201):
        raise ApiError(
            _extrair_erro(resp, f'Erro ao agendar coleta (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def buscar_coleta_por_id(coleta_id: int) -> dict[str, Any]:
    """GET /api/v1/coletas/{id}."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                f'/api/v1/coletas/{coleta_id}',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao buscar coleta (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, dict) else {}


def listar_minhas_coletas(status: str | None = None) -> list[dict[str, Any]]:
    """GET /api/v1/coletas/minhas."""
    params: dict[str, str] = {}
    if status and status.strip():
        params['status'] = status.strip().upper()

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/coletas/minhas',
                params=params or None,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar coletas (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def listar_coletas_operacional(status: str | None = None) -> list[dict[str, Any]]:
    """GET /api/v1/coletas/operacional."""
    params: dict[str, str] = {}
    if status and status.strip():
        params['status'] = status.strip().upper()

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/coletas/operacional',
                params=params or None,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar coletas operacionais (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def buscar_timeline_coleta(coleta_id: int) -> list[dict[str, Any]]:
    """GET /api/v1/coletas/{id}/timeline."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                f'/api/v1/coletas/{coleta_id}/timeline',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao consultar timeline da coleta (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def atualizar_status_coleta(
    coleta_id: int,
    novo_status: str,
    observacao: str | None = None,
    cooperativa_id: int | None = None,
) -> dict[str, Any]:
    """PATCH /api/v1/coletas/{id}/status."""
    payload: dict[str, Any] = {'novoStatus': novo_status}
    if observacao and observacao.strip():
        payload['observacao'] = observacao.strip()[:255]
    if cooperativa_id is not None:
        payload['cooperativaId'] = cooperativa_id

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/coletas/{coleta_id}/status',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao atualizar status da coleta (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def cancelar_coleta(coleta_id: int) -> dict[str, Any]:
    """PATCH /api/v1/coletas/{id}/cancelar."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/coletas/{coleta_id}/cancelar',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao cancelar coleta (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def listar_minhas_instituicoes() -> list[dict[str, Any]]:
    """GET /api/v1/instituicoes/minhas."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/instituicoes/minhas',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar instituições (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def listar_instituicoes(
    *,
    tipo: str | None = None,
    status: str | None = None,
) -> list[dict[str, Any]]:
    """GET /api/v1/instituicoes (opcionalmente filtrado por tipo e status)."""
    params: dict[str, str] = {}
    if tipo and tipo.strip():
        params['tipo'] = tipo.strip().upper()
    if status and status.strip():
        params['status'] = status.strip().upper()

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/instituicoes',
                params=params or None,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar instituições (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def atualizar_status_instituicao(
    instituicao_id: int,
    novo_status: str,
    motivo: str | None = None,
) -> dict[str, Any]:
    """PATCH /api/v1/instituicoes/{id}/status."""
    payload: dict[str, Any] = {'status': novo_status.strip().upper()}
    if motivo and motivo.strip():
        payload['motivo'] = motivo.strip()[:255]

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/instituicoes/{instituicao_id}/status',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao atualizar status da instituição (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def listar_cooperativas(status: str | None = None) -> list[dict[str, Any]]:
    """GET /api/v1/cooperativas (opcionalmente filtrado por status)."""
    params: dict[str, str] = {}
    if status and status.strip():
        params['status'] = status.strip().upper()

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/cooperativas',
                params=params or None,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar cooperativas (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def atualizar_status_cooperativa(
    cooperativa_id: int,
    novo_status: str,
    motivo: str | None = None,
) -> dict[str, Any]:
    """PATCH /api/v1/cooperativas/{id}/status."""
    payload: dict[str, Any] = {'status': novo_status.strip().upper()}
    if motivo and motivo.strip():
        payload['motivo'] = motivo.strip()[:255]

    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/cooperativas/{cooperativa_id}/status',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao atualizar status da cooperativa (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def cadastrar_instituicao(payload: dict[str, Any]) -> dict[str, Any]:
    """POST /api/v1/instituicoes."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.post(
                '/api/v1/instituicoes',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code not in (200, 201):
        raise ApiError(
            _extrair_erro(resp, f'Erro ao cadastrar instituição (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def buscar_instituicao_por_id(instituicao_id: int) -> dict[str, Any]:
    """GET /api/v1/instituicoes/{id}."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                f'/api/v1/instituicoes/{instituicao_id}',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao buscar instituição (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def buscar_ecoimpacto_global() -> dict[str, Any]:
    """GET /api/v1/ecoimpacto para obter o impacto ambiental."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/ecoimpacto',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao consultar EcoImpacto global (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, dict) else {}


def buscar_meu_ecoimpacto() -> dict[str, Any]:
    """GET /api/v1/ecoimpacto/meu para obter o impacto do usuário autenticado."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/ecoimpacto/meu',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao consultar meu EcoImpacto (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, dict) else {}


def listar_meus_ecoselos() -> list[dict[str, Any]]:
    """GET /api/v1/ecoselos/meus para obter os selos do usuário autenticado."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/ecoselos/meus',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar meus EcoSelos (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def listar_ecoselos() -> list[dict[str, Any]]:
    """GET /api/v1/ecoselos para obter selos/catálogo."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/ecoselos',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar EcoSelos (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def buscar_meu_ecoscore() -> dict[str, Any]:
    """GET /api/v1/ecoscore/meu para obter a pontuação do usuário autenticado."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/ecoscore/meu',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao consultar meu EcoScore (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, dict) else {}


def listar_ranking_ecoscore() -> list[dict[str, Any]]:
    """GET /api/v1/ecoscore/ranking para obter o ranking geral de EcoScore."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/ecoscore/ranking',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao consultar ranking EcoScore (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []

def atualizar_sessao_atual() -> dict[str, Any]:
    """Reconsulta GET /api/v1/usuarios/me e atualiza perfil/nome/email na sessao."""
    token = obter_token()
    if not token:
        return {}
    try:
        me = buscar_perfil_atual(token)
    except ApiError:
        return {}

    perfil_me = _normalizar_perfil(me.get('perfil'))
    if perfil_me:
        SESSAO_ATUAL['perfil'] = perfil_me
    if me.get('nome'):
        SESSAO_ATUAL['nome'] = str(me['nome']).strip()
    if me.get('email'):
        SESSAO_ATUAL['email'] = str(me['email']).strip().lower()
    return me


def vincular_membro_instituicao(
    instituicao_id: int,
    cpf: str,
    email: str,
    papel: str,
) -> dict[str, Any]:
    """POST /api/v1/instituicoes/{instituicaoId}/membros."""
    payload = {
        'cpf': cpf.strip(),
        'email': email.strip().lower(),
        'papel': papel.strip().upper(),
    }
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.post(
                f'/api/v1/instituicoes/{instituicao_id}/membros',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code not in (200, 201):
        raise ApiError(
            _extrair_erro(resp, f'Erro ao vincular membro (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def listar_membros_instituicao(instituicao_id: int) -> list[dict[str, Any]]:
    """GET /api/v1/instituicoes/{instituicaoId}/membros."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                f'/api/v1/instituicoes/{instituicao_id}/membros',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar membros (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def desvincular_membro_instituicao(instituicao_id: int, membro_id: int) -> None:
    """DELETE /api/v1/instituicoes/{instituicaoId}/membros/{membroId}."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.delete(
                f'/api/v1/instituicoes/{instituicao_id}/membros/{membro_id}',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code not in (200, 204):
        raise ApiError(
            _extrair_erro(resp, f'Erro ao desvincular membro (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )


def listar_meus_vinculos_instituicao() -> list[dict[str, Any]]:
    """GET /api/v1/instituicoes/meus-vinculos."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/instituicoes/meus-vinculos',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar vínculos (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def listar_minha_rota_motorista() -> list[dict[str, Any]]:
    """GET /api/v1/coletas/minha-rota."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/coletas/minha-rota',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar minha rota (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    dados = resp.json()
    return dados if isinstance(dados, list) else []


def atribuir_motorista_coleta(coleta_id: int, motorista_id: int) -> dict[str, Any]:
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/coletas/{coleta_id}/atribuir-motorista',
                params={'motoristaId': motorista_id},
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao atribuir motorista (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()


def concluir_entrega_motorista(coleta_id: int, payload: dict[str, Any]) -> dict[str, Any]:
    """PATCH /api/v1/coletas/{id}/concluir-entrega."""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.patch(
                f'/api/v1/coletas/{coleta_id}/concluir-entrega',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao concluir entrega (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )

    return resp.json()

def listar_artigos_educativos(categoria: str | None = None) -> list[dict[str, Any]]:
    """GET /api/v1/educativo/artigos"""
    params = {'categoria': categoria} if categoria else None
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/educativo/artigos',
                params=params,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar artigos educativos (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )
    dados = resp.json()
    return dados if isinstance(dados, list) else []


def buscar_artigo_por_slug(slug: str) -> dict[str, Any]:
    """GET /api/v1/educativo/artigos/{slug}"""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                f'/api/v1/educativo/artigos/{slug}',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao buscar artigo "{slug}" (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )
    dados = resp.json()
    return dados if isinstance(dados, dict) else {}


def listar_guia_triagem() -> list[dict[str, Any]]:
    """GET /api/v1/educativo/guia-triagem"""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.get(
                '/api/v1/educativo/guia-triagem',
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code != 200:
        raise ApiError(
            _extrair_erro(resp, f'Erro ao listar guia de triagem (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )
    dados = resp.json()
    return dados if isinstance(dados, list) else []


def criar_artigo_educativo(payload: dict[str, Any]) -> dict[str, Any]:
    """POST /api/v1/educativo/artigos"""
    try:
        with httpx.Client(base_url=API_BASE_URL, timeout=10.0) as client:
            resp = client.post(
                '/api/v1/educativo/artigos',
                json=payload,
                headers=_auth_headers_obrigatorio(),
            )
    except httpx.RequestError as exc:
        raise ApiError(f'Falha de conexão com o back-end em {API_BASE_URL}.') from exc

    if resp.status_code not in (200, 201):
        raise ApiError(
            _extrair_erro(resp, f'Erro ao criar artigo (HTTP {resp.status_code}).'),
            status_code=resp.status_code,
        )
    dados = resp.json()
    return dados if isinstance(dados, dict) else {}