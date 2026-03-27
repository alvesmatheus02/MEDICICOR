package br.com.sankhya.bhz.controleFaturamento.acoes;

import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

import java.math.BigDecimal;
import java.util.Collection;

public class acaoAprovInventarioInfUso implements AcaoRotinaJava {
    JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
    JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();

        if (linhas.length < 1){
            ErroUtils.disparaErro("Selecione ao menos uma linha para ação!");
        }

        BigDecimal nuNota = BigDecimal.ZERO;

        for(Registro linha : linhas) {
            nuNota =  new BigDecimal(linha.getCampo("NUNOTA").toString());

            DynamicVO cabVO = cabDAO.findByPK(nuNota);

            if (null == cabVO.asString("AD_BHZAPROVINVENTARIO") || "N".equals(cabVO.asString("AD_BHZAPROVINVENTARIO"))) {
                Collection<DynamicVO> iteVO = iteDAO.find("NUNOTA = ? AND SEQUENCIA < 0", nuNota);

                for (DynamicVO itemVO : iteVO) {
                    iteDAO.prepareToUpdate(itemVO)
                            .set("CODLOCALORIG", BigDecimal.valueOf(1400))
                            .set("CODLOCALTERC", BigDecimal.valueOf(1400))
                            .update();
                }
                linha.setCampo("AD_BHZAPROVINVENTARIO", "S");
            }
        }

        contexto.setMensagemRetorno("Inventário Informe de uso aprovado com sucesso!");
    }
}
