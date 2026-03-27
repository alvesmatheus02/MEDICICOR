package br.com.sankhya.bhz.controleFaturamento.regras;

import br.com.sankhya.bhz.controleFaturamento.model.valAjusteEstTerceiro;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ContextoRegra;
import br.com.sankhya.modelcore.comercial.Regra;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

import java.math.BigDecimal;
import java.util.Collection;

public class regraAjusteEstTerc implements Regra {
    JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
    JapeWrapper tpoDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
    JapeWrapper tpoMovAutoDAO = JapeFactory.dao("AD_BHZAUTOMOV");

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
            DynamicVO tpoVO = tpoDAO.findByPK(cabVO.asBigDecimalOrZero("CODTIPOPER"), cabVO.asTimestamp("DHTIPOPER"));
            DynamicVO tpoMovAutoVO = tpoMovAutoDAO.findOne("CODTIPOPERDEST = ?", cabVO.asBigDecimalOrZero("CODTIPOPER"));

            boolean confirmando = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.FALSE);

            if (confirmando && null != tpoMovAutoVO && !tpoVO.asString("ATUALESTTERC").equals("N")) {
                Collection<DynamicVO> iteVO = iteDAO.find("NUNOTA = ?", nuNota);
                valAjusteEstTerceiro.validaEstoqueTerceiro(iteVO);
            }
        }
    }

    @Override
    public void afterDelete(ContextoRegra ctx) throws Exception {

    }
}
