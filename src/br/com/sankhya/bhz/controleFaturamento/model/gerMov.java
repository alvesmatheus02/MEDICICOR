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

    public static BigDecimal geraCabecalho (BigDecimal nuNotaMod, DynamicVO tpoVO, DynamicVO cabOrigVO, String confirma, String tipMov, BigDecimal codLocalDest) throws Exception {

        JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
        JapeWrapper compParDAO = JapeFactory.dao("ComplementoParc");

        String sucess = "S";
        String obs = null;
        BigDecimal nuNotaMov = BigDecimal.ZERO;
        BigDecimal nuNotaOrig =  cabOrigVO.asBigDecimalOrZero("NUNOTA");
        BigDecimal codParc = cabOrigVO.asBigDecimalOrZero("CODPARC");
        BigDecimal codEmp = cabOrigVO.asBigDecimalOrZero("CODEMP");
        BigDecimal codTipVenda = BigDecimal.ZERO;

        DynamicVO cabModVO = cabDAO.findByPK(nuNotaMod);
        DynamicVO compParVO = compParDAO.findByPK(codParc);

        if (tpoVO.asBigDecimalOrZero("ATUALFIN").compareTo(BigDecimal.ZERO) == 0) {
            codTipVenda = BigDecimal.ZERO;
        } else if (null != compParVO && !compParVO.asBigDecimalOrZero("SUGTIPNEGSAID").equals(BigDecimal.ZERO)) {
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
            alteracoes.put("CODTIPOPER", tpoVO.asBigDecimalOrZero("CODTIPOPER"));
            alteracoes.put("DHTIPOPER", tpoVO.asTimestamp("DHALTER"));
            alteracoes.put("OBSERVACAO", obs);
            alteracoes.put("CODTIPVENDA", codTipVenda);
            alteracoes.put("DHTIPVENDA", Utilitarios.getDataMaxTipVenda(codTipVenda));

            DynamicVO cabMov = Utilitarios.duplicaRegistroVO(cabModVO, "CabecalhoNota", alteracoes);

            nuNotaMov = cabMov.asBigDecimalOrZero("NUNOTA");

            gerMov.insertItens(cabMov, tpoVO,  nuNotaOrig, tipMov, codLocalDest);

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

        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        ResultSet resultSet = null;

        Collection<DynamicVO> itensVO = null;

        String atualEstConfTop = tpoVO.asString("ADIARATUALEST");
        String atualEstTop = tpoVO.asString("ATUALEST");
        String atualEstTerc = tpoVO.asString("ATUALESTTERC");
        String usarPrecoCusto = tpoVO.asString("USARPRECOCUSTO");
        String teceiros = "N";
        String reserva = "N";
        BigDecimal nuNotaMov = cabMov.asBigDecimalOrZero("NUNOTA");
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

        if (tipMov.equals("RR")) {

            sql.loadSql(gerMov.class, "sql/consultaItensPendRem.sql");
            sql.setNamedParameter("NUNOTA", nuNotaOrig);
            resultSet = sql.executeQuery();

            while (resultSet.next()) {
                codProd = resultSet.getBigDecimal("CODPROD");
                qtdNeg = resultSet.getBigDecimal("QTDPENDRET");
                vlrUnit = resultSet.getBigDecimal("VLRUNIT");
                vlrTot = qtdNeg.multiply(vlrUnit);
                codVol = resultSet.getString("CODVOL");
                codLocalOrig = codLocalDest == BigDecimal.ZERO ? resultSet.getBigDecimal("CODLOCALORIG") : codLocalDest;
                controle = resultSet.getString("CONTROLE");

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
                creITE.set("ATUALESTTERC", atualEstTerc);
                creITE.set("TERCEIROS", teceiros);
                creITE.set("VLRUNIT", vlrUnit);
                creITE.set("VLRTOT", vlrTot);

                DynamicVO itemCriado = creITE.save();
            }
        } else {

            itensVO = iteDAO.find("NUNOTA = ?", nuNotaOrig);


            for (DynamicVO iteVO : itensVO) {
                try {
                    codProd = iteVO.asBigDecimalOrZero("CODPROD");
                    qtdNeg = iteVO.asBigDecimalOrZero("QTDNEG");
                    vlrUnit = iteVO.asBigDecimalOrZero("VLRUNIT");
                    vlrTot = iteVO.asBigDecimalOrZero("VLRTOT");
                    codVol = iteVO.asString("CODVOL");
                    usoProd = iteVO.asString("USOPROD");
                    codLocalOrig = iteVO.asBigDecimalOrZero("CODLOCALORIG");
                    if (codLocalDest == null || codLocalDest.equals(BigDecimal.ZERO))
                        codLocalDest = iteVO.asBigDecimalOrZero("CODLOCALORIG");

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
                    creITE.set("ATUALESTTERC", atualEstTerc);
                    creITE.set("TERCEIROS", teceiros);
                    creITE.set("VLRUNIT", vlrUnit);
                    creITE.set("VLRTOT", vlrTot);

                    DynamicVO itemCriado = creITE.save();

                    if (!codLocalOrig.equals(BigDecimal.ZERO) && !codLocalDest.equals(codLocalOrig)) {
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
}
