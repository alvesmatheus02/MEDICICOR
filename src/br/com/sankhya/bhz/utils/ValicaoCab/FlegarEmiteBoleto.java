package br.com.sankhya.bhz.utils.ValicaoCab;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class FlegarEmiteBoleto implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {

        DynamicVO cab = (DynamicVO) event.getVo();

        String tipMov = cab.asString("TIPMOV");

        // Só para Pedido e Venda
        if (!"P".equals(tipMov) && !"V".equals(tipMov)) {
            return;
        }

        if (cab.asBigDecimal("CODPARC") == null) {
            return;
        }

        JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");

        DynamicVO parceiro = parceiroDAO.findByPK(
                cab.asBigDecimal("CODPARC")
        );

        if (parceiro == null) {
            return;
        }

        String tipoGerBoleto = parceiro.asString("TIPOGERBOLETO");

        if (tipoGerBoleto != null && !tipoGerBoleto.trim().isEmpty()) {
            cab.setProperty("AD_EMIT_BOLETO", "SIM");
        }
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {

    }
}
