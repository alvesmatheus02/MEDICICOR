package br.com.sankhya.bhz.utils.ValicaoCab;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

import java.sql.Timestamp;

public class ValidaDTProcedimentoAgendado implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {

        DynamicVO vo = (DynamicVO) event.getVo();

        String status = vo.asString("AD_STATUSCOTACAO");
        Timestamp dtProced = vo.asTimestamp("AD_DTPROCED");

        // INSERT já com status 4
        if ("4".equals(status) && dtProced == null) {
            throw new Exception(
                    "Para definir o Status da Cotação como 'Agendado', o campo Data do Procedimento é obrigatório."
            );
        }
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {

        DynamicVO vo = (DynamicVO) event.getVo();
        DynamicVO voOld = (DynamicVO) event.getOldVO();

        String novoStatus = vo.asString("AD_STATUSCOTACAO");
        String statusAntigo = voOld != null ? voOld.asString("AD_STATUSCOTACAO") : null;

        Timestamp dtProced = vo.asTimestamp("AD_DTPROCED");

        // UPDATE mudando para 4
        if ("4".equals(novoStatus)
                && !"4".equals(statusAntigo)
                && dtProced == null) {

            throw new Exception(
                    "Para definir o Status da Cotação como 'Agendado', o campo Data do Procedimento é obrigatório."
            );
        }
    }

    @Override public void beforeDelete(PersistenceEvent event) throws Exception {}
    @Override public void afterInsert(PersistenceEvent event) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent event) throws Exception {}
    @Override public void afterDelete(PersistenceEvent event) throws Exception {}
    @Override public void beforeCommit(TransactionContext tranCtx) throws Exception {}
}