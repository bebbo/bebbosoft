package de.bb.ws;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.xml.ws.WebFault;

import de.bb.util.XmlFile;

public class O {

    String opName;
    String inputType;
    String outputType;

    private String inputLit;
    private String outputLit;
    private HashMap<String, String> faultLits;
    HashMap<String, String> faultTypes;
    HashMap<String, Integer> inParamNames;
    HashMap<String, Integer> outParamNames;
    HashMap<String, Class<?>> exMap;

    O(SD sd, XmlFile wsdl, String opKey, String ptypeKey, String root, String ns) {
        opName = wsdl.getString(opKey, "name", null);
        String popKey = ptypeKey + opName;

        String inputMsg = rest(wsdl.getString(popKey + "/" + ns + "input", "message", null));
        String inputKey = root + "\\" + ns + "message\\" + inputMsg + "/" + ns + "part/";
        String inputPart = rest(wsdl.getString(inputKey, "element", null));

        if (inputPart == null) {
            inputType = redefine(sd, wsdl, root, ns, inputMsg);
        } else {
            inputType = rest(sd.elements.get(inputPart).type);
        }

        inputLit = wsdl.getString(opKey + ns + "input/soap:body", "use", null);

        String outputMsg = rest(wsdl.getString(popKey + "/" + ns + "output", "message", null));
        String outputKey = root + "\\" + ns + "message\\" + outputMsg + "/" + ns + "part/";
        String outputPart = rest(wsdl.getString(outputKey, "element", null));

        if (outputPart == null) {
            outputType = redefine(sd, wsdl, root, ns, outputMsg);
        } else {
            outputType = rest(sd.elements.get(outputPart).type);
        }

        outputLit = wsdl.getString(opKey + "output/soap:body", "use", null);

        faultLits = new HashMap<String, String>();
        faultTypes = new HashMap<String, String>();
        for (String faultKey : wsdl.getSections(popKey + "/fault")) {
            String faultMsg = rest(wsdl.getString(faultKey, "message", null));
            String faultName = wsdl.getString(faultKey, "name", null);
            String fKey = root + "\\" + ns + "message\\" + faultMsg + "/part/";
            String faultPart = rest(wsdl.getString(fKey, "element", null));

            String faultType;
            if (outputPart == null) {
                faultType = redefine(sd, wsdl, root, ns, faultMsg);
            } else {
                faultType = rest(sd.elements.get(faultPart).type);
            }

            faultTypes.put(faultName, faultType);

            String faultLit = wsdl.getString(opKey + "\\fault\\" + faultName + "/soap:fault", "use", null);
            faultLits.put(faultName, faultLit);
        }
    }

    private static String redefine(SD sd, XmlFile wsdl, String root, String ns, String msg) {

        String type = msg + "Type";
        for (int i = 0;; ++i) {
            if (sd.types.get(type) == null)
                break;
            type = msg + "Type" + i;
        }
        List<EL> parms = new ArrayList<EL>();
        String key = root + "\\" + ns + "message\\" + msg + "/";

        for (String pKey : wsdl.getSections(key)) {
            EL el = new EL(wsdl, pKey);
            parms.add(el);
        }
        sd.types.put(type, parms);
        return type;
    }

    static String rest(String s) {
        if (s == null)
            return null;
        int colon = s.indexOf(':');
        return s.substring(colon + 1);
    }

    public void createCode(SD sd, StringBuilder sb, HashSet<String> used) {

        List<EL> in = sd.types.get(inputType);
        HashSet<String> inNames = new HashSet<String>();
        for (EL el : in) {
            inNames.add(el.name);
        }
        List<EL> out = sd.types.get(outputType);

        EL ret = null;
        for (EL el : out) {
            if (!inNames.contains(el.name)) {
                ret = el;
                break;
            }
        }

        sb.append("  @WebMethod(operationName = \"" + opName + "\")\r\n");

        if (ret == null) {
            sb.append("  public void ");
        } else {
            sb.append("  @WebResult(name = \"" + ret.name + "\")\r\n");
            sb.append("  public ");
            sb.append(sd.mapType(ret.type)).append(" ");
            used.add(ret.type);
        }

        sb.append(opName).append("(");

        boolean needsKomma = false;
        HashSet<String> inout = new HashSet<String>();
        // add out values
        for (EL el : out) {
            if (ret != null && ret.name.equals(el.name))
                continue;
            if (needsKomma)
                sb.append(", ");
            sb.append("@WebParam(name = \"" + el.name + "\", mode = ");
            if (inNames.contains(el.name)) {
                inout.add(el.name);
                sb.append("Mode.INOUT) ");
            } else {
                sb.append("Mode.OUT) ");
            }
            sb.append("Holder<").append(sd.mapType(el.type)).append("> ").append(el.name);
            used.add(el.type);
            needsKomma = true;
        }

        // add in values - also check for INOUT
        for (EL el : in) {
            if (inout.contains(el.name))
                continue;
            if (needsKomma)
                sb.append(", ");
            sb.append("@WebParam(name = \"" + el.name + "\", mode = Mode.IN) ");

            sb.append(sd.mapType(el.type)).append(" ").append(el.name);
            used.add(el.type);
            needsKomma = true;
        }

        sb.append(");\r\n\r\n");
    }

    void init(Method method) {
        if (inParamNames != null)
            return;
        
        // get parameter names
        Annotation[][] annoss = method.getParameterAnnotations();
        inParamNames = new HashMap<String, Integer>();
        outParamNames = new HashMap<String, Integer>();
        int index = 0;
        for (Annotation annos[] : annoss) {
            Anno: {
                for (Annotation a : annos) {
                    if (a.annotationType().equals(WebParam.class)) {
                        WebParam wp = (WebParam) a;
                        String paramName = wp.name();
                        if (paramName.length() == 0)
                            paramName = "arg" + index;

                        if (Mode.IN.equals(wp.mode())) {
                            inParamNames.put(paramName, index);
                        } else if (Mode.OUT.equals(wp.mode())) {
                            outParamNames.put(paramName, index);
                        } else {
                            outParamNames.put(paramName, index);
                            inParamNames.put(paramName, index);
                        }

                        ++index;
                        break Anno;
                    }
                }
                String paramName = "arg" + index;
                outParamNames.put(paramName, index);
                inParamNames.put(paramName, index);
                ++index;

            }
        }

        String retName;
        WebResult ra = method.getAnnotation(WebResult.class);
        if (ra != null && ra.name().length() > 0) {
            retName = ra.name();
        } else {
            retName = "return";
        }
        outParamNames.put(retName, -1);


        exMap = new HashMap<String, Class<?>>();
        Class<?>[] exs = method.getExceptionTypes();
        for (Class<?> ex : exs) {
            Annotation[] annos = ex.getAnnotations();
            for (Annotation a : annos) {
                if (a instanceof WebFault) {
                    WebFault wf = (WebFault) a;
                    String wfName = wf.name();
                    if (wfName.length() == 0) {
                        wfName = ex.getName();
                        int slash = wfName.lastIndexOf('/');
                        wfName = wfName.substring(slash + 1);
                    }
                    exMap.put(wfName, ex);
                }
            }
        }
    }

    public String getInputType() {
        return inputType;
    }

    public Set<Entry<String, Integer>> getOutParameters() {
        return outParamNames.entrySet();
    }
    public Set<Entry<String, Integer>> getInParameters() {
        return inParamNames.entrySet();
    }

    public String getOutputType() {
        return outputType;
    }
}
