package br.com.sankhya.bhz.controleFaturamento.acoes;

import br.com.sankhya.bhz.controleFaturamento.model.gerMov;
import br.com.sankhya.bhz.controleFaturamento.model.valAjusteEstTerceiro;
import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.bhz.utils.Utilitarios;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class acaoGeraInfoUsoConvenio implements AcaoRotinaJava {
    JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
    JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
    JapeWrapper varDAO = JapeFactory.dao(DynamicEntityNames.COMPRA_VENDA_VARIOS_PEDIDO);
    JapeWrapper tpoDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);

    JapeWrapper movConsDAO = JapeFactory.dao("AD_BHZCONTMOVCONSIG");

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();

        if (linhas.length < 1){
            ErroUtils.disparaErro("Selecione ao menos uma linha para ação!");
        }

        BigDecimal nuNota = BigDecimal.ZERO;
        BigDecimal sequenica = null;
        BigDecimal nuNotaMov = BigDecimal.ZERO;
        BigDecimal newCodParc =  new BigDecimal(contexto.getParam("NOVOCODPARC").toString());
        String sucess = "N";

        DynamicVO cabMov = null;

        for(Registro linha : linhas) {
            nuNota = new BigDecimal(linha.getCampo("NUNOTA").toString());
            sequenica = new BigDecimal(linha.getCampo("SEQUENCIA").toString());
            BigDecimal sequenicaMov = null;

            DynamicVO cabVO = cabDAO.findByPK(nuNota);
            DynamicVO tpoVO = tpoDAO.findByPK(cabVO.asBigDecimalOrZero("CODTIPOPER"), cabVO.asTimestamp("DHTIPOPER"));
            DynamicVO iteVO = iteDAO.findByPK(nuNota, sequenica);
            DynamicVO iteNegVO = iteDAO.findByPK(nuNota, sequenica.multiply(BigDecimal.valueOf(-1)));

            if (null != iteVO) {
                DynamicVO varVO = varDAO.findOne("NUNOTA = ? AND SEQUENCIA = ?", nuNota, sequenica);

                if (null != varVO /*&& iteVO.asString("PENDENTE").equals("S")*/) {
                    if (nuNotaMov.compareTo(BigDecimal.ZERO) == 0) {
                        Map<String, Object> alteracoes = new HashMap<>();
                         alteracoes.put("AD_BHZCODPARCIFU", newCodParc);
                        cabMov = Utilitarios.duplicaRegistroVO(cabVO, "CabecalhoNota", alteracoes);

                        nuNotaMov = cabMov.asBigDecimalOrZero("NUNOTA");
                    }

                    DynamicVO movPositivo = null;
                    DynamicVO movNegativo = null;

                    movPositivo = movConsDAO.create()
                            .set("NUNOTAIFU", nuNota)
                            .set("SEQIFU", sequenica)
                            .set("CODVOLIFU", iteVO.asString("CODVOL"))
                            .set("QTDNEGIFU", iteVO.asBigDecimalOrZero("QTDNEG"))
                            .set("NUNOTAORIG", varVO.asBigDecimalOrZero("NUNOTAORIG"))
                            .set("SEQORIG", varVO.asBigDecimalOrZero("SEQUENCIAORIG"))
                            .set("CODLOCALORIG", iteVO.asBigDecimalOrZero("CODLOCALORIG"))
                            .set("CODLOCALTERC", iteVO.asBigDecimalOrZero("CODLOCALTERC"))
                            .set("DTMOV", TimeUtils.getNow())
                            .set("CODUSUMOV", AuthenticationInfo.getCurrent().getUserID())
                            .save();

                    if (null != iteNegVO) {
                        movNegativo = movConsDAO.create()
                                .set("NUNOTAIFU", nuNota)
                                .set("SEQIFU", sequenica.multiply(BigDecimal.valueOf(-1)))
                                .set("CODVOLIFU", iteNegVO.asString("CODVOL"))
                                .set("QTDNEGIFU", iteNegVO.asBigDecimalOrZero("QTDNEG"))
                                .set("NUNOTAORIG", varVO.asBigDecimalOrZero("NUNOTAORIG"))
                                .set("SEQORIG", varVO.asBigDecimalOrZero("SEQUENCIAORIG"))
                                .set("CODLOCALORIG", iteNegVO.asBigDecimalOrZero("CODLOCALORIG"))
                                .set("CODLOCALTERC", iteNegVO.asBigDecimalOrZero("CODLOCALTERC"))
                                .set("DTMOV", TimeUtils.getNow())
                                .set("CODUSUMOV", AuthenticationInfo.getCurrent().getUserID())
                                .save();
                    }

                    iteDAO.deleteByCriteria("NUNOTA = ? AND SEQUENCIA = ?", nuNota, sequenica);

                    sequenicaMov = insertItens(cabMov, iteVO, iteNegVO, tpoVO);
                    geraVar(nuNotaMov, sequenicaMov, varVO.asBigDecimalOrZero("NUNOTAORIG"), varVO.asBigDecimalOrZero("SEQUENCIAORIG"), iteVO.asBigDecimalOrZero("QTDNEG"));

                    movConsDAO.prepareToUpdate(movPositivo)
                            .set("SEQDEST", sequenicaMov)
                            .set("NUNOTADEST", nuNotaMov)
                            .update();

                    movConsDAO.prepareToUpdate(movNegativo)
                            .set("SEQDEST", sequenicaMov.multiply(BigDecimal.valueOf(-1)))
                            .set("NUNOTADEST", nuNotaMov)
                            .update();

                    sucess = "S";
                }
            }
        }

        Collection<DynamicVO> itens = iteDAO.find("NUNOTA = ?", nuNotaMov);

        valAjusteEstTerceiro.validaEstoqueTerceiro(itens);

        if(sucess.equals("S")){
            contexto.setMensagemRetorno("Informe de uso do convênio gerada com sucesso! <br><br> Nro. Único: "+nuNotaMov);
        } else {
            ErroUtils.disparaErro("Falha ao gerar informe de uso do convênio!");
        }
    }

    private static BigDecimal insertItens (DynamicVO cabMov, DynamicVO iteVO, DynamicVO iteNegVO, DynamicVO tpoVO) throws Exception {
        JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);

        BigDecimal atualEst = BigDecimal.ZERO;
        BigDecimal nuNotaMov = cabMov.asBigDecimalOrZero("NUNOTA");
        BigDecimal sequencia = null;
        BigDecimal codEmp = cabMov.asBigDecimalOrZero("CODEMP");
        BigDecimal codProd = iteVO.asBigDecimalOrZero("CODPROD");
        BigDecimal qtdNeg = iteVO.asBigDecimalOrZero("QTDNEG");
        BigDecimal vlrUnit = iteVO.asBigDecimalOrZero("VLRUNIT");
        BigDecimal vlrTot = iteVO.asBigDecimalOrZero("VLRTOT");
        BigDecimal codLocalOrig = iteVO.asBigDecimalOrZero("CODLOCALORIG");
        BigDecimal codLocalTerc = iteVO.asBigDecimalOrZero("CODLOCALTERC");
        BigDecimal codLocalOrigNeg = iteNegVO.asBigDecimalOrZero("CODLOCALORIG");
        BigDecimal codLocalTercNeg = iteNegVO.asBigDecimalOrZero("CODLOCALTERC");
        String controle = iteVO.asString("CONTROLE");
        String codVol = iteVO.asString("CODVOL");
        String atualEstConfTop = tpoVO.asString("ADIARATUALEST");
        String atualEstTop = tpoVO.asString("ATUALEST");
        String atualEstTerc = tpoVO.asString("ATUALESTTERC");
        String teceiros = "N";
        String reserva = "N";

        if (atualEstTop.equals("B") && atualEstConfTop.equals("N")) {
            atualEst = BigDecimal.valueOf(-1);
        } else if (atualEstTop.equals("E") && atualEstConfTop.equals("N")) {
            atualEst = BigDecimal.ONE;
        } else if (atualEstTop.equals("R") && atualEstConfTop.equals("N")) {
            atualEst = BigDecimal.ONE;
            reserva = "S";
        }

        if (!atualEstTerc.equals("N")) {
            teceiros = "S";
        }

        FluidCreateVO creITE = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA).create();
        creITE.set("NUNOTA", nuNotaMov);
        creITE.set("CODEMP", codEmp);
        creITE.set("CODPROD", codProd);
        creITE.set("CODVOL", codVol);
        creITE.set("QTDNEG", qtdNeg);
        creITE.set("CONTROLE", controle);
        creITE.set("ATUALESTOQUE", atualEst);
        creITE.set("RESERVA", reserva);
        creITE.set("CODLOCALORIG", codLocalOrig);
        creITE.set("CODLOCALTERC", codLocalTerc);
        creITE.set("ATUALESTTERC", atualEstTerc);
        creITE.set("TERCEIROS", teceiros);
        creITE.set("VLRUNIT", vlrUnit);
        creITE.set("VLRTOT", vlrTot);

        DynamicVO itemCriado = creITE.save();

        sequencia = itemCriado.asBigDecimalOrZero("SEQUENCIA").multiply(BigDecimal.valueOf(-1));

        if (null != iteNegVO){
            iteDAO.prepareToUpdateByPK(nuNotaMov, sequencia)
                    .set("CODLOCALORIG", codLocalOrigNeg)
                    .set("CODLOCALTERC", codLocalTercNeg)
                    .update();
        }

        return sequencia;
    }

    private static void geraVar(BigDecimal nunota, BigDecimal sequencia, BigDecimal nunotaorig, BigDecimal sequenciaorig, BigDecimal qtdatendida) throws Exception {

        JapeWrapper varDAO = JapeFactory.dao("CompraVendavariosPedido");

        FluidCreateVO varVO = varDAO.create();
        varVO.set("NUNOTA",nunota);
        varVO.set("SEQUENCIA",sequencia);
        varVO.set("NUNOTAORIG",nunotaorig);
        varVO.set("SEQUENCIAORIG",sequenciaorig);
        varVO.set("QTDATENDIDA", qtdatendida);
        varVO.set("STATUSNOTA","A");
        varVO.set("CUSATEND", null);
        varVO.set("FIXACAO", null);
        varVO.set("NROATOCONCDRAW", null);
        varVO.set("NROMEMORANDO", null);
        varVO.set("NROREGEXPORT", null);
        varVO.set("ORDEMPROD", null);
        varVO.save();
    }


}
