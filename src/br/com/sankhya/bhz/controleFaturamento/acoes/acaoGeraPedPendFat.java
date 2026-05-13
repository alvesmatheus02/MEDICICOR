package br.com.sankhya.bhz.controleFaturamento.acoes;

import br.com.sankhya.bhz.controleFaturamento.model.gerMov;
import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.bhz.utils.Utilitarios;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

import java.math.BigDecimal;

public class acaoGeraPedPendFat implements AcaoRotinaJava {
    JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
    JapeWrapper tpoDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
    JapeWrapper pendFatDAO = JapeFactory.dao("AD_PENDFAT");
    JapeWrapper contModDAO = JapeFactory.dao("AD_BHZGESMODTPO");
    JapeWrapper contModEmpDAO = JapeFactory.dao("AD_BHZGESMODEMP");

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();

        if (linhas.length < 1){
            ErroUtils.disparaErro("Selecione ao menos uma linha para ação!");
        }

        BigDecimal codTpo = BigDecimal.valueOf(2372);
        BigDecimal codTpoRetSimb = BigDecimal.valueOf(2373);
        BigDecimal nuNotaOrigem = null;
        BigDecimal nuNotaMod = null;
        BigDecimal nuNotaModRetSimb = null;
        BigDecimal nuNotaMov = null;
        BigDecimal nuNotaMovRetSimb = null;
        BigDecimal codEmp = null;
        BigDecimal codParc = null;

        DynamicVO tpoVO = tpoDAO.findByPK(codTpo, Utilitarios.getDataMaxTipoOper(codTpo));
        DynamicVO tpoRetSimbVO = tpoDAO.findByPK(codTpoRetSimb, Utilitarios.getDataMaxTipoOper(codTpoRetSimb));
        DynamicVO modModVO = contModDAO.findOne("CODTIPOPER = ?", codTpo);
        DynamicVO modModRetSimbVO = contModDAO.findOne("CODTIPOPER = ?", codTpoRetSimb);

        if (null == modModVO /*|| null == modModRetSimbVO*/) {
            ErroUtils.disparaErro("Modelo do Tipo de Operação não identificado, favor rever configuração de modelos de tipo de operação.");
        } else {
            nuNotaMod = modModVO.asBigDecimalOrZero("NUNOTA");
            nuNotaModRetSimb = modModRetSimbVO.asBigDecimalOrZero("NUNOTA");
        }

        for(Registro linha : linhas) {
            nuNotaOrigem = new BigDecimal(linha.getCampo("NUNOTA").toString());

            DynamicVO cabVO = cabDAO.findOne("NUNOTA = ?", nuNotaOrigem);

            codEmp = cabVO.asBigDecimalOrZero("CODEMP");
            codParc = cabVO.asBigDecimalOrZero("CODPARC");

            if (codEmp.equals(BigDecimal.ZERO)) {
                ErroUtils.disparaErro("Empresa não identificada, favor revisar lançamento de Pendência de Faturamento.");
            }
            if (codParc.equals(BigDecimal.ZERO)) {
                ErroUtils.disparaErro("Parceiro não identificado, favor revisar lançamento de Pendência de Faturamento.");
            }

            DynamicVO contMOdEmpVO = contModEmpDAO.findOne("CODIGO = ? AND CODEMP = ?", modModVO.asBigDecimalOrZero("CODIGO"), codEmp);

            if (null != contMOdEmpVO) {
                nuNotaMod = contMOdEmpVO.asBigDecimalOrZero("NUNOTA");
            }

            if (null != cabVO && cabVO.asBigDecimalOrZero("AD_BHZNUNOTAMOVDEST").equals(BigDecimal.ZERO)) {

                if (null == cabVO.asString("AD_BHZAPROVINVENTARIO") || cabVO.asString("AD_BHZAPROVINVENTARIO").equals("N")) {
                    ErroUtils.disparaErro("Informe de uso não aprovado, favor entrar em contato com o setor responsável para analise.");
                }


                nuNotaMovRetSimb = gerMov.geraCabecalho(nuNotaModRetSimb, tpoRetSimbVO, cabVO, "S", "RETSIMB", null, BigDecimal.valueOf(200), "N");
//                nuNotaMov = gerMov.geraCabecalho(nuNotaMod, tpoVO, cabVO, "S", "PEDINFUSO", null, BigDecimal.valueOf(200), "S");

//                if (nuNotaMovRetSimb.compareTo(BigDecimal.ZERO) == 0 || nuNotaMov.compareTo(BigDecimal.ZERO) == 0) {
//                    ErroUtils.disparaErro("Não foi possível gerar o pedido de retorno simbólico, favor entrar em contato com o setor responsável para analise.");
//                }
//
                cabDAO.prepareToUpdate(cabVO)
                        .set("AD_BHZNUNOTAMOVDEST", nuNotaMovRetSimb)
                        .update();
//
//                cabDAO.prepareToUpdateByPK(nuNotaMovRetSimb)
//                        .set("AD_BHZNUNOTAMOVDEST", nuNotaMov)
//                        .update();
            } else {
                ErroUtils.disparaErro("Pendencia de faturamento já possui pedido lançado no portal.");
            }
        }
        contexto.setMensagemRetorno("Pedido "+nuNotaMov+" de venda gerado com sucesso!");
    }
}
