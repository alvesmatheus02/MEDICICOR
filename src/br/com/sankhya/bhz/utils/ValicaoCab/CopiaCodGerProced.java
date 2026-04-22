package br.com.sankhya.bhz.utils.ValicaoCab;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class CopiaCodGerProced implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
    }

    private void atualizar(PersistenceEvent event) throws Exception {

        DynamicVO cabVO = (DynamicVO) event.getVo();

        BigDecimal nunota = cabVO.asBigDecimal("NUNOTA");
        BigDecimal codProced = cabVO.asBigDecimal("AD_CODPROCED");
        BigDecimal codRegGerAtual = cabVO.asBigDecimal("AD_CODREGGER");

        if (nunota == null || codProced == null)
            return;

        // 👉 Se já tem valor, não mexe (permite alteração manual)
        if (codRegGerAtual != null)
            return;

        JapeWrapper tipoProcedDAO = JapeFactory.dao("AD_TIPOPROCED");
        DynamicVO tipoProcedVO = tipoProcedDAO.findOne("CODPROCED = ?", codProced);

        if (tipoProcedVO == null)
            return;

        BigDecimal codGer = tipoProcedVO.asBigDecimal("CODGER");

        if (codGer == null)
            return;

        JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
        cabDAO.prepareToUpdateByPK(nunota)
                .set("AD_CODREGGER", codGer)
                .update();
    }
    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        atualizar(event);
    }
    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {
        atualizar(event);
    }
    @Override public void beforeDelete(PersistenceEvent event) {}
    @Override public void afterDelete(PersistenceEvent event) {}

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {

    }
}