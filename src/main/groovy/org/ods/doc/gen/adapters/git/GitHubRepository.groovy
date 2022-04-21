package org.ods.doc.gen.adapters.git

import feign.Headers
import feign.Param
import feign.RequestLine
import feign.Response

interface GitHubRepository {

    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /opendevstack/ods-document-generation-templates/archive/v{version}.zip")
    Response getTemplatesZip(@Param("version") String version)

}