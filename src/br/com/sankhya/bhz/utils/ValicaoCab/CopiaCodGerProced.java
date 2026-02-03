package br.com.sankhya.bhz.utils.ValicaoCab;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.wrapper.JapeFactory;

public class CopiaCodGerProced implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {

        DynamicVO vo = (DynamicVO) event.getVo();

        String tipMov = vo.asString("TIPMOV");

        if (!"V".equals(tipMov) && !"P".equals(tipMov)) {
            return;
        }

        BigDecimal codProced = vo.asBigDecimal("AD_CODPROCED");

        if (codProced == null) {
            return;
        }

        JdbcWrapper jdbc = null;
        NativeSql sql = null;
        ResultSet rs = null;

        try {
            jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
            jdbc.openSession();

            sql = new NativeSql(jdbc);

            sql.appendSql(
                    "SELECT CODGER " +
                            "FROM AD_TIPOPROCED " +
                            "WHERE AD_CODPROCED = :CODPROCED"
            );

            sql.setNamedParameter("CODPROCED", codProced);

            rs = sql.executeQuery();

            if (rs.next()) {
                BigDecimal codGer = rs.getBigDecimal("CODGER");
                vo.setProperty("AD_CODREGGER", codGer);
            }

        } finally {
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
        }
    }

    @Override public void beforeUpdate(PersistenceEvent event) {}
    @Override public void afterInsert(PersistenceEvent event) {}
    @Override public void afterUpdate(PersistenceEvent event) {}
    @Override public void beforeDelete(PersistenceEvent event) {}
    @Override public void afterDelete(PersistenceEvent event) {}

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {

    }
}
