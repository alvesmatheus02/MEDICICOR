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
import java.util.Base64;

public class acaoPedFatGerPed implements AcaoRotinaJava {
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

        BigDecimal codTpo = new BigDecimal(contexto.getParam("CODTIPOPER").toString());
        BigDecimal nuNotaMod = null;
        BigDecimal nuNotaMov = null;

        DynamicVO tpoVO = tpoDAO.findByPK(codTpo, Utilitarios.getDataMaxTipoOper(codTpo));
        DynamicVO modModVO = contModDAO.findOne("CODTIPOPER = ?", codTpo);

        if (null == modModVO) {
            ErroUtils.disparaErro("Modelo do Tipo de Operação não identificado, favor rever configuração de modelos de tipo de operação.");
        } else {
            nuNotaMod = modModVO.asBigDecimalOrZero("NUNOTA");
        }

        for(Registro linha : linhas) {
            BigDecimal codPend = new BigDecimal(linha.getCampo("CODPEND").toString());
            BigDecimal codEmp = null;
            BigDecimal codParc = null;

            DynamicVO pendFatVO = pendFatDAO.findByPK(codPend);
            DynamicVO cabVO = cabDAO.findOne("AD_BHZCODPEND = ?", codPend);

            codEmp = pendFatVO.asBigDecimalOrZero("CODEMP");
            codParc = pendFatVO.asBigDecimalOrZero("CODPARC");

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

            if (null == cabVO) {
//                nuNotaMov = gerMov.geraCabecalho(BigDecimal.ZERO, nuNotaMod, pendFatVO, tpoVO, codParc, codEmp, "N", "PF", BigDecimal.ZERO);
            } else {
                ErroUtils.disparaErro("Pendencia de faturamento já possui pedido lançado no portal, favor conferir Nro. Único "
                        +cabVO.asBigDecimalOrZero("NUNOTA").toString()
                        +".");
            }
        }
        contexto.setMensagemRetorno("Pedido de venda gerado com sucesso!");
    }
}
