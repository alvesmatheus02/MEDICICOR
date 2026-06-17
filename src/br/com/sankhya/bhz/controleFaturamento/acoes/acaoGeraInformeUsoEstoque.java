package br.com.sankhya.bhz.controleFaturamento.acoes;

import br.com.sankhya.bhz.controleFaturamento.model.gerMov;
import br.com.sankhya.bhz.controleFaturamento.model.valAjusteEstTerceiro;
import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.bhz.utils.Utilitarios;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;

public class acaoGeraInformeUsoEstoque implements AcaoRotinaJava {
    JapeWrapper estDAO = JapeFactory.dao(DynamicEntityNames.ESTOQUE);
    JapeWrapper tpoDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
    JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
    JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();

        if (linhas.length < 1){
            ErroUtils.disparaErro("Selecione ao menos uma linha para ação!");
        }

        BigDecimal nuNotaMov = null;

        for(Registro linha : linhas) {

            BigDecimal codEmp = (BigDecimal) linha.getCampo("CODEMP");
            BigDecimal codProd = (BigDecimal) linha.getCampo("CODPROD");
            BigDecimal codLocal = (BigDecimal) linha.getCampo("CODLOCAL");
            String controle = (String) linha.getCampo("CONTROLE");
            BigDecimal codParc = (BigDecimal) linha.getCampo("CODPARC");
            String tipo = (String) linha.getCampo("TIPO");
            BigDecimal qtdEst = BigDecimal.ZERO;

            DynamicVO estVO = estDAO.findByPK(codEmp, codProd, codLocal, controle, codParc, tipo);
            DynamicVO tpoVO = tpoDAO.findByPK(BigDecimal.valueOf(2282), Utilitarios.getDataMaxTipoOper(BigDecimal.valueOf(2282)));


            if (null != estVO && null != tpoVO) {

                EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
                JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
                NativeSql sql = new NativeSql(jdbc);
                sql.loadSql(gerMov.class, "sql/consModInformeUso.sql");
                sql.setNamedParameter("CODEMP", codEmp);
                ResultSet resultSet = sql.executeQuery();

                if (resultSet.next()) {

                    BigDecimal nuNotaMod = resultSet.getBigDecimal("NUNOTAMOD");

                    DynamicVO cabVO = cabDAO.findByPK(nuNotaMod);

                    if (null == nuNotaMov && null != cabVO) {
                        nuNotaMov = gerMov.geraCabecalho(nuNotaMod, tpoVO, cabVO, "N", "XX", BigDecimal.valueOf(1400), null, "S", estVO);
                    } else {
                        DynamicVO cabMov = cabDAO.findByPK(nuNotaMov);
                        gerMov.insertItens(cabMov, tpoVO, nuNotaMod, "XX", BigDecimal.valueOf(1400), estVO.asBigDecimalOrZero("CODLOCAL"), "S", estVO);
                    }
                } else {
                    ErroUtils.disparaErro("Modelo para geração do informe de uso não identificado, favor revisar o parâmetros BHZTPOINFUSO e os  Modelos Tipo de Operação.");
                }
            }
        }

        Collection<DynamicVO> iteVO = iteDAO.find("NUNOTA = ?", nuNotaMov);
        valAjusteEstTerceiro.validaEstoqueTerceiro(iteVO);
    }
}
