package br.com.sankhya.bhz.controleFaturamento.model;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

public class valAjusteEstTerceiro {

    public static void validaEstoqueTerceiro (Collection<DynamicVO> iteVO) throws Exception {
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();

        ServiceContext sctx = new ServiceContext(null);
        sctx.setAutentication(AuthenticationInfo.getCurrent());
        sctx.makeCurrent();
        sctx.getResourceId();
        JapeWrapper estDAO = JapeFactory.dao("Estoque");

        try {
            SPBeanUtils.setupContext(sctx);
        } catch (Exception e) {
            e.printStackTrace();
        }

        jdbc.openSession();
        try {
            try {
                for (DynamicVO ite : iteVO) {
                    NativeSql sqlv1 = new NativeSql(jdbc);
                    sqlv1.loadSql(valAjusteEstTerceiro.class, "sql/Verificacao1.sql");
                    sqlv1.setNamedParameter("CODPROD", ite.asBigDecimal("CODPROD"));
                    sqlv1.setNamedParameter("CONTROLE", ite.asString("CONTROLE"));
                    ResultSet resultSetv1 = sqlv1.executeQuery();

                    while (resultSetv1.next()) {
                        BigDecimal codemp = resultSetv1.getBigDecimal("CODEMP");
                        BigDecimal codprod = resultSetv1.getBigDecimal("CODPROD");
                        BigDecimal codlocal = resultSetv1.getBigDecimal("CODLOCAL");
                        BigDecimal codparc = resultSetv1.getBigDecimal("CODPARC");
                        String tipo = resultSetv1.getString("TIPO");
                        String controle = resultSetv1.getString("CONTROLE");
                        BigDecimal central = resultSetv1.getBigDecimal("CENTRAL");
                        BigDecimal estoque = resultSetv1.getBigDecimal("ESTOQUE");

                        int resultComparacao = central.compareTo(estoque);

                        DynamicVO estVO = estDAO.findOne("CODEMP = ? AND CODPROD = ? AND CODLOCAL = ? AND CONTROLE = ? AND CODPARC = ? AND TIPO = ? ",
                                codemp, codprod, codlocal, controle, codparc, tipo);


                        if (resultComparacao > 0) {
                            //Estoque Central > Estoque na TGFEST
                            //Talvez precise mexer na validação do estoque na TGFGRU

                            if (null == estVO) {
                                //Não encontrou registro na EST, então cria um novo e grava;
                                FluidCreateVO fluidCreateVO = estDAO.create();
                                fluidCreateVO.set("CODEMP", codemp);
                                fluidCreateVO.set("CODPROD", codprod);
                                fluidCreateVO.set("CODLOCAL", codlocal);
                                fluidCreateVO.set("CONTROLE", controle);
                                fluidCreateVO.set("CODPARC", codparc);
                                fluidCreateVO.set("TIPO", tipo);
                                fluidCreateVO.set("ESTOQUE", central);
                                fluidCreateVO.save();
                            } else {
                                FluidUpdateVO fluidUpdateVO = estDAO.prepareToUpdate(estVO);
                                fluidUpdateVO.set("ESTOQUE", central);
                                fluidUpdateVO.update();
                            }

                        } else if (resultComparacao < 0) {
                            //Estoque Central < Estoque na TGFEST
                            FluidUpdateVO fluidUpdateVO1 = estDAO.prepareToUpdate(estVO);
                            fluidUpdateVO1.set("ESTOQUE", central);
                            fluidUpdateVO1.update();
                        }
                    }
                    resultSetv1.close();

                }
            } catch (Exception e) {
                RuntimeException re = new RuntimeException(e);
                System.out.println("Erro Exception: " + re);
                throw re;
            }
        } finally {
            JdbcWrapper.closeSession(jdbc);
        }
    }


}
