package ca.licef.comete.metadata.util;

import ca.licef.comete.core.util.Constants;
import licef.tsapi.model.Triple;

public class XSLTUtil {

    public static String getTriplesAsString(Triple[] triples) throws Exception {
        StringBuilder str = new StringBuilder();
        String tripleDelimiter = "";
        for (Triple triple : triples) {
            str.append(tripleDelimiter);
            str.append(convertTripleToString(triple));
            tripleDelimiter = "@@@";
        }
        return (str.toString());
    }

    public static String getTripleAsString(Triple triple) throws Exception {
        StringBuilder str = new StringBuilder();
        str.append(convertTripleToString(triple));
        return (str.toString());
    }

    private static String convertTripleToString(Triple triple) {
        StringBuilder str = new StringBuilder();
        str.append(triple.getSubject()).append("###");
        str.append(triple.getPredicate()).append("###");
        str.append(triple.getObject()).append("###");
        str.append(triple.isObjectLiteral());
        if (triple.getLanguage() != null)
            str.append("###").append(triple.getLanguage());
        return (str.toString());
    }

}
