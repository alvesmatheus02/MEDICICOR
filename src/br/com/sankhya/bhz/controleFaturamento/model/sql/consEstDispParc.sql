SELECT *
FROM (
         SELECT
             EST.CODPROD,
             EST.CONTROLE,
             EST.CODLOCAL,
             EST.CODEMP,
             EST.CODPARC,
             EST.ESTOQUE,
             EST.RESERVADO,
             EST.ESTOQUE - EST.RESERVADO                              AS ESTOQUEDISPONIVEL,
             SUM(EST.ESTOQUE - EST.RESERVADO) OVER (
            PARTITION BY EST.CODEMP, EST.CODPROD, EST.CODPARC, EST.CONTROLE
            ORDER BY EST.CODLOCAL
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        )                                                        AS ACUMULADO,
             SUM(EST.ESTOQUE - EST.RESERVADO) OVER (
            PARTITION BY EST.CODEMP, EST.CODPROD, EST.CODPARC, EST.CONTROLE
        )                                                        AS TOTAL_DISPONIVEL,
             :QTD                                                     AS QTD_NECESSARIA
         FROM TGFEST EST
         WHERE EST.ESTOQUE > 0
           AND EST.CODPARC > 0
           AND EST.TIPO = 'P'
           AND EST.CODEMP    = :EMP
           AND EST.CODPROD   = :PROD
           AND EST.CODPARC   = :PARC
           AND EST.CONTROLE  = :CONTROLE
           AND EST.ESTOQUE - EST.RESERVADO > 0
           AND EST.CODLOCAL NOT IN (1400)
     )
WHERE ACUMULADO - ESTOQUEDISPONIVEL < :QTD
ORDER BY CODLOCAL