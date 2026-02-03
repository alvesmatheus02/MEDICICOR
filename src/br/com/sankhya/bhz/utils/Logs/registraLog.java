package br.com.sankhya.bhz.utils.Logs;

import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.dao.EntityPropertyDescriptor;
import br.com.sankhya.jape.dao.PersistentObjectUID;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class registraLog {

    public static void gravarLog(EntityDAO entityDAO, DynamicVO vo, DynamicVO voOld, String tipoAlter) throws Exception{
        JapeWrapper logDAO = JapeFactory.dao("AD_TSILOG");
        JapeWrapper campoDAO = JapeFactory.dao("Campo"); //TDDCAM
        JapeWrapper insDAO = JapeFactory.dao("Instancia"); //TDDINS

        String entity = entityDAO.getEntityName(); //Nome da Entidade
        String nomeTab = insDAO.findOne("NOMEINSTANCIA = ?",entity).asString("NOMETAB");

        PersistentObjectUID objectUID = entityDAO.getSQLProvider().getPkObjectUID();
        EntityPropertyDescriptor[] pkFields = objectUID.getFieldDescriptors();
        String pk = "";

        for (EntityPropertyDescriptor pkField : pkFields) {
            String campoPk = pkField.getField().getName();
            if (!"".equals(pk)){
                pk = pk.concat(", ");
            }

            DynamicVO campoVO = campoDAO.findOne("NOMECAMPO = ? AND NOMETAB = ?",campoPk,nomeTab);
            DynamicVO voPk = vo;
            if ("D".equals(tipoAlter)){
                voPk = voOld;
            }
            pk = pk.concat(pkField.getField().getName()+": "+valorCampo(campoVO,voPk,campoPk));
        }

        //ErroUtils.disparaErro(pk);

        Map<String, Object> campos = new HashMap<>();
        campos = entityDAO.getSQLProvider().getAllFieldsByName();
        String novo,antigo = "";

        BigDecimal codUsu = AuthenticationInfo.getCurrent().getUserID();

        for (String campo : campos.keySet()) {  //Nome do Campo

            DynamicVO campoVO = campoDAO.findOne("NOMECAMPO = ? AND NOMETAB = ?",campo,nomeTab);
            novo = valorCampo(campoVO,vo,campo);
            antigo = valorCampo(campoVO,voOld,campo);

            if ((null != novo || null != antigo)
                    && (!StringUtils.getNullAsEmpty(novo).equals(StringUtils.getNullAsEmpty(antigo)))){
                logDAO.create()
                        .set("CODUSU", codUsu)
                        .set("INSTANCIA",entity)
                        .set("DHALTER", TimeUtils.getNow())
                        .set("NOVO",novo)
                        .set("CAMPO",campo)
                        .set("ANTIGO",antigo)
                        .set("TIPOALTER",tipoAlter)
                        .set("TABELA",campoVO.asString("NOMETAB"))
                        .set("CHAVE",pk)
                        .save();
            }
        }
    }

    public static String valorCampo(DynamicVO campoVO, DynamicVO vo, String campo){

        if (vo==null){
            return null;
        }

        String valor = "";
        switch (campoVO.asString("TIPCAMPO")) {
            case "S":
                valor = StringUtils.getNullAsEmpty(vo.asString(campo));
                break;
            case "I":
                valor = vo.asBigDecimalOrZero(campo).toString();
                break;
            case "D":
                valor = null;
                if (null != vo.asTimestamp(campo)) {
                    valor = vo.asTimestamp(campo).toString();
                }
                break;
            case "F":
                valor = vo.asBigDecimalOrZero(campo).toString();
                break;
            case "H":
                valor = null;
                if (null != vo.asTimestamp(campo)) {
                    valor = vo.asTimestamp(campo).toString();
                }
                break;
            case "T":
//                valor = null;
//                if (null != vo.asTimestamp(campo)) {
//                    valor = vo.asTimestamp(campo).toString();
//                }
                valor = vo.asBigDecimalOrZero(campo).toString();
                break;
            case "B":
                valor = null;
                break;
            default:
                valor = StringUtils.getNullAsEmpty(vo.asString(campo));
        }
        return valor;
    }
}
