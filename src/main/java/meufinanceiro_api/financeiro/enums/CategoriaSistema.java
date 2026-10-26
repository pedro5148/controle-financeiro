package meufinanceiro_api.financeiro.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoriaSistema {
    AJUSTE_SALDO("Ajuste de Saldo"),
    PAGAMENTO_FATURA("Pagamento de Fatura");

    private final String nomePadrao;
}

