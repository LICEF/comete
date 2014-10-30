package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import licef.tsapi.model.Triple;

import java.util.Arrays;

public class Metadata {

    private static Metadata instance;

    public static Metadata getInstance() {
        if (instance == null)
            instance = new Metadata();
        return (instance);
    }

}
