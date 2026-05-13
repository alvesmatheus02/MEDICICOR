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

    public static BigDecimal geraCabecalho (BigDecimal nuNotaMod, DynamicVO tpoVO, DynamicVO cabOrigVO, String confirma, String tipMov, BigDecimal codLocalDest, BigDecimal codLocalOrig, String geraVinculo) throws Exception {

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

        if (tpoVO.asBigDecimalOrZero("ATUALFIN").compareTo(BigDecimal.ZERO) == 0 && !tpoVO.asString("TIPMOV").equals("P")) {
            codTipVenda = BigDecimal.ZERO;
        } else if (null != compParVO && !compParVO.asBigDecimalOrZero("SUGTIPNEGSAID").equals(BigDecimal.ZERO)) {
            codTipVenda = compParVO.asBigDecimalOrZero("SUGTIPNEGSAID");
        } else {
            DynamicVO cabVoMod = cabDAO.findByPK(nuNotaMod);
            codTipVenda = cabVoMod.asBigDecimalOrZero("CODTIPVENDA");
        }

        if (null == codLocalDest) {
            codLocalDest = BigDecimal.ZERO;
        }
        if (cabOrigVO.asBigDecimalOrZero("AD_BHZCODPARCIFU").compareTo(BigDecimal.ZERO) > 0 && tpoVO.asString("ADIARATUALEST").equals("N")) {
            codParc = cabOrigVO.asBigDecimalOrZero("AD_BHZCODPARCIFU");
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

            alteracoes.put("AD_AUTORIZACAO", cabOrigVO.asString("AD_AUTORIZACAO"));
            alteracoes.put("AD_CARTPAC", cabOrigVO.asString("AD_CARTPAC"));
            alteracoes.put("AD_CODKIT", cabOrigVO.asBigDecimal("AD_CODKIT"));
            alteracoes.put("AD_CODLOCPROC", cabOrigVO.asBigDecimal("AD_CODLOCPROC"));
            alteracoes.put("AD_CODMEDICO", cabOrigVO.asBigDecimal("AD_CODMEDICO"));
            alteracoes.put("AD_CODPACIENTE", cabOrigVO.asBigDecimal("AD_CODPACIENTE"));
            alteracoes.put("AD_CODPROCED", cabOrigVO.asBigDecimal("AD_CODPROCED"));
            alteracoes.put("AD_CODPROD", cabOrigVO.asBigDecimal("AD_CODPROD"));
            alteracoes.put("AD_DTCRETPROCED", cabOrigVO.asTimestamp("AD_DTCRETPROCED"));
            alteracoes.put("AD_DTPROCED", cabOrigVO.asTimestamp("AD_DTPROCED"));
            alteracoes.put("AD_DTPROCED2", cabOrigVO.asTimestamp("AD_DTPROCED2"));
            alteracoes.put("AD_DTRETPROCED", cabOrigVO.asTimestamp("AD_DTRETPROCED"));
            alteracoes.put("AD_NUM_REQUISICAO", cabOrigVO.asString("AD_NUM_REQUISICAO"));
            alteracoes.put("AD_NUMGUIAOPER", cabOrigVO.asString("AD_NUMGUIAOPER"));
            alteracoes.put("AD_NVLPROCED", cabOrigVO.asString("AD_NVLPROCED"));
            alteracoes.put("AD_OBSERVACAOINTERNA", cabOrigVO.asString("AD_OBSERVACAOINTERNA"));
            alteracoes.put("AD_AIHPRONTATENDCODDOPROC", cabOrigVO.asString("AD_AIHPRONTATENDCODDOPROC"));
            alteracoes.put("AD_CODCONVENIO", cabOrigVO.asBigDecimal("AD_CODCONVENIO"));
            alteracoes.put("AD_CODREGGER", cabOrigVO.asBigDecimal("AD_CODREGGER"));
            alteracoes.put("AD_CODREGINST", cabOrigVO.asBigDecimal("AD_CODREGINST"));
            alteracoes.put("AD_GERENTE", cabOrigVO.asString("AD_GERENTE"));


            DynamicVO cabMov = Utilitarios.duplicaRegistroVO(cabModVO, "CabecalhoNota", alteracoes);

            nuNotaMov = cabMov.asBigDecimalOrZero("NUNOTA");

            gerMov.insertItens(cabMov, tpoVO,  nuNotaOrig, tipMov, codLocalDest, codLocalOrig, geraVinculo);

        } catch (Exception e){
            e.printStackTrace();
            sucess = "N";
            ErroUtils.disparaErro(e.getMessage());
        } finally {
            if (sucess.equals("S")) {
                try {
                    try {
                        Utilitarios.totalizar(nuNotaMov);
                        if (confirma.equals("S")) {
                            Utilitarios.confirmarNota(nuNotaMov);
                        }
                    }catch (Exception c) {
                        c.printStackTrace();
                        ErroUtils.disparaErro(c.getMessage());
                    }
                } catch (Exception ce) {
                    ce.printStackTrace();
                    ErroUtils.disparaErro(ce.getMessage());
                }
            }
        }
        return nuNotaMov;
    }

    public static void insertItens (DynamicVO cabMov, DynamicVO tpoVO, BigDecimal nuNotaOrig, String tipMov, BigDecimal codLocalDest, BigDecimal codLocalOrig, String geraVinculo) throws Exception {

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
        BigDecimal nuNotaRem = BigDecimal.ZERO;
        BigDecimal seqRem = BigDecimal.ZERO;
        BigDecimal seq = BigDecimal.ZERO;
        BigDecimal codLocalTerc = BigDecimal.ZERO;
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
                if (null == codLocalOrig) {
                    codLocalOrig = codLocalDest == BigDecimal.ZERO ? resultSet.getBigDecimal("CODLOCALORIG") : codLocalDest;
                }
                controle = resultSet.getString("CONTROLE");
                nuNotaRem = resultSet.getBigDecimal("NUNOTAREM");
                seqRem = resultSet.getBigDecimal("SEQUENCIAREM");
                codLocalTerc = resultSet.getBigDecimal("CODLOCALTERC");

                FluidCreateVO creITE = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA).create();
                creITE.set("NUNOTA", nuNotaMov);
                creITE.set("CODEMP", codEmp);
                creITE.set("CODPROD", codProd);
                creITE.set("CODVOL", codVol);
                creITE.set("QTDNEG", qtdNeg);
                creITE.set("CONTROLE", controle);
                creITE.set("ATUALESTOQUE", atualEst);
                creITE.set("RESERVA", reserva);
                creITE.set("CODLOCALORIG", codLocalTerc);
                creITE.set("CODLOCALTERC", codLocalTerc);
                creITE.set("ATUALESTTERC", atualEstTerc);
                creITE.set("TERCEIROS", teceiros);
                creITE.set("VLRUNIT", vlrUnit);
                creITE.set("VLRTOT", vlrTot);

                DynamicVO itemCriado = creITE.save();

                if (!codLocalOrig.equals(BigDecimal.ZERO) && !codLocalTerc.equals(codLocalOrig)) {
                    iteDAO.prepareToUpdateByPK(cabMov.asBigDecimalOrZero("NUNOTA"), itemCriado.asBigDecimalOrZero("SEQUENCIA").multiply(BigDecimal.valueOf(-1)))
                            .set("CODLOCALORIG", codLocalOrig)
                            .set("CODLOCALTERC", codLocalOrig)
                            .update();
                }

                geraVar(nuNotaMov, itemCriado.asBigDecimal("SEQUENCIA"), nuNotaRem, seqRem, qtdNeg);

            }
        } else {
            if (tipMov.equals("RETSIMB")) {
                itensVO = iteDAO.find("NUNOTA = ? AND CODLOCALORIG = 1400", nuNotaOrig);
            } else if (tipMov.equals("PEDINFUSO")) {
                itensVO = iteDAO.find("NUNOTA = ? AND CODLOCALORIG = 200", nuNotaOrig);
            } else {
                itensVO = iteDAO.find("NUNOTA = ?", nuNotaOrig);
            }

            for (DynamicVO iteVO : itensVO) {
                try {

                    if (tipMov.equals("RETSIMB")) {
                        iteDAO.prepareToUpdate(iteVO)
                                .set("CODLOCALORIG", BigDecimal.valueOf(200))
                                .set("CODLOCALTERC", BigDecimal.valueOf(200))
                                .update();
                    }

                    seq = iteVO.asBigDecimalOrZero("SEQUENCIA");
                    codProd = iteVO.asBigDecimalOrZero("CODPROD");
                    qtdNeg = iteVO.asBigDecimalOrZero("QTDNEG");
                    vlrUnit = iteVO.asBigDecimalOrZero("VLRUNIT");
                    vlrTot = iteVO.asBigDecimalOrZero("VLRTOT");
                    codVol = iteVO.asString("CODVOL");
                    usoProd = iteVO.asString("USOPROD");
                    controle = iteVO.asString("CONTROLE");
                    if (null == codLocalOrig) {
                        codLocalOrig = iteVO.asBigDecimalOrZero("CODLOCALORIG");
                    }
                    if (codLocalDest == null || codLocalDest.equals(BigDecimal.ZERO))
                        codLocalDest = codLocalOrig;

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
                    creITE.set("CODLOCALTERC", codLocalOrig);
                    creITE.set("ATUALESTTERC", atualEstTerc);
                    creITE.set("TERCEIROS", teceiros);
                    creITE.set("VLRUNIT", vlrUnit);
                    creITE.set("VLRTOT", vlrTot);

                    DynamicVO itemCriado = creITE.save();

                    if (!codLocalOrig.equals(BigDecimal.ZERO) && !codLocalDest.equals(codLocalOrig)) {
                        iteDAO.prepareToUpdateByPK(cabMov.asBigDecimalOrZero("NUNOTA"), itemCriado.asBigDecimalOrZero("SEQUENCIA").multiply(BigDecimal.valueOf(-1)))
                                .set("CODLOCALORIG", codLocalDest)
                                .set("CODLOCALTERC", codLocalDest)
                                .update();
                    }




                    if (geraVinculo.equals("S")) {
                        geraVar(nuNotaMov, itemCriado.asBigDecimal("SEQUENCIA"), nuNotaOrig, seq, qtdNeg);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    ErroUtils.disparaErro(e.getMessage());
                }
            }
        }
    }

    public static void geraVar(BigDecimal nunota, BigDecimal sequencia, BigDecimal nunotaorig, BigDecimal sequenciaorig, BigDecimal qtdatendida) throws Exception {
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
