package br.com.sankhya.bhz.utils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.LiberacaoSolicitada;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

public class util {
    public util() {
    }

    public static Timestamp getData() {
        Date data = new Date();
        Timestamp ts = new Timestamp(data.getTime());
        return ts;
    }

    public static int RetornaUltimoCodTabela(String Arquivo, BigDecimal Codemp, String Tabela, String NomeCampo, Integer DtSync) throws Exception {
        JdbcWrapper jdbc = null;
        JapeSession.SessionHandle hnd = null;
        hnd = JapeSession.open();
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        jdbc = dwfEntityFacade.getJdbcWrapper();
        CallableStatement cstmt = null;
        new NativeSql(jdbc);
        int codfila = 0;

        try {
            jdbc = dwfEntityFacade.getJdbcWrapper();
            jdbc.openSession();
            cstmt = jdbc.getConnection().prepareCall("{call STP_KEYGEN_TGFNUM(?,?,?,?,?,?)}");
            cstmt.setString(1, Arquivo);
            cstmt.setBigDecimal(2, Codemp);
            cstmt.setString(3, Tabela);
            cstmt.setString(4, NomeCampo);
            cstmt.setInt(5, DtSync);
            cstmt.registerOutParameter(6, 4);
            cstmt.execute();
            codfila = cstmt.getInt(6);
        } finally {
            if (cstmt != null) {
                cstmt.close();
            }

            JdbcWrapper.closeSession(jdbc);
            if (codfila != 0) {
                return codfila;
            } else {
                throw new Exception("Não foi possível identificar o ultimo registro de amostra. \nPara o analista: Erro na geracao do ultimo codigo de amostra.\n Botao de acao DuplicaAmostra");
            }
        }
    }

    public static void incluirAvisoSistema(BigDecimal codusuremetente, String titulo, String descricao, String solucao, String tipoDest, BigDecimal destinatario, BigDecimal importacia) throws Exception {
        JapeSession.SessionHandle hnd = null;
        JdbcWrapper jdbc = null;
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        jdbc = dwfEntityFacade.getJdbcWrapper();
        BigDecimal codgrupo = null;
        BigDecimal codusu = null;
        if ("G".equals(tipoDest)) {
            codgrupo = destinatario;
        } else {
            codusu = destinatario;
        }

        if (!verificarAvisoEnviado(jdbc, titulo, descricao, solucao, "PERSONALIZADO", importacia, codusu, codgrupo)) {
            try {
                JapeWrapper occ = JapeFactory.dao("AvisoSistema");
                ((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)occ.create().set("CODUSUREMETENTE", codusuremetente)).set("TITULO", titulo)).set("DESCRICAO", descricao)).set("DHCRIACAO", TimeUtils.getNow())).set("IDENTIFICADOR", "PERSONALIZADO")).set("TIPO", "P")).set("SOLUCAO", solucao)).set("CODGRUPO", codgrupo)).set("CODUSU", codusu)).set("IMPORTANCIA", importacia)).save();
            } catch (Exception var13) {
                throw new MGEModelException("Erro incluir aviso no sistema: " + var13);
            }
        }
    }

    public static void EnviaEmail2(String endereco, String assunto, String mensagem, BigDecimal codSmtp) throws Exception {
        JapeSession.SessionHandle hnd = null;
        JdbcWrapper jdbc = null;
        char[] MsgChar = mensagem.toCharArray();
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        jdbc = dwfEntityFacade.getJdbcWrapper();
        if (!verificarMensagemEnviada(mensagem, jdbc, endereco, assunto)) {
            JapeWrapper occ = JapeFactory.dao("MSDFilaMensagem");
            ((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)occ.create().set("CODFILA", (Object)null)).set("CODUSU", new BigDecimal(0))).set("DTENTRADA", getData())).set("STATUS", "Pendente")).set("CODCON", new BigDecimal(0))).set("MENSAGEM", MsgChar)).set("TIPOENVIO", "E")).set("MAXTENTENVIO", new BigDecimal(3))).set("ASSUNTO", assunto)).set("EMAIL", endereco)).set("REENVIAR", "N")).set("CODSMTP", codSmtp)).save();
        }
    }

    private static boolean verificarMensagemEnviada(String mensagem, JdbcWrapper jdbc, String email, String assunto) throws Exception {
        NativeSql sqlMsgEnviada = new NativeSql(jdbc);
        sqlMsgEnviada.appendSql("SELECT DISTINCT 1 FROM TMDFMG");
        sqlMsgEnviada.appendSql(" WHERE LEN(CONVERT(VARCHAR(MAX),MENSAGEM)) = LEN(:MENSAGEM)");
        sqlMsgEnviada.appendSql("   AND DTENTRADA >= CONVERT(DATE,:DTHOJE)");
        sqlMsgEnviada.appendSql("   AND EMAIL LIKE :EMAIL");
        sqlMsgEnviada.appendSql("   AND ASSUNTO LIKE :ASSUNTO");
        sqlMsgEnviada.setNamedParameter("MENSAGEM", mensagem);
        sqlMsgEnviada.setNamedParameter("DTHOJE", new Timestamp(TimeUtils.getToday()));
        sqlMsgEnviada.setNamedParameter("EMAIL", email);
        sqlMsgEnviada.setNamedParameter("ASSUNTO", assunto);
        ResultSet rsMsgEnviada = sqlMsgEnviada.executeQuery();
        return rsMsgEnviada.next();
    }

    private static boolean verificarAvisoEnviado(JdbcWrapper jdbc, String titulo, String descricao, String solucao, String identificador, BigDecimal importancia, BigDecimal codusu, BigDecimal codgrupo) throws Exception {
        NativeSql sqlMsgEnviada = new NativeSql(jdbc);
        sqlMsgEnviada.appendSql("SELECT DISTINCT 1");
        sqlMsgEnviada.appendSql("  FROM TSIAVI");
        sqlMsgEnviada.appendSql(" WHERE CONVERT(DATE,DHCRIACAO) = CONVERT(DATE,GETDATE())");
        sqlMsgEnviada.appendSql("   AND LEN(TITULO) = LEN(:TITULO)");
        sqlMsgEnviada.appendSql("   AND LEN(DESCRICAO) = LEN(:DESCRICAO)");
        sqlMsgEnviada.appendSql("   AND LEN(SOLUCAO) = LEN(:SOLUCAO)");
        sqlMsgEnviada.appendSql("   AND CODUSU = ISNULL(:CODUSU,CODUSU)");
        sqlMsgEnviada.setNamedParameter("TITULO", titulo);
        sqlMsgEnviada.setNamedParameter("DESCRICAO", descricao);
        sqlMsgEnviada.setNamedParameter("SOLUCAO", solucao);
        sqlMsgEnviada.setNamedParameter("CODUSU", codusu);
        ResultSet rsMsgEnviada = sqlMsgEnviada.executeQuery();
        return rsMsgEnviada.next();
    }

    public static Integer stpKeygenTgfnum(String arquivo, Integer codemp, String tabela, String campo, Integer dsync) throws Exception {
        Integer ultCod = 0;
        new Timestamp(System.currentTimeMillis());
        JdbcWrapper jdbc = null;
        JapeSession.SessionHandle hnd = null;
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        jdbc = dwfEntityFacade.getJdbcWrapper();
        CallableStatement cstmt = null;
        new NativeSql(jdbc);

        try {
            jdbc = dwfEntityFacade.getJdbcWrapper();
            jdbc.openSession();
            cstmt = jdbc.getConnection().prepareCall("{call STP_KEYGEN_TGFNUM(?,?,?,?,?,?)}");
            cstmt.setString(1, arquivo);
            cstmt.setInt(2, codemp);
            cstmt.setString(3, tabela);
            cstmt.setString(4, campo);
            cstmt.setInt(5, dsync);
            cstmt.registerOutParameter(6, 4);
            cstmt.execute();
            ultCod = cstmt.getInt(6);
        } finally {
            if (cstmt != null) {
                cstmt.close();
            }

            JdbcWrapper.closeSession(jdbc);
        }

        return ultCod;
    }

    public static boolean validaEmpAtivaPefin(BigDecimal codemp) throws Exception {
        boolean validador = false;
        String usuarioPefin = NativeSql.getString("ISNULL(E.AD_LOGONSERASA,M.AD_LOGONSERASA)", "TSIEMP E INNER JOIN TSIEMP M ON E.CODEMPMATRIZ = M.CODEMP", " E.CODEMP = ?", new Object[]{codemp});
        if (usuarioPefin != null) {
            validador = true;
        }

        return validador;
    }

    public static void confirmarNota(BigDecimal nuNota) throws Exception {
        String toResult="";
        CACHelper cacHelper = new CACHelper();

        BarramentoRegra barramento = BarramentoRegra.build(CACHelper.class,
                "regrasConfirmacaoCAC.xml", AuthenticationInfo.getCurrent());
        cacHelper.confirmarNota(nuNota, barramento);


        if (barramento.getLiberacoesSolicitadas().size() == 0 &&
                barramento.getErros().size() == 0) {
            System.out.println("Nota Confirmada " + nuNota + "");

        } else {
            if (barramento.getErros().size() > 0) {
                System.out.println("Erro na confirma��o " +
                        nuNota);

                for (Exception e : barramento.getErros()) {
                    toResult =
                            e.getMessage();
                    break;
                }
            }

            if (barramento.getLiberacoesSolicitadas().size() > 0) {
                System.out.println("Erro na confirma��o " + nuNota
                        + ". Foi solicitada libera��es");
                toResult = "Libera��es solicitadas - \n";
                for (LiberacaoSolicitada e :
                        barramento.getLiberacoesSolicitadas()) {
                    toResult += "Evento: "
                            + e.getEvento() + (e.getDescricao() != null ? " Descri��o:  "
                            + e.getDescricao() + "\n" : "\n");
                    break;
                }

            }

        }
        System.out.println(toResult);
    }
    public static void recalculaImpostosNota(BigDecimal nuNota) throws Exception {
        ImpostosHelpper impostohelp = new ImpostosHelpper();
        impostohelp.setForcarRecalculo(true);
        impostohelp.setSankhya(false);
        impostohelp.calcularImpostos(nuNota);
    }
}
