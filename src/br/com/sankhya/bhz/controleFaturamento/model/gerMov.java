package br.com.sankhya.bhz.controleFaturamento.model;

import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.bhz.utils.Utilitarios;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class gerMov {

    public static BigDecimal geraCabecalho (BigDecimal nuNotaOrig, BigDecimal nuNotaMod, DynamicVO pendFatVO, DynamicVO tpoVO, BigDecimal codParc, BigDecimal codEmp, String confirma, String tipMov, BigDecimal codLocalDest) throws Exception {

        JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
        JapeWrapper parDAO = JapeFactory.dao(DynamicEntityNames.PARCEIRO);
        JapeWrapper compParDAO = JapeFactory.dao("ComplementoParc");

        DynamicVO cabModVO = cabDAO.findByPK(nuNotaMod);
        DynamicVO compParVO = compParDAO.findByPK(codParc);

        String sucess = "S";
        String obs = null;
        BigDecimal nuNotaMov = BigDecimal.ZERO;
        BigDecimal codPend = BigDecimal.ZERO;
        BigDecimal codCR = BigDecimal.ZERO;
        BigDecimal codTipVenda = BigDecimal.ZERO;
        BigDecimal codConvenio = BigDecimal.ZERO;
        BigDecimal codVend = BigDecimal.ZERO;
        BigDecimal codVend2 = BigDecimal.ZERO;
        BigDecimal codGer = BigDecimal.ZERO;
        BigDecimal codProced = BigDecimal.ZERO;
        BigDecimal numReq = BigDecimal.ZERO;

        if (null != pendFatVO) {
            obs = pendFatVO.asString("OBSERVACAO");
            codPend = pendFatVO.asBigDecimalOrZero("CODPEND");
            codCR = pendFatVO.asBigDecimalOrZero("CODCENCUS");
            codVend = pendFatVO.asBigDecimalOrZero("CODVEND");
            codVend2 = pendFatVO.asBigDecimalOrZero("CODVEND2");
            codGer = pendFatVO.asBigDecimalOrZero("CODGER");
            codConvenio = pendFatVO.asBigDecimalOrZero("CODCONVENIO");
            codProced = pendFatVO.asBigDecimalOrZero("CODPROCED");
            numReq = BigDecimal.ONE;
        }
        if (null != compParVO && !compParVO.asBigDecimalOrZero("SUGTIPNEGSAID").equals(BigDecimal.ZERO)) {
            codTipVenda = compParVO.asBigDecimalOrZero("SUGTIPNEGSAID");
        }
        if (null == codLocalDest) {
            codLocalDest = BigDecimal.ZERO;
        }

        try {
            Map<String, Object> alteracoes = new HashMap<>();
            alteracoes.put("CODPARC", codParc);
            alteracoes.put("CODEMP", codEmp);
            alteracoes.put("CODEMPNEGOC", codEmp);
            alteracoes.put("DTNEG", TimeUtils.getNow());
            alteracoes.put("DTENTSAI", TimeUtils.getNow());
            alteracoes.put("DTMOV", TimeUtils.getNow());
            alteracoes.put("HRENTSAI", TimeUtils.getNow());
            alteracoes.put("DHTIPOPER", tpoVO.asTimestamp("DHALTER"));
            alteracoes.put("OBSERVACAO", obs);
            if (!codCR.equals(BigDecimal.ZERO)) alteracoes.put("CODCENCUS", codCR);
            if (!codPend.equals(BigDecimal.ZERO)) alteracoes.put("AD_BHZCODPEND", codPend);
            if (!codTipVenda.equals(BigDecimal.ZERO)) alteracoes.put("CODTIPVENDA", codTipVenda);
            if (!codTipVenda.equals(BigDecimal.ZERO)) alteracoes.put("DHTIPVENDA", Utilitarios.getDataMaxTipVenda(codTipVenda));
            if (!codConvenio.equals(BigDecimal.ZERO)) alteracoes.put("AD_CODCONVENIO", codConvenio);
            if (!codVend.equals(BigDecimal.ZERO)) alteracoes.put("CODVEND", codVend);
            if (!codVend2.equals(BigDecimal.ZERO)) alteracoes.put("AD_CODVEND2", codVend2);
            if (!codGer.equals(BigDecimal.ZERO)) alteracoes.put("AD_CODREGGER", codGer);
            if (!codProced.equals(BigDecimal.ZERO)) alteracoes.put("AD_CODPROCED", codProced);
//            if (!numReq.equals(BigDecimal.ZERO)) alteracoes.put("AD_NUM_REQUISICAO", numReq);

            DynamicVO cabMov = Utilitarios.duplicaRegistroVO(cabModVO, "CabecalhoNota", alteracoes);

            nuNotaMov = cabMov.asBigDecimalOrZero("NUNOTA");

            gerMov.insertItens(cabMov, tpoVO,  codPend, tipMov, codLocalDest);

        } catch (Exception e){
            e.printStackTrace();
            sucess = "N";
            ErroUtils.disparaErro(e.getMessage());
        } finally {
            if (sucess.equals("S") && confirma.equals("S")) {
                try {
                    try {
                        Utilitarios.totalizar(nuNotaMov);
                        Utilitarios.confirmarNota(nuNotaMov);

                    }catch (Exception c) {
                        c.printStackTrace();
                        ErroUtils.disparaErro(c.getMessage());
                    }
                } catch (Exception ce) {
                    ce.printStackTrace();
                    ErroUtils.disparaErro(ce.getMessage());
                }
            } else {
                try {
                    Utilitarios.totalizar(nuNotaMov);
                }catch (Exception c) {
                    c.printStackTrace();
                    ErroUtils.disparaErro(c.getMessage());
                }
            }
        }
        return nuNotaMov;
    }

    public static void insertItens (DynamicVO cabMov, DynamicVO tpoVO, BigDecimal nuNotaOrig, String tipMov, BigDecimal codLocalDest) throws Exception {

        JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
        JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        JapeWrapper itePendFatDAO = JapeFactory.dao("AD_TGFESTPEND");

        Collection<DynamicVO> itensVO = null;

        String atualEstTop = tpoVO.asString("ATUALEST");
        String atualEstTerc = tpoVO.asString("ATUALESTTERC");
        String usarPrecoCusto = tpoVO.asString("USARPRECOCUSTO");
        String teceiros = "N";
        String reserva = "N";
        BigDecimal nuTab = BigDecimal.ZERO;
        BigDecimal atualEst = BigDecimal.ZERO;
        BigDecimal codEmp = cabMov.asBigDecimalOrZero("CODEMP");
        BigDecimal codParc = cabMov.asBigDecimalOrZero("CODPARC");
        BigDecimal codProd = null;
        BigDecimal qtdNeg = null;
        BigDecimal vlrUnit = null;
        BigDecimal vlrTot = null;
        BigDecimal codLocalOrig = BigDecimal.ZERO;
        Timestamp dtVal = null;
        String controle = " ";
        String codVol = null;
        String usoProd = null;



        if (atualEstTop.equals("B")) {
            atualEst = BigDecimal.valueOf(-1);
        } else if (atualEstTop.equals("E")) {
            atualEst = BigDecimal.ONE;
        } else if (atualEstTop.equals("R")) {
            atualEst = BigDecimal.ONE;
            reserva = "S";
        }

        if (!atualEstTerc.equals("N")) {
            teceiros = "S";
        }

        if (tipMov.equals("P")) { /*MOVIMENTOS PORTAIS*/
            itensVO = iteDAO.find("NUNOTA = ?", nuNotaOrig);
        } else if (tipMov.equals("PF")) { /*MOVIMENTOS PENDENCIA FATURAMENTO*/
            itensVO = itePendFatDAO.find("CODPEND = ?", nuNotaOrig);
        }

        for (DynamicVO iteVO : itensVO) {
            try {
                DynamicVO proVO = proDAO.findByPK(iteVO.asBigDecimalOrZero("CODPROD"));

                if (tipMov.equals("P")) { /*MOVIMENTOS PORTAIS SIMPLES*/
                    codProd = iteVO.asBigDecimalOrZero("CODPROD");
                    qtdNeg = iteVO.asBigDecimalOrZero("QTDNEG");
                    vlrUnit = iteVO.asBigDecimalOrZero("VLRUNIT");
                    vlrTot = iteVO.asBigDecimalOrZero("VLRTOT");
                    codVol = iteVO.asString("CODVOL");
                    usoProd = iteVO.asString("USOPROD");
                    codLocalOrig = iteVO.asBigDecimalOrZero("CODLOCALORIG");
                    if (codLocalDest == null || codLocalDest.equals(BigDecimal.ZERO)) codLocalDest = iteVO.asBigDecimalOrZero("CODLOCALORIG");

                } else if (tipMov.equals("PF")) { /*MOVIMENTOS PENDENCIA FATURAMENTO*/
                    codProd = iteVO.asBigDecimalOrZero("CODPROD");
                    qtdNeg = iteVO.asBigDecimalOrZero("ESTOQUE");
                    vlrUnit = iteVO.asBigDecimalOrZero("VLRUNIT");
                    vlrTot = qtdNeg.multiply(vlrUnit);
                    codVol = proVO.asString("CODVOL");
                    usoProd = proVO.asString("USOPROD");
                    codLocalOrig = iteVO.asBigDecimalOrZero("CODLOCAL");
                    if (codLocalDest == null || codLocalDest.equals(BigDecimal.ZERO)) codLocalDest = iteVO.asBigDecimalOrZero("CODLOCAL");
                    controle = iteVO.asString("CONTROLE");
                    dtVal = iteVO.asTimestamp("DTVAL");
                }

                FluidCreateVO creITE = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA).create();
                creITE.set("NUNOTA", nuNotaOrig);
                creITE.set("CODEMP", codEmp);
                creITE.set("CODPROD", codProd);
                creITE.set("CODVOL", codVol);
                creITE.set("QTDNEG", qtdNeg);
                creITE.set("CONTROLE", controle);
                creITE.set("AD_DTVAL", dtVal);
                creITE.set("ATUALESTOQUE", atualEst);
                creITE.set("RESERVA", reserva);
                creITE.set("CODLOCALORIG", codLocalOrig);
                creITE.set("ATUALESTTERC", atualEstTerc);
                creITE.set("TERCEIROS", teceiros);
                creITE.set("USOPROD", usoProd);
                creITE.set("VLRUNIT", vlrUnit);
                creITE.set("VLRTOT", vlrTot);

                DynamicVO itemCriado = creITE.save();

                if(!codLocalOrig.equals(BigDecimal.ZERO) && !codLocalDest.equals(codLocalOrig)) {
                    iteDAO.prepareToUpdateByPK(cabMov.asBigDecimalOrZero("NUNOTA"), itemCriado.asBigDecimalOrZero("SEQUENCIA").multiply(BigDecimal.valueOf(-1)))
                            .set("CODLOCALORIG", codLocalDest)
                            .update();
                }

            } catch (Exception e) {
                e.printStackTrace();
                ErroUtils.disparaErro(e.getMessage());
            }
        }
    }
}
