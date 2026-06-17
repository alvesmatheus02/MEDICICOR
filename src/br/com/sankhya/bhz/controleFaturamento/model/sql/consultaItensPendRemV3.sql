WITH
-- Base: itens pendentes de retorno
PENDENTES AS (
    SELECT
        R.NUNOTA,
        R.SEQUENCIA,
        R.CODPARC,
        R.CODEMP,
        R.CODPROD,
        R.VLRUNIT,
        R.CODVOL,
        R.QTDNEG,
        R.QTDRETORNO,
        R.CONTROLE,
        R.QTDNEG - R.QTDRETORNO QTDPENDRET,
        R.CODLOCALORIG,
        R.CODLOCALTERC,
        ROW_NUMBER() OVER (
                            PARTITION BY CODEMP, CODPROD, CONTROLE, CODPARC
                            ORDER BY NUNOTA, SEQUENCIA
                        ) AS ORD_NOTA
    FROM (
             SELECT
                 IREM.NUNOTA,
                 IREM.SEQUENCIA,
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

             UNION ALL

             SELECT
                 ITE.NUNOTA NUNOTAREM,
                 ITE.SEQUENCIA SEQUENCIAREM,
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

    ) R
    WHERE R.QTDNEG - R.QTDRETORNO > 0
),

-- Estoque disponível por local, ordenado por prioridade
ESTOQUES AS (
    SELECT
        EST.CODEMP,
        EST.CODPROD,
        EST.CODLOCAL,
        EST.CONTROLE,
        EST.CODPARC,
        EST.ESTOQUE - EST.RESERVADO AS ESTOQUE_DISPONIVEL,
        -- Prioridade: local preferencial da nota vem primeiro
        -- Como cada nota pode ter CODLOCALTERC diferente, 
        -- usamos a prioridade global: ordena por local
        ROW_NUMBER() OVER (
            PARTITION BY EST.CODEMP, EST.CODPROD, EST.CONTROLE, EST.CODPARC
            ORDER BY EST.CODLOCAL  -- ajuste aqui se tiver outra regra de prioridade
        ) AS PRIORIDADE,
        -- Soma acumulada de estoque disponível até este local (acumulado de saldo)
        SUM(EST.ESTOQUE - EST.RESERVADO) OVER (
            PARTITION BY EST.CODEMP, EST.CODPROD, EST.CONTROLE, EST.CODPARC
            ORDER BY EST.CODLOCAL
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS ESTOQUE_ACUM,
        -- Acumulado ANTERIOR (saldo já "coberto" pelos locais anteriores)
        NVL(SUM(EST.ESTOQUE - EST.RESERVADO) OVER (
            PARTITION BY EST.CODEMP, EST.CODPROD, EST.CONTROLE, EST.CODPARC
            ORDER BY EST.CODLOCAL
            ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
        ), 0) AS ESTOQUE_ACUM_ANTERIOR,
        -- Total de estoque disponível (todos os locais somados)
        SUM(EST.ESTOQUE - EST.RESERVADO) OVER (
            PARTITION BY EST.CODEMP, EST.CODPROD, EST.CONTROLE, EST.CODPARC
        ) AS ESTOQUE_TOTAL
    FROM TGFEST EST
    WHERE EST.ESTOQUE - EST.RESERVADO > 0
      AND EST.TIPO      = 'P'
      AND EST.CODPARC  != 0
    AND EST.CODLOCAL NOT IN (1400)
    ),

-- Acumulado de demanda entre notas (quanto já foi "prometido" às notas anteriores)
    PENDENTES_ACUM AS (
SELECT
    P.*,
    -- Soma acumulada da QTDPENDRET das notas ANTERIORES (exclusive a atual)
    NVL(SUM(P.QTDPENDRET) OVER (
    PARTITION BY P.CODEMP, P.CODPROD, P.CONTROLE, P.CODPARC
    ORDER BY P.ORD_NOTA
    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
    ), 0) AS DEMANDA_ACUM_ANTERIOR,
    -- Soma acumulada incluindo a nota atual
    SUM(P.QTDPENDRET) OVER (
    PARTITION BY P.CODEMP, P.CODPROD, P.CONTROLE, P.CODPARC
    ORDER BY P.ORD_NOTA
    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS DEMANDA_ACUM_ATUAL
FROM PENDENTES P
    ),

-- Cruzamento: para cada nota x cada local, calcula quanto consome
    CONSUMO_BRUTO AS (
SELECT
    PA.NUNOTA,
    PA.SEQUENCIA,
    PA.CODPARC,
    PA.CODEMP,
    PA.CODPROD,
    PA.VLRUNIT,
    PA.CODVOL,
    PA.QTDNEG,
    PA.QTDRETORNO,
    PA.CONTROLE,
    PA.QTDPENDRET,
    PA.CODLOCALORIG,
    PA.CODLOCALTERC,
    PA.ORD_NOTA,
    PA.DEMANDA_ACUM_ANTERIOR,
    PA.DEMANDA_ACUM_ATUAL,
    E.CODLOCAL                  AS CODLOCAL_CONSUMO,
    E.PRIORIDADE                AS PRIORIDADE_CONSUMO,
    E.ESTOQUE_DISPONIVEL        AS ESTOQUE_DISP_LOCAL,
    E.ESTOQUE_ACUM,
    E.ESTOQUE_ACUM_ANTERIOR,
    E.ESTOQUE_TOTAL,
    /*
     * Lógica de consumo por local, considerando demanda acumulada de notas anteriores:
     *
     * O "espaço" disponível neste local para esta nota é:
     *   INÍCIO do intervalo desta nota neste local:
     *     MAX(ESTOQUE_ACUM_ANTERIOR, DEMANDA_ACUM_ANTERIOR)
     *     → começa de onde já foi consumido (por locais anteriores OU por notas anteriores)
     *   FIM do intervalo disponível deste local:
     *     MIN(ESTOQUE_ACUM, DEMANDA_ACUM_ATUAL)
     *     → até onde este local vai OU até onde esta nota precisa
     *   CONSUMO = GREATEST(FIM - INÍCIO, 0)
     */
    GREATEST(
    LEAST(E.ESTOQUE_ACUM, PA.DEMANDA_ACUM_ATUAL)
    - GREATEST(E.ESTOQUE_ACUM_ANTERIOR, PA.DEMANDA_ACUM_ANTERIOR),
    0
    ) AS QTD_CONSUMO_LOCAL
FROM PENDENTES_ACUM PA
    -- LEFT JOIN para manter notas sem estoque
    LEFT JOIN ESTOQUES E
ON  E.CODEMP   = PA.CODEMP
    AND E.CODPROD  = PA.CODPROD
    AND E.CONTROLE = PA.CONTROLE
    AND E.CODPARC  = PA.CODPARC
    ),

-- Resultado final
    DISTR_FIN AS (
SELECT
    NUNOTA,
    SEQUENCIA,
    CODPARC,
    CODEMP,
    CODPROD,
    VLRUNIT,
    CODVOL,
    QTDNEG,
    QTDRETORNO,
    CONTROLE,
    QTDPENDRET,
    CODLOCALORIG,
    CODLOCALTERC,
    CODLOCAL_CONSUMO,
    PRIORIDADE_CONSUMO,
    ESTOQUE_DISP_LOCAL,
    QTD_CONSUMO_LOCAL,
    -- Quanto ainda ficou sem atendimento (pendente restante)
    GREATEST(QTDPENDRET - SUM(QTD_CONSUMO_LOCAL) OVER (
    PARTITION BY NUNOTA, SEQUENCIA, CODEMP, CODPROD, CONTROLE, CODPARC
    ), 0) AS QTDPEND_RESTANTE
FROM CONSUMO_BRUTO
WHERE QTD_CONSUMO_LOCAL > 0
   OR CODLOCAL_CONSUMO IS NULL  -- sem estoque: mantém linha visível
ORDER BY
    NUNOTA,
    SEQUENCIA,
    PRIORIDADE_CONSUMO)


SELECT
    NUNOTA,
    SEQUENCIA,
    CODPARC,
    CODEMP,
    CODPROD,
    VLRUNIT,
    CODVOL,
    QTDNEG,
    QTDRETORNO,
    CONTROLE,
    QTDPENDRET,
    CODLOCALORIG,
    CODLOCALTERC,
    CODLOCAL_CONSUMO,
    SUM(QTD_CONSUMO_LOCAL) QTD_CONSUMO_LOCAL
FROM DISTR_FIN

GROUP BY
    NUNOTA,
    SEQUENCIA,
    CODPARC,
    CODEMP,
    CODPROD,
    VLRUNIT,
    CODVOL,
    QTDNEG,
    QTDRETORNO,
    CONTROLE,
    QTDPENDRET,
    CODLOCALORIG,
    CODLOCALTERC,
    CODLOCAL_CONSUMO