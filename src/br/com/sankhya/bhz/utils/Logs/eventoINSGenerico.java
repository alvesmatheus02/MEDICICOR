package br.com.sankhya.bhz.utils.Logs;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class eventoINSGenerico implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        EntityDAO entityDAO = persistenceEvent.getTargetDAO();


        registraLog.gravarLog(entityDAO,vo,null,"I");

    }

    @Override
    public void afterUpdate(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void afterDelete(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext transactionContext) throws Exception {

    }
}
