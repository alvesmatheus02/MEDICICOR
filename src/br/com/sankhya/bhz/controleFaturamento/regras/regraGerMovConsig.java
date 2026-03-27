package br.com.sankhya.bhz.controleFaturamento.regras;

import br.com.sankhya.bhz.controleFaturamento.model.gerMov;
import br.com.sankhya.bhz.utils.Utilitarios;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ContextoRegra;
import br.com.sankhya.modelcore.comercial.Regra;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;

public class regraGerMovConsig implements Regra {
    JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
    JapeWrapper tpoDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);

    @Override
    public void beforeInsert(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void beforeUpdate(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void beforeDelete(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void afterInsert(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void afterUpdate(ContextoRegra ctx) throws Exception {

        DynamicVO cabVO = ctx.getPrePersistEntityState().getNewVO();

        boolean tgfCab = "CabecalhoNota".equals(ctx.getPrePersistEntityState().getDao().getEntityName());
        BigDecimal nuNota = cabVO.asBigDecimalOrZero("NUNOTA");

        if(tgfCab) {
            boolean confirmando = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.FALSE);

            if(confirmando){

                EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
                JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
                NativeSql sql = new NativeSql(jdbc);

                sql.loadSql(gerMov.class, "sql/consMovAuto.sql");
                sql.setNamedParameter("NUNOTA", nuNota);
                ResultSet resultSet = sql.executeQuery();

                if (resultSet.next()) {

                    BigDecimal nuNotaMod = resultSet.getBigDecimal("NUNOTAMOD");
                    BigDecimal codLocalDest = resultSet.getBigDecimal("CODLOCALDEST");
                    BigDecimal codTipPoperDest = resultSet.getBigDecimal("CODTIPOPERDEST");
                    String gerConf = resultSet.getString("GERACONF");
                    String  tipMovAuto = resultSet.getString("TIPMOVAUTO");

                    DynamicVO tpoVO = tpoDAO.findByPK(codTipPoperDest, Utilitarios.getDataMaxTipoOper(codTipPoperDest));

                    gerMov.geraCabecalho(nuNotaMod, tpoVO, cabVO, gerConf, tipMovAuto, codLocalDest, null, "N");
                }
            }
        }

    }

    @Override
    public void afterDelete(ContextoRegra ctx) throws Exception {

    }
}
