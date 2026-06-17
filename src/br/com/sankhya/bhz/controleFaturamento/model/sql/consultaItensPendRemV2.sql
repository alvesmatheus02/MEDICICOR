WITH
-- BASE: saldo pendente de retorno por remessa
PENDENTE AS (
    SELECT
        R.CODPARC,
        R.CODEMP,
        R.CODPROD,
        R.CONTROLE,
        R.CODVOL,
        R.CODLOCALTERC,                      -- local preferencial
        SUM(R.QTDNEG - R.QTDRETORNO) AS QTDPENDRET_TOTAL
    FROM (
             SELECT
                 IREM.NUNOTA NUNOTAREM,
                 IREM.SEQUENCIA SEQUENCIAREM,
                 CREM.CODPARC,
                 IREM.CODEMP,
                 IREM.CODPROD,
                 IREM.VLRUNIT,
                 PRO.CODVOL,
                 IREM.QTDNEG,
                 SUM(NVL(VAR.QTDATENDIDA,0)) QTDRETORNO,
                 IREM.CONTROLE,
                 IREM.CODLOCALORIG,
                 IREM.CODLOCALTERC
             FROM TGFCAB CREM
                      INNER JOIN TGFITE IREM ON IREM.NUNOTA = CREM.NUNOTA
                      INNER JOIN TGFPRO PRO ON PRO.CODPROD = IREM.CODPROD
                      LEFT JOIN TGFVAR VAR ON VAR.NUNOTAORIG = CREM.NUNOTA AND VAR.SEQUENCIAORIG = IREM.SEQUENCIA

             WHERE CREM.NUNOTA IN (SELECT DISTINCT V.NUNOTAORIG FROM TGFCAB C
                                                                         INNER JOIN TGFVAR V ON V.NUNOTA = C.NUNOTA

                                   WHERE C.TIPMOV = 'D'
                                     AND C.NUNOTA = :NUNOTA)

             GROUP BY IREM.NUNOTA,
                      IREM.SEQUENCIA,
                      CREM.CODPARC,
                      IREM.CODEMP,
                      IREM.CODPROD,
                      IREM.VLRUNIT,
                      PRO.CODVOL,
                      IREM.QTDNEG,
                      IREM.CONTROLE,
                      IREM.CODLOCALORIG,
                      IREM.CODLOCALTERC

             HAVING SUM(IREM.QTDNEG - NVL(VAR.QTDATENDIDA,0)) > 0

             UNION ALL

             SELECT
                 ITE.NUNOTA,
                 ITE.SEQUENCIA,
                 CAB.CODPARC,
                 ITE.CODEMP,
                 ITE.CODPROD,
                 ITE.VLRUNIT,
                 PRO.CODVOL,
                 ITE.QTDNEG,
                 SUM(NVL(VAR.QTDATENDIDA,0)) QTDRETORNO,
                 ITE.CONTROLE,
                 ITE.CODLOCALORIG,
                 ITE.CODLOCALTERC
             FROM TGFCAB CAB
                      INNER JOIN TGFITE ITE ON ITE.NUNOTA = CAB.NUNOTA
                      INNER JOIN TGFPRO PRO ON PRO.CODPROD = ITE.CODPROD
                      LEFT JOIN TGFVAR VAR ON VAR.NUNOTAORIG = CAB.NUNOTA AND VAR.SEQUENCIAORIG = ITE.SEQUENCIA

             WHERE CAB.TIPMOV = 'V'
               AND CAB.NUNOTA = :NUNOTA

             GROUP BY ITE.NUNOTA,
                      ITE.SEQUENCIA,
                      CAB.CODPARC,
                      ITE.CODEMP,
                      ITE.CODPROD,
                      ITE.VLRUNIT,
                      PRO.CODVOL,
                      ITE.QTDNEG,
                      ITE.CONTROLE,
                      ITE.CODLOCALORIG,
                      ITE.CODLOCALTERC
             HAVING SUM(ITE.QTDNEG - NVL(VAR.QTDATENDIDA,0)) > 0) R

    GROUP BY
        R.CODPARC,
        R.CODEMP,
        R.CODPROD,
        R.CONTROLE,
        R.CODVOL,
        R.CODLOCALTERC
),

-- BASE: saldo disponível por local na TGFEST
ESTOQUE AS (
    SELECT
        EST.CODEMP,
        EST.CODPROD,
        EST.CODLOCAL,
        EST.CONTROLE,
        EST.CODPARC,
        EST.ESTOQUE - EST.RESERVADO AS ESTOQUE_DISPONIVEL
    FROM TGFEST EST
    WHERE EST.ESTOQUE - EST.RESERVADO > 0
      AND EST.TIPO    = 'P'
      AND EST.CODPARC != 0
    AND EST.CODLOCAL NOT IN (1400)
    ),

-- CRUZAMENTO: une pendente com todos os locais disponíveis
-- ordenando local preferencial primeiro, depois demais por maior saldo
    CRUZADO AS (
SELECT
    P.CODPARC,
    P.CODEMP,
    P.CODPROD,
    P.CONTROLE,
    P.CODVOL,
    P.QTDPENDRET_TOTAL,
    P.CODLOCALTERC                          AS LOCAL_PREFERENCIAL,
    E.CODLOCAL,
    E.ESTOQUE_DISPONIVEL,
    -- Ordena local preferencial = 1, demais por maior saldo
    ROW_NUMBER() OVER (
    PARTITION BY P.CODPARC, P.CODEMP, P.CODPROD, P.CONTROLE
    ORDER BY
    CASE WHEN E.CODLOCAL = P.CODLOCALTERC THEN 0 ELSE 1 END,
    E.ESTOQUE_DISPONIVEL DESC
    ) AS ORDEM_LOCAL
FROM PENDENTE P
    JOIN ESTOQUE E
ON  E.CODPARC  = P.CODPARC
    AND E.CODEMP   = P.CODEMP
    AND E.CODPROD  = P.CODPROD
    AND E.CONTROLE = P.CONTROLE
    ),

-- ACUMULADO: soma de saldo acumulado por ordem de local
    ACUMULADO AS (
SELECT
    C.*,
    -- Saldo acumulado ATÉ o local anterior (exclusive)
    SUM(C.ESTOQUE_DISPONIVEL) OVER (
    PARTITION BY C.CODPARC, C.CODEMP, C.CODPROD, C.CONTROLE
    ORDER BY C.ORDEM_LOCAL
    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
    ) AS SALDO_ACUM_ANTERIOR
FROM CRUZADO C
    )

-- RESULTADO FINAL: calcula quanto consome em cada local
SELECT
    A.CODPARC,
    A.CODEMP,
    A.CODPROD,
    A.CONTROLE,
    A.CODVOL,
    A.QTDPENDRET_TOTAL, /*Total que precisa ser consumido*/
    A.LOCAL_PREFERENCIAL,
    A.CODLOCAL, /*Local de onde será consumido*/
    A.ORDEM_LOCAL,
    A.ESTOQUE_DISPONIVEL, /*Saldo disponível naquele local*/
    -- Quanto já foi "comprometido" pelos locais anteriores
    NVL(A.SALDO_ACUM_ANTERIOR, 0)                                   AS SALDO_CONSUMIDO_ANTES,
    -- Quanto ainda falta consumir chegando neste local
    GREATEST(A.QTDPENDRET_TOTAL - NVL(A.SALDO_ACUM_ANTERIOR, 0), 0) AS QTDE_AINDA_NECESSARIA,
    -- Quanto efetivamente consome neste local
    LEAST(
            A.ESTOQUE_DISPONIVEL,
            GREATEST(A.QTDPENDRET_TOTAL - NVL(A.SALDO_ACUM_ANTERIOR, 0), 0)
    )                                                                AS QTDE_A_CONSUMIR, /*Quanto efetivamente consome neste local*/
    -- Flag de local preferencial
    CASE WHEN A.CODLOCAL = A.LOCAL_PREFERENCIAL THEN 'S' ELSE 'N' END AS FL_LOCAL_PREF, /*se for o local preferencial da remessa*/
    -- Status do atendimento nesta linha
    CASE
        WHEN LEAST(
                     A.ESTOQUE_DISPONIVEL,
                     GREATEST(A.QTDPENDRET_TOTAL - NVL(A.SALDO_ACUM_ANTERIOR, 0), 0)
             ) = 0
            THEN 'SEM_CONSUMO'         -- local não foi necessário
        WHEN NVL(A.SALDO_ACUM_ANTERIOR, 0) + A.ESTOQUE_DISPONIVEL >= A.QTDPENDRET_TOTAL
            THEN 'ATENDIDO_COMPLETO'   -- pendente zerado neste local
        ELSE 'ATENDIDO_PARCIAL'    -- ainda sobra pendente após este local
        END                                                              AS STATUS_ATENDIMENTO /*Se o pendente foi totalmente ou parcialmente atendido*/
FROM ACUMULADO A
WHERE
    -- Remove locais que não participaram do consumo
    GREATEST(A.QTDPENDRET_TOTAL - NVL(A.SALDO_ACUM_ANTERIOR, 0), 0) > 0
ORDER BY
    A.CODPARC,
    A.CODPROD,
    A.CONTROLE,
    A.ORDEM_LOCAL