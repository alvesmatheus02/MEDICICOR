WITH SALDOS AS (
    SELECT
        ITE.NUNOTA,
        ITE.SEQUENCIA,
        ITE.CODPROD,
        ITE.CONTROLE,
        ITE.VLRUNIT,
        ITE.CODVOL,
        CAB.DTENTSAI,
        (ITE.QTDNEG - ITE.QTDENTREGUE)                         AS QTDDISP,
        SUM(ITE.QTDNEG - ITE.QTDENTREGUE)
                                                                  OVER (ORDER BY CAB.DTENTSAI, ITE.NUNOTA, ITE.SEQUENCIA
                  ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS ACUMULADO
    FROM TGFCAB CAB
             INNER JOIN TGFITE ITE ON ITE.NUNOTA = CAB.NUNOTA
    WHERE ITE.ATUALESTTERC = 'P'
      AND ITE.TERCEIROS    = 'S'
      AND (ITE.QTDNEG - ITE.QTDENTREGUE) > 0
      AND ITE.CODEMP   = :CODEMP
      AND ITE.CODPROD  = :CODPROD
      AND ITE.CONTROLE = :CONTROLE
      AND CAB.CODPARC  = :CODPARC

    ORDER BY CAB.DTENTSAI
),
     PARAMS AS (
         SELECT :QTD AS QTDE_CONSUMIR FROM DUAL  -- quantidade a distribuir
     )
SELECT
    S.NUNOTA,
    S.SEQUENCIA,
    S.CODPROD,
    S.CONTROLE,
    S.VLRUNIT,
    S.CODVOL,
    S.DTENTSAI,
    S.QTDDISP,
    S.ACUMULADO,
    -- Quanto desta linha é efetivamente consumido
    GREATEST(
            0,
            LEAST(S.QTDDISP, P.QTDE_CONSUMIR - (S.ACUMULADO - S.QTDDISP))
    )                                      AS QTDE_CONSUMIDA,
    -- Saldo que sobra nesta linha após o consumo
    S.QTDDISP - GREATEST(
            0,
            LEAST(S.QTDDISP, P.QTDE_CONSUMIR - (S.ACUMULADO - S.QTDDISP))
                )                                      AS SALDO_REMANESCENTE
FROM SALDOS S
         CROSS JOIN PARAMS P
WHERE (S.ACUMULADO - S.QTDDISP) < P.QTDE_CONSUMIR  -- apenas linhas que participam
ORDER BY S.DTENTSAI, S.NUNOTA, S.SEQUENCIA