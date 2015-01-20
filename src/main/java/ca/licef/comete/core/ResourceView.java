package ca.licef.comete.core;

import licef.tsapi.model.Triple;

import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 28-Jun-2012
 */
public interface ResourceView {

    String getRdf(String uri, String includeIncomingLinks, boolean includeRdfMetadataInfos, boolean isHumanReadable ) throws Exception;
    String getIncomingLinks( String uri, boolean isHumanReadable, int offset, int limit, String format ) throws Exception;
    String getHtml(String uri, Locale locale, String style) throws Exception;
    Triple[] getTriples(String uri, String includeIncomingLinks, boolean includeRdfMetadataInfos, boolean isHumanReadable ) throws Exception;
    
}
