package br.com.sankhya.bhz.controleFaturamento.acoes;

import br.com.sankhya.bhz.controleFaturamento.model.gerMov;
import br.com.sankhya.bhz.controleFaturamento.model.valAjusteEstTerceiro;
import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.bhz.utils.Utilitarios;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;

public class acaoGeraInfUsoManual implements AcaoRotinaJava {
    JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
    JapeWrapper tpoDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
    JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);


    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();

        if (linhas.length < 1){
            ErroUtils.disparaErro("Selecione ao menos uma linha para ação!");
        }

        BigDecimal nuNota = null;
        BigDecimal nuNotaMov = null;
        BigDecimal codEmp = null;
        BigDecimal codParc = null;


        for(Registro linha : linhas) {
            nuNota = new BigDecimal(linha.getCampo("NUNOTA").toString());
            DynamicVO cabVO = cabDAO.findOne("NUNOTA = ?", nuNota);

            /* NOVO À PARTIR DAQUI*/

            EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
            JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
            NativeSql sql = new NativeSql(jdbc);
            NativeSql sql2 = new NativeSql(jdbc);

            sql.loadSql(gerMov.class, "sql/consMovAutoBotaoAcao.sql");
            sql.setNamedParameter("NUNOTA", nuNota);
            ResultSet resultSet = sql.executeQuery();

            if (resultSet.next()) {

                sql2.loadSql(gerMov.class, "sql/consultaItensPendRem.sql");
                sql2.setNamedParameter("NUNOTA", nuNota);
                ResultSet resultSetValPend = sql2.executeQuery();

                if (!resultSetValPend.next()) {
                    ErroUtils.disparaErro("Remessa não possui itens pendentes para geração do informe de uso, favor verificar.");
                }

                BigDecimal nuNotaMod = resultSet.getBigDecimal("NUNOTAMOD");
                BigDecimal codLocalDest = resultSet.getBigDecimal("CODLOCALDEST");
                BigDecimal codTipPoperDest = resultSet.getBigDecimal("CODTIPOPERDEST");
                String gerConf = resultSet.getString("GERACONF");
                String gerVinc = resultSet.getString("GERAVINCORIG");
                String  tipMovAuto = resultSet.getString("TIPMOVAUTO");

                DynamicVO tpoVO = tpoDAO.findByPK(codTipPoperDest, Utilitarios.getDataMaxTipoOper(codTipPoperDest));

                nuNotaMov = gerMov.geraCabecalho(nuNotaMod, tpoVO, cabVO, gerConf, tipMovAuto, codLocalDest, null, gerVinc, null);

                Collection<DynamicVO> iteVO = iteDAO.find("NUNOTA = ?", nuNotaMov);
                valAjusteEstTerceiro.validaEstoqueTerceiro(iteVO);
            }

        }
        contexto.setMensagemRetorno("Informe de uso nro único "+nuNotaMov+" gerado com sucesso!");
    }
}
