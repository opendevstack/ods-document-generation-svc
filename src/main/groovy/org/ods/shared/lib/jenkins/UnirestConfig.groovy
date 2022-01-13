package org.ods.shared.lib.jenkins


import kong.unirest.Unirest

class UnirestConfig {

    
    static void init() {
        Unirest.config().socketTimeout(6000000).connectTimeout(600000)
    }

}
