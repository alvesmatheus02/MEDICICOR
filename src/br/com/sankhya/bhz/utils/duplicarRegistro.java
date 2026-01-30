package br.com.sankhya.bhz.utils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.dao.EntityPropertyDescriptor;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.dao.PersistentObjectUID;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

public class duplicarRegistro {

    public static DynamicVO duplicaRegistroVO(DynamicVO voOrigem, String entidade, Map<String, Object> map) throws Exception {
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        EntityDAO rootDAO = dwfFacade.getDAOInstance(entidade);
        DynamicVO destinoVO = voOrigem.buildClone();
        limparPk(destinoVO, rootDAO);
        if (map != null)
            for (String campo : map.keySet())
                destinoVO.setProperty(campo, map.get(campo));
        PersistentLocalEntity createEntity = dwfFacade.createEntity(entidade, (EntityVO) destinoVO);
        return (DynamicVO) createEntity.getValueObject();
    }

    private static void limparPk(DynamicVO vo, EntityDAO rootDAO) throws Exception {
        PersistentObjectUID objectUID = rootDAO.getSQLProvider().getPkObjectUID();
        EntityPropertyDescriptor[] pkFields = objectUID.getFieldDescriptors();
        for (EntityPropertyDescriptor pkField : pkFields) {
            vo.setProperty(pkField.getField().getName(), null);
        }
    }

    public static Timestamp getDataMaxOper(BigDecimal codigoTipoOperacao) throws Exception {
        AcessoBanco acessoBanco = null;
        try{
            acessoBanco = new AcessoBanco();
            return acessoBanco.findOne("SELECT MAX(DHALTER) AS DT FROM TGFTOP WHERE CODTIPOPER = " + codigoTipoOperacao)
                    .getTimestamp("DT");
        }finally {
            acessoBanco.closeSession();
        }
    }

    public static Timestamp getDataMaxTipoNeg(BigDecimal codigoTipoNegociacao) throws Exception {
        AcessoBanco acessoBanco = null;
        try{
            acessoBanco = new AcessoBanco();
            return acessoBanco.findOne("SELECT MAX(DHALTER) AS DT FROM TGFTPV WHERE CODTIPVENDA = " + codigoTipoNegociacao)
                    .getTimestamp("DT");
        }finally {
            acessoBanco.closeSession();
        }
    }

    public static BigDecimal getUltCusto(DynamicVO cabVO, DynamicVO iteVO, BigDecimal codEmp) throws Exception{
        JdbcWrapper jdbc = null;
        Timestamp dt = null;
        ResultSet rset = null;
        NativeSql sql = null;
        String usaPrecoComo = cabVO.asString("TipoOperacao.USARPRECOCUSTO");
        String campoCusto = "CUSSEMICM";
        if(usaPrecoComo.equals("E")){
            campoCusto = "ENTRADACOMICMS";
        }
        if(usaPrecoComo.equals("G")){
            campoCusto = "CUSMED";
        }
        if(usaPrecoComo.equals("L")){
            campoCusto = "CUSGER";
        }
        if(usaPrecoComo.equals("M")){
            campoCusto = "CUSMEDICM";
        }
        if(usaPrecoComo.equals("R")){
            campoCusto = "CUSREP";
        }
        if(usaPrecoComo.equals("S")){
            campoCusto = "ENTRADASEMICMS";
        }
        if(usaPrecoComo.equals("V")){
            campoCusto = "CUSVARIAVEL";
        }
        if(usaPrecoComo.equals("Z")){
            campoCusto = "ENTRADASEMICMS";
        }

        boolean paramUsaControleCusto = (Boolean)MGECoreParameter.getParameter("controla.custo.controle");
        int paramCampoAtuaCusto = BigDecimalUtil.getValueOrZero((BigDecimal) MGECoreParameter.getParameter("com.data.para.atualizar.custo")).intValue();
        boolean paramControlaCustoPorEmpresa = (Boolean)MGECoreParameter.getParameter("controla.custo.empresa");
        boolean paramControlaCustoPorLocal = (Boolean)MGECoreParameter.getParameter("controla.custo.local");

        StringBuffer sqlBuf = new StringBuffer();
        sqlBuf.append(" SELECT CUS.").append(campoCusto);
        sqlBuf.append(" FROM TGFCUS CUS");
        sqlBuf.append(" WHERE CUS.CODPROD = :CODPROD");
        sqlBuf.append(" /*EMPRESA AND CUS.CODEMP = :CODEMP EMPRESA*/");
        sqlBuf.append(" /*LOCAL AND CUS.CODLOCAL = :CODLOCAL LOCAL*/");
        sqlBuf.append(" /*CONTROLE AND CUS.CONTROLE = :CONTROLE CONTROLE*/");
        sqlBuf.append(" AND CUS.DTATUAL <= :DT");
        sqlBuf.append(" AND CUS.DTATUAL = (SELECT MAX(CN.DTATUAL)");
        sqlBuf.append(" FROM TGFCUS CN");
        sqlBuf.append(" WHERE CN.CODPROD = :CODPROD");
        sqlBuf.append(" /*EMPRESA AND CN.CODEMP = :CODEMP EMPRESA*/");
        sqlBuf.append(" /*LOCAL AND CN.CODLOCAL = :CODLOCAL LOCAL*/");
        sqlBuf.append(" /*CONTROLE AND CN.CONTROLE = :CONTROLE CONTROLE*/");
        sqlBuf.append(" AND CN.DTATUAL <= :DT)");
        jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
        jdbc.openSession();
        sql = new NativeSql(jdbc);
        sql.appendSql(sqlBuf.toString());
        if (paramControlaCustoPorEmpresa) {
            sql.removeSQLComment("EMPRESA");
            sql.setNamedParameter("CODEMP", codEmp);
        } else {
            sql.removeSQLBlock("EMPRESA");
        }

        sql.setNamedParameter("CODPROD", iteVO.asBigDecimal("CODPROD"));
        if (paramControlaCustoPorLocal) {
            sql.removeSQLComment("LOCAL");
            sql.setNamedParameter("CODLOCAL", iteVO.asBigDecimal("CODLOCALORIG"));
        } else {
            sql.removeSQLBlock("LOCAL");
        }

        if (paramUsaControleCusto) {
            sql.removeSQLComment("CONTROLE");
            sql.setNamedParameter("CONTROLE", iteVO.asString("CONTROLE"));
        } else {
            sql.removeSQLBlock("CONTROLE");
        }

        String nomeCampoDtCusto = ComercialUtils.getCampoParaAtuaCusto(paramCampoAtuaCusto);
        StringBuffer bufDtCusto = new StringBuffer();
        bufDtCusto.append("CabecalhoNota.");
        bufDtCusto.append(nomeCampoDtCusto);
        if (!"DTNEG".equals(nomeCampoDtCusto) && iteVO.asTimestamp(bufDtCusto.toString()) != null) {
            dt = iteVO.asTimestamp(bufDtCusto.toString());
        } else {
            dt = cabVO.asTimestamp("DTNEG");
        }

        sql.setNamedParameter("DT", buildDataHora(dt, cabVO.asBigDecimal("HRMOV")));
        rset = sql.executeQuery();
        BigDecimal result = null;
        if (rset.next()) {
            result = rset.getBigDecimal(1);
        }
        return result;
    }

    private static Timestamp buildDataHora(Timestamp dtMov, BigDecimal hrMov) throws Exception {
        if (hrMov == null) {
            hrMov = BigDecimalUtil.valueOf(TimeUtils.getNow("HHmmss"));
        }

        Timestamp horas = TimeUtils.bigDecimal2Timestamp(hrMov);
        Calendar calHrMov = new GregorianCalendar();
        calHrMov.setTimeInMillis(horas.getTime());
        Calendar calDtMov = new GregorianCalendar();
        calDtMov.setTimeInMillis(TimeUtils.clearTime(dtMov.getTime()));
        calDtMov.set(11, calHrMov.get(11));
        calDtMov.set(12, calHrMov.get(12));
        calDtMov.set(13, calHrMov.get(13));
        return new Timestamp(calDtMov.getTimeInMillis());
    }
}
