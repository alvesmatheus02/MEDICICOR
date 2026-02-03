package br.com.sankhya.bhz.utils.ValicaoCab;

import java.sql.Timestamp;
import java.util.Calendar;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.vo.DynamicVO;

public class AtualizaValidadeProposta implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        aplicar(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        aplicar(event);
    }

    private void aplicar(PersistenceEvent event) {

        DynamicVO vo = (DynamicVO) event.getVo();

        // IF :NEW.TIPMOV = 'P'
        if (!"P".equals(vo.asString("TIPMOV"))) {
            return;
        }

        // AND :NEW.DTNEG IS NOT NULL
        Timestamp dtNeg = vo.asTimestamp("DTNEG");
        if (dtNeg == null) {
            return;
        }

        // :NEW.AD_DTVAL := :NEW.DTNEG + 30;
        Calendar cal = Calendar.getInstance();
        cal.setTime(dtNeg);
        cal.add(Calendar.DAY_OF_MONTH, 30);

        vo.setProperty(
                "AD_DTVAL",
                new Timestamp(cal.getTimeInMillis())
        );
    }

    @Override public void afterInsert(PersistenceEvent event) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent event) throws Exception {}
    @Override public void beforeDelete(PersistenceEvent event) throws Exception {}
    @Override public void afterDelete(PersistenceEvent event) throws Exception {}
    @Override public void beforeCommit(br.com.sankhya.jape.event.TransactionContext tranCtx) throws Exception {}
}
