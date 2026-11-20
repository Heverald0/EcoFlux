from __future__ import annotations

from dataclasses import dataclass
from typing import Any
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


def cadastrar_usuario(nome: str, email: str, senha: str) -> UsuarioCriado:
    """Envia POST /api/v1/usuarios para o Spring Boot."""
    payload = {
        'nome': nome.strip(),
        'email': email.strip().lower(),
        'senha': senha,
    }

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

    if response.status_code == 401:
        raise ApiError('E-mail ou senha inválidos.', status_code=401)

    if response.status_code != 200:
        raise ApiError(
            _extrair_erro(response, f'Falha ao autenticar (HTTP {response.status_code}).'),
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
}


def definir_sessao(token: str, email: str) -> None:
    SESSAO_ATUAL['token'] = token
    SESSAO_ATUAL['email'] = email.strip().lower()


def obter_token() -> str | None:
    return SESSAO_ATUAL['token']


def _auth_headers_obrigatorio() -> dict[str, str]:
    token = obter_token()
    if not token:
        raise ApiError('Sessão expirada ou não autenticada. Faça login novamente.', status_code=401)
    return auth_headers(token)


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


def listar_minhas_coletas(status: str | None = None) -> list[dict[str, Any]]:
    """GET /api/v1/coletas/minhas."""
    params: dict[str, str] = {}
    if status and status.strip():
        params['status'] = status.strip()

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