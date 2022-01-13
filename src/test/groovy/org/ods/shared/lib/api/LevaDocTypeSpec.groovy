package org.ods.shared.lib.api

import spock.lang.Specification

class LevaDocTypeSpec extends Specification {

    def "testConver"(){
        expect:
        def type = LevaDocType.valueOf("CSD")
        type == LevaDocType.CSD
    }
}
