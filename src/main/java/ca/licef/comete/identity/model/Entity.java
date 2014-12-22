package ca.licef.comete.identity.model;

import ca.licef.comete.core.util.Constants;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 28-Nov-2011
 */
public abstract class Entity {

    Hashtable<String, ArrayList<String>> data = new Hashtable<>();

    public ArrayList<String> getList(String namespace) {
        return data.get(namespace);
    }

    public void addValue(String namespace, String value) {
        addValue(namespace, value, Constants.IDENTITY_PREDICATES.contains(namespace));
    }

    public void addValue(String predicate, String value, boolean isMain) {
        if (Constants.IDENTITY_ALT_PREDICATES.contains(predicate))
            predicate = Constants.getMainPredicate(predicate);

        ArrayList<String> list = data.get(predicate);
        if (!list.contains(value)) {
            if (isMain)
                list.add(0, value);
            else
                list.add(value);
        }
    }

    public String getMainValue(ArrayList<String> list) {
        if (list.isEmpty())
            return null;
        return list.get(0);
    }

    public String getMainValue(ArrayList<String> list, String prefix) {
        if (list.isEmpty())
            return null;
        if (list.get(0).startsWith(prefix))
            return list.get(0);
        if (list.size() >= 2 && list.get(1).startsWith(prefix))
            return list.get(1);
        return null;
    }

    public abstract String getUri();

    public abstract String getFN();

    public abstract boolean isSameType(Entity en);

    public abstract boolean hasJustFN();

    public ArrayList getPopulated() {
        ArrayList<ArrayList<String>> populated = new ArrayList<ArrayList<String>>();
        for (String key: data.keySet()) {
            ArrayList<String> l = data.get(key);
            if (!l.isEmpty())
                populated.add(l);
        }
        return populated;
    }
}
