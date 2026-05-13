package br.com.sankhya.bhz.centrais.regras;

import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ContextoRegra;
import br.com.sankhya.modelcore.comercial.Regra;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class regraValDocAnexo implements Regra {
    JapeWrapper anexoDAO = JapeFactory.dao("ContainerCTe");
    JapeWrapper cadDocDAO = JapeFactory.dao("AD_BHZCADDOC");
    JapeWrapper topDocDAO = JapeFactory.dao("AD_BHZTOPDOC");

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
        List<String> documentos = new ArrayList<String>();

        if(tgfCab) {
            boolean confirmando = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.FALSE);

            if (confirmando) {

                EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
                JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
                NativeSql sql = new NativeSql(jdbc);

                sql.loadSql(regraValDocAnexo.class, "sql/consultaDocAnexo.sql");
                sql.setNamedParameter("NUNOTA", nuNota);
                ResultSet resultSet = sql.executeQuery();

                while (resultSet.next()) {
                    String documento = resultSet.getString("DOCUMENTO");
                    documentos.add(documento);
                }

                if (!documentos.isEmpty()) {

                    StringBuilder msg = new StringBuilder("Lançamento possui pendencia de anexo dos seguintes documentos: ");

                    for (String documento : documentos) {
                        if (msg.charAt(msg.length() - 1) != ' ') {
                            msg.append(", ");
                        }
                        msg.append(documento);
                    }
                    ErroUtils.disparaErro(msg + ".<br><br> Favor revisar lançamento!");
                }
            }
        }
    }

    @Override
    public void afterDelete(ContextoRegra ctx) throws Exception {

    }
}
