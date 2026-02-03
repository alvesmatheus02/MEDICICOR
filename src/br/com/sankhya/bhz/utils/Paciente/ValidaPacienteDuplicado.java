package br.com.sankhya.bhz.utils.Paciente;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

import java.sql.ResultSet;



public class ValidaPacienteDuplicado implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        validar(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        validar(event);
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

    private void validar(PersistenceEvent event) throws Exception {

        DynamicVO vo = (DynamicVO) event.getVo();
        String nomePaciente = vo.asString("DESCRPACIENTE");

        if (nomePaciente == null || nomePaciente.trim().isEmpty()) {
            return;
        }

        JapeWrapper pacienteDAO = JapeFactory.dao("AD_PACIENTE");

        DynamicVO pacienteExistente = pacienteDAO.findOne(
                "UPPER(TRIM(DESCRPACIENTE)) = UPPER(TRIM(?))",
                nomePaciente
        );

        if (pacienteExistente != null) {
            throw new Exception(
                    "Já existe um paciente cadastrado com este nome. Verifique os nomes cadastrados."
            );
        }
    }
}

