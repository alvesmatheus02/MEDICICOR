/***
 * Created by: Fabio Barroso
 * Date: 24/04/2019
 */
package br.com.sankhya.bhz.utils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import com.sankhya.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class paramsRelatorio {

    public static Map<String, Object> buildReportParams(EntityFacade dwf, Map<String, Object> pk, JdbcWrapper jdbc) throws Exception {

        Map<String, Object> reportParams = new HashMap();
        String pastaModelos = StringUtils.getEmptyAsNull((String) MGECoreParameter.getParameter("os.diretorio.modelos"));
        reportParams.put("REPORT_CONNECTION", jdbc.getConnection());
        reportParams.put("PDIR_MODELO", StringUtils.getEmptyAsNull(pastaModelos));
        reportParams.put("PCODUSULOGADO", AuthenticationInfo.getCurrent().getUserID());
        reportParams.put("PNOMEUSULOGADO", AuthenticationInfo.getCurrent().getName());
        Iterator var5 = pk.entrySet().iterator();

        while (var5.hasNext()) {
            Map.Entry<String, Object> entry = (Map.Entry) var5.next();
            reportParams.put((String) entry.getKey(), entry.getValue());
        }
        return reportParams;
    }
}
