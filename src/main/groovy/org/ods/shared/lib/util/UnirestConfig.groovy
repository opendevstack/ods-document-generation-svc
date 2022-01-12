package org.ods.shared.lib.util


import kong.unirest.Unirest

class UnirestConfig {

    
    static void init() {
        Unirest.config().socketTimeout(6000000).connectTimeout(600000)
    }

}
