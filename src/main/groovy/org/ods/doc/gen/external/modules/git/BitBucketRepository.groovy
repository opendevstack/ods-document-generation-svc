package org.ods.doc.gen.external.modules.git

import feign.Headers
import feign.Param
import feign.RequestLine
import feign.Response

interface BitBucketRepository {

    // TODO by config limit=1
    static final int PAGE_LIMIT = 10

    @Headers("Accept: application/json")
    @RequestLine("GET /rest/api/latest/projects/{project}/repos/{repo}/commits?limit=10&start={start}")
    String getCommitsForDefaultBranch(@Param("project") String project,
                                   @Param("repo") String repo,
                                   @Param("start") int start)

    @Headers("Accept: application/json")
    @RequestLine("GET /rest/api/latest/projects/{project}/repos/{repo}/commits/{commit}/pull-requests")
    String getPRforMergedCommit(@Param("project") String project,
                             @Param("repo") String repo,
                             @Param("commit") String commit)

    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /rest/api/latest/projects/{project}/repos/{repo}/archive?at={branch}&format=zip")
    Response getRepoZipArchive(@Param("project") String project,
                               @Param("repo") String repo,
                               @Param("branch") String branch)

    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /rest/api/latest/projects/{project}/repos/{repo}/archive?at={branch}&format=zip&path={filePath}")
    Response getRepoFileInZipArchive(@Param("project") String project,
                                     @Param("repo") String repo,
                                     @Param("branch") String branch,
                                     @Param("filePath") String filePath)

}