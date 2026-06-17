package br.com.sankhya.bhz;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.sql.NativeSql;

public class ControlaTipoNeg implements EventoProgramavelJava {

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {

        DynamicVO cab = (DynamicVO) event.getVo();

        BigDecimal nunota = cab.asBigDecimal("NUNOTA");
        BigDecimal codParc = cab.asBigDecimal("CODPARC");
        BigDecimal codConvenio = cab.asBigDecimal("AD_CODCONVENIO");

        if (codParc == null) {
            return;
        }

        JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
        jdbc.openSession();

        try {

            BigDecimal codTipVenda = null;

            NativeSql sql = new NativeSql(jdbc);

            /* -----------------------------------
               1 - Tenta buscar na AD_BHZTN
               ----------------------------------- */

            if (codConvenio != null) {

                sql.appendSql(
                        "SELECT CODTIPVENDA " +
                                "FROM AD_BHZTN " +
                                "WHERE CODPARC = :CODPARC " +
                                "AND CODCONVENIO = :CODCONVENIO"
                );

                sql.setNamedParameter("CODPARC", codParc);
                sql.setNamedParameter("CODCONVENIO", codConvenio);

                ResultSet rs = sql.executeQuery();

                if (rs.next()) {
                    codTipVenda = rs.getBigDecimal("CODTIPVENDA");
                }

                rs.close();

            }

            /* -----------------------------------
               2 - Se não encontrou, busca TGFCPL
               ----------------------------------- */

            if (codTipVenda == null) {

                sql = new NativeSql(jdbc);

                sql.appendSql(
                        "SELECT SUGTIPNEGSAID " +
                                "FROM TGFCPL " +
                                "WHERE CODPARC = :CODPARC "+
                                "AND SUGTIPNEGSAID > 0"
                );

                sql.setNamedParameter("CODPARC", codParc);

                ResultSet rs = sql.executeQuery();

                if (rs.next()) {
                    codTipVenda = rs.getBigDecimal("SUGTIPNEGSAID");
                }

                rs.close();

            }

            /* -----------------------------------
               3 - Se ainda for null, não faz nada
               ----------------------------------- */

            if (codTipVenda == null) {
                return;
            }

            /* -----------------------------------
               4 - Atualiza TGFCAB
               ----------------------------------- */

            sql = new NativeSql(jdbc);

            sql.appendSql(
                    "UPDATE TGFCAB CAB " +
                            "SET CAB.CODTIPVENDA = :CODTIPVENDA, " +
                            "CAB.DHTIPVENDA = ( " +
                            "   SELECT MAX(DHALTER) " +
                            "   FROM TGFTPV " +
                            "   WHERE CODTIPVENDA = :CODTIPVENDA " +
                            ") " +
                            "WHERE CAB.NUNOTA = :NUNOTA"
            );

            sql.setNamedParameter("CODTIPVENDA", codTipVenda);
            sql.setNamedParameter("NUNOTA", nunota);

            sql.executeUpdate();

        } finally {
            jdbc.closeSession();
        }
    }

    @Override public void beforeInsert(PersistenceEvent event) throws Exception {}
    @Override public void beforeUpdate(PersistenceEvent event) throws Exception {}
    @Override public void beforeDelete(PersistenceEvent event) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent event) throws Exception {}
    @Override public void afterDelete(PersistenceEvent event) throws Exception {}
    @Override public void beforeCommit(TransactionContext tranCtx) throws Exception {}
}