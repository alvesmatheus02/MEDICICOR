package br.com.sankhya.bhz.utils.ValicaoCab;


import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class ValidaOrigemCotacao implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        validar(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        validar(event);
    }

    private void validar(PersistenceEvent event) throws Exception {

        DynamicVO cabVO = (DynamicVO) event.getVo();

        String tipMov = cabVO.asString("TIPMOV");
        String origemCot = cabVO.asString("AD_ORIGEM_COT");
        String portalBHZ = cabVO.asString("AD_BHZPORTAIS");

        if ("P".equals(tipMov)
                && origemCot != null
                && "PORTAL".equalsIgnoreCase(origemCot)
                && (portalBHZ == null || portalBHZ.trim().isEmpty())) {

            throw new Exception(
                    "Quando a origem da cotação for Portal, o campo Portal deve ser preenchido."
            );
        }
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
