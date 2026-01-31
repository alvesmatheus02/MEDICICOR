SELECT
    BHZ_OBTEMCUSTO(PRO.CODPROD, --AQUI VAI O CODIGO DO PRODUTO
                (SELECT LOGICO FROM TSIPAR WHERE CHAVE = 'CUSTOPOREMP' AND CODUSU = 0),
                41, --AQUI VAI CODIGO DA EMPRESA
                (SELECT LOGICO FROM TSIPAR WHERE CHAVE = 'CUSTOPORLOC' AND CODUSU = 0),
                0, --AQUI VAI O CODIGO DO LOCAL CASO EMPRESA TRABALHA COM CUSTO POR LOCAL
                (SELECT LOGICO FROM TSIPAR WHERE CHAVE = 'CUSTOPORCONT' AND CODUSU = 0),
                ' ', --AQUI VAI O CODIGO DO LOCAL CASO EMPRESA TRABALHA COM CUSTO POR LOTE
                TRUNC(:DTMOV), --AQUI VAI A DATA DE MOVIMENTAÇÃO
                CASE WHEN :USAPRECOCUSTO = 'E' THEN 6
                     WHEN :USAPRECOCUSTO = 'G' THEN 7
                     WHEN :USAPRECOCUSTO = 'L' THEN 1
                     WHEN :USAPRECOCUSTO = 'M' THEN 4
                     WHEN :USAPRECOCUSTO = 'R' THEN 0
                     WHEN :USAPRECOCUSTO = 'S' THEN 5
                     WHEN :USAPRECOCUSTO = 'V' THEN 2
                     WHEN :USAPRECOCUSTO = 'Z' THEN 3
                    ELSE 999 END -- TIPO DE CUSTO 0 - REPOSIÇÃO  / 1 CUSTO GERENCIAL / 2 CUSTO VARIAVEL / 3 CUSTO MED SEM ICSM / 4 CUSTO MED COM ICMS / 5 ENTRADA SEM ICMS / 6 ENTRADA COM ICMS / 7 CUSTO MED GERENCIAL
    ) VLRCUSTO
FROM TGFPRO PRO

WHERE CODPROD = :CODPROD

/*
D = Preço em Moeda
E = Último Custo de Entrada Com ICMS - OK
G = Último Custo Médio Gerencial - OK
L = Último Custo Gerencial - OK
M = Último Custo Médio Com ICMS - OK
N = Nenhum - NÃO IMPLEMENTADO
O = Média das notas de origem - NÃO IMPLEMENTADO
P = Preço de Venda - NÃO IMPLEMENTADO
Q = Valor Líquido da origem - NÃO IMPLEMENTADO
R = Último Custo de Reposição -OK
S = Último Custo de Entrada Sem ICMS - OK
V = Último Custo Variável - OK
Z = Último Custo Médio Sem ICMS - OK
*/