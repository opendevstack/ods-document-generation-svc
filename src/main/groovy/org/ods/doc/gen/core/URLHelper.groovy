package org.ods.doc.gen.core

class URLHelper {

    static String replaceHostInUrl(String originalUrl, String newUrlHost) {
        URI uri = new URI(originalUrl)
        URI newUri = new URI(newUrlHost)
        return new URI(newUri.getScheme(),
                    newUri.getAuthority(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()).toString();
    }

}
