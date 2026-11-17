package com.example.EcoFlux.relatorios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.text.Normalizer;
import java.util.*;

@Component @RequiredArgsConstructor
public class RelatorioPdf {
    private final ObjectMapper mapper;

    public byte[] gerar(Relatorio relatorio) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Escrita escrita = new Escrita(documento, relatorio);
            escrita.pagina();
            escrita.titulo(relatorio.getNome());
            escrita.linha("Periodo: " + relatorio.getInicio() + " a " + relatorio.getFim(), false);
            escrita.linha("Gerado em: " + relatorio.getGeradoEm().toString().replace('T', ' '), false);
            escrita.linha("Tipo: " + relatorio.getTipo() + " | Relatorio " + relatorio.getId(), false);
            escrita.espaco();
            JsonNode dados = mapper.readTree(relatorio.getDados());
            escrita.linha("Instituicao: " + dados.path("instituicao").asText(), true);
            for (String campo : List.of("total", "resumo", "score", "certificados", "registros")) {
                if (dados.has(campo)) escrita.bloco(campo, dados.get(campo), 0);
            }
            escrita.fechar();
            documento.getDocumentInformation().setTitle(relatorio.getNome());
            documento.getDocumentInformation().setAuthor("EcoFlux");
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Nao foi possivel gerar o PDF.", ex);
        }
    }

    private static final class Escrita {
        private final PDDocument documento;
        private final Relatorio relatorio;
        private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream fluxo;
        private float y;
        private int numero;

        Escrita(PDDocument documento, Relatorio relatorio) { this.documento = documento; this.relatorio = relatorio; }

        void pagina() throws IOException {
            if (fluxo != null) fluxo.close();
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            fluxo = new PDPageContentStream(documento, pagina);
            numero++;
            texto("ECOFLUX", negrito, 16, 44, 797);
            texto("Relatorio " + relatorio.getId(), normal, 9, 440, 800);
            fluxo.setStrokingColor(new java.awt.Color(34, 139, 68));
            fluxo.moveTo(44, 784); fluxo.lineTo(551, 784); fluxo.stroke();
            texto("EcoFlux | Dados consolidados na data de geracao", normal, 8, 44, 30);
            texto("Pagina " + numero, normal, 8, 500, 30);
            y = 760;
        }

        void texto(String texto, PDFont fonte, float tamanho, float x, float altura) throws IOException {
            fluxo.beginText(); fluxo.setFont(fonte, tamanho); fluxo.newLineAtOffset(x, altura);
            fluxo.showText(limpar(texto)); fluxo.endText();
        }

        void titulo(String titulo) throws IOException { linha(titulo, true); espaco(); }
        void espaco() { y -= 10; }
        void fechar() throws IOException { if (fluxo != null) fluxo.close(); }

        void linha(String valor, boolean forte) throws IOException {
            PDFont fonte = forte ? negrito : normal;
            String restante = limpar(valor).replaceAll("\\s+", " ").trim();
            if (restante.isEmpty()) return;
            while (!restante.isEmpty()) {
                int fim = restante.length();
                while (fim > 1 && fonte.getStringWidth(restante.substring(0, fim)) * 10 / 1000 > 507) fim--;
                if (fim < restante.length()) {
                    int quebra = restante.lastIndexOf(' ', fim);
                    if (quebra > 0) fim = quebra;
                }
                if (y < 58) pagina();
                texto(restante.substring(0, fim), fonte, 10, 44, y);
                y -= 14;
                restante = restante.substring(fim).stripLeading();
            }
        }

        void bloco(String nome, JsonNode valor, int nivel) throws IOException {
            if (valor.isNull()) return;
            if (valor.isValueNode()) { linha(rotulo(nome) + ": " + valor.asText(), false); return; }
            if (y < (valor.isArray() ? 145 : 100)) pagina();
            espaco(); linha(rotulo(nome), true);
            if (valor.isArray()) {
                if (valor.isEmpty()) linha("Nenhum registro no periodo.", false);
                else if (nome.equals("registros")) tabelaRegistros(valor);
                else if (nome.equals("evolucao")) tabelaMensal(valor);
                else {
                    int i = 0;
                    for (JsonNode item : valor) {
                        i++;
                        if (item.isValueNode()) linha(item.asText(), false);
                        else {
                            linha("Registro " + i, true);
                            var campos = item.fields();
                            while (campos.hasNext()) { var campo = campos.next(); bloco(campo.getKey(), campo.getValue(), nivel + 1); }
                            espaco();
                        }
                    }
                }
            } else {
                var campos = valor.fields();
                while (campos.hasNext()) { var campo = campos.next(); bloco(campo.getKey(), campo.getValue(), nivel + 1); }
            }
        }

        void tabelaRegistros(JsonNode registros) throws IOException {
            cabecalhoTabela();
            for (JsonNode registro : registros) {
                if (y < 88) { pagina(); cabecalhoTabela(); }
                String material = registro.path("material").asText(registro.path("tipoMaterial").asText());
                String quantidade = registro.has("quantidade") ? registro.get("quantidade").asText() : registro.path("quantidadeReal").asText("-");
                if (quantidade.equals("null")) quantidade = "-";
                quantidade += " " + registro.path("unidade").asText("KG");
                texto(registro.path("id").asText(), normal, 9, 44, y);
                texto(material, normal, 9, 102, y);
                texto(quantidade, normal, 9, 255, y);
                texto(registro.path("status").asText(), normal, 9, 410, y);
                y -= 17;
                String data = registro.path("data").asText(registro.path("dataHoraAgendamento").asText());
                linha("Data: " + data + (registro.hasNonNull("quantidadeEstimadaKg")
                        ? " | Quantidade estimada: " + registro.get("quantidadeEstimadaKg").asText() + " " + registro.path("unidade").asText() : ""), false);
                if (registro.hasNonNull("origem")) linha("Origem: " + registro.get("origem").asText() + " | Destino: " + registro.path("destino").asText(), false);
                if (registro.hasNonNull("volume")) linha("Volume: " + registro.get("volume").asText(), false);
                espaco();
            }
        }

        void cabecalhoTabela() throws IOException {
            texto("ID", negrito, 9, 44, y);
            texto("Material", negrito, 9, 102, y);
            texto("Quantidade real", negrito, 9, 255, y);
            texto("Status", negrito, 9, 410, y);
            y -= 20;
        }

        void tabelaMensal(JsonNode meses) throws IOException {
            for (int i = 0; i < meses.size(); i++) {
                if (i == 0 || y < 78) {
                    if (y < 78) pagina();
                    String[] titulos = {"Mes", "kg", "L", "CO2 (kg)", "Agua (L)", "Energia (kWh)", "Arvores"};
                    float[] posicoes = {44, 110, 170, 225, 300, 380, 480};
                    for (int c = 0; c < titulos.length; c++) texto(titulos[c], negrito, 8, posicoes[c], y);
                    y -= 19;
                }
                JsonNode mes = meses.get(i);
                String[] valores = {mes.path("mes").asText(), mes.path("quantidades").path("KG").asText(),
                        mes.path("quantidades").path("L").asText(), mes.path("impacto").path("co2Kg").asText(),
                        mes.path("impacto").path("aguaL").asText(), mes.path("impacto").path("energiaKwh").asText(),
                        mes.path("impacto").path("arvores").asText()};
                float[] posicoes = {44, 110, 170, 225, 300, 380, 480};
                for (int c = 0; c < valores.length; c++) {
                    String valor = valores[c];
                    if (c > 0) valor = new java.math.BigDecimal(valor).stripTrailingZeros().toPlainString();
                    float largura = c == 6 ? 71 : posicoes[c + 1] - posicoes[c] - 6;
                    float tamanho = Math.min(8, largura * 1000 / Math.max(1, normal.getStringWidth(limpar(valor))));
                    texto(valor, normal, tamanho, posicoes[c], y);
                }
                y -= 17;
            }
        }

        String limpar(String texto) {
            StringBuilder resultado = new StringBuilder();
            texto.codePoints().forEach(cp -> {
                String caractere = new String(Character.toChars(cp));
                try { normal.encode(caractere); resultado.append(caractere); }
                catch (Exception ex) {
                    String simples = Normalizer.normalize(caractere, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                    resultado.append(simples.codePoints().allMatch(c -> c >= 32 && c < 127) ? simples : " ");
                }
            });
            return resultado.toString();
        }

        String rotulo(String nome) {
            return switch (nome) {
                case "co2Kg" -> "CO2 evitado (kg)";
                case "aguaL" -> "Agua economizada (L)";
                case "energiaKwh" -> "Energia poupada (kWh)";
                case "arvores" -> "Arvores preservadas";
                case "registrosSemFator" -> "Registros sem fator de impacto cadastrado";
                case "quantidades" -> "Quantidades por unidade";
                case "regraVersao" -> "Versao da regra";
                default -> { String s = nome.replaceAll("([a-z])([A-Z])", "$1 $2"); yield s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1); }
            };
        }
    }
}
