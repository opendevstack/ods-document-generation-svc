package org.ods.doc.gen.external.modules.git

import com.xlson.groovycsv.CsvParser
import com.xlson.groovycsv.PropertyMapper
import groovy.util.logging.Slf4j
import org.ods.doc.gen.leva.doc.services.StringCleanup
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.text.SimpleDateFormat

@Slf4j
@Service
class BitbucketTraceabilityUseCase {

    private static final String CSV_FILE = "source-code-review.csv"
    static final String CSV_FOLDER = "review"
    private static final int PAGE_LIMIT = 10
    protected static Map CHARACTER_REMOVEABLE = [
        '/': '/\u200B',
        '@': '@\u200B',
    ]

    private final BitbucketService bitbucketService
    private final Project project

    @Inject
    BitbucketTraceabilityUseCase(BitbucketService bitbucketService, Project project) {
        this.project = project
        this.bitbucketService = bitbucketService
    }

    /**
     * Create a CSV file that contains the following records
     * for every merge event into the integration branch of every ODS component:
     * @return absolutePath of the created file
     */
    List<Map> getPRMergeInfo(ProjectData projectData) {
        String csvFIleWithInfo = generateSourceCodeReviewFile(projectData)
        return readSourceCodeReviewFile(csvFIleWithInfo)
    }

    /**
     * Create a CSV file that contains the following records
     * for every merge event into the integration branch of every ODS component:
     * @return absolutePath of the created file
     */
    String generateSourceCodeReviewFile(ProjectData projectData) {
        String token = bitbucketService.getToken()
        File file = createReportFile(projectData)
        processRepositories(file, token)
        return file.absolutePath
    }

    /**
     * Read an existing csv file and parse the info to obtaining an structured List of data.
     *
     * @param filePath The csv
     * @return List of commits
     */
    @SuppressWarnings(['JavaIoPackageAccess'])
    List<Map> readSourceCodeReviewFile(String filePath) {
        def file = new File(filePath)
        def result = []
        def data = processCsv(
            file.text,
            Record.CSV,
            ['commitDate', 'author', 'reviewers', 'pullRequestUrl', 'commitSHA', 'component']
        ).findAll { it != null }

        def dataSize = data.size()
        for (def i = 0; i < dataSize; i++) {
            def info = data[i]
            result << getCommitInfo(info)
        }
        return result
    }

    
    private Map<String, Object> getCommitInfo(info) {
        def authorInfo = processCsvDeveloper(info.author)
        def commitInfo = [
            date: info.commitDate,
            authorName: removeCharacters(authorInfo, "name"),
            authorEmail: removeCharacters(authorInfo, "email"),
            reviewers: getReviewers(info),
            url: removeCharacters(info, "pullRequestUrl"),
            commit: info.commitSHA,
            component: info.component,
        ]
        return commitInfo
    }

    
    private List getReviewers(PropertyMapper info) {
        def reviewers = []
        List infoReviewers = info.reviewers.split(Record.REVIEWERS_DELIMITER).findAll { it != null }
        def dataSize = infoReviewers.size()
        for (def i = 0; i < dataSize; i++) {
            def infoReviewer = infoReviewers[i]
            PropertyMapper reviewerInfo = processCsvDeveloper(infoReviewer)
            String reviewerNameValue = removeCharacters(reviewerInfo, "name")
            String reviewerEmailValue = removeCharacters(reviewerInfo, "email")
            Map infoMap = [reviewerName: reviewerNameValue, reviewerEmail: reviewerEmailValue,]
            reviewers.add(infoMap)
        }
        return reviewers
    }

    
    private String removeCharacters(PropertyMapper propertyMapper, String columnName) {
        String answer
        try {
            answer = StringCleanup.removeCharacters(propertyMapper.getProperty(columnName), CHARACTER_REMOVEABLE)
        } catch (Exception exception) {
            answer = "N/A"
        }
        return answer
    }

    @SuppressWarnings(['JavaIoPackageAccess'])
    private File createReportFile(ProjectData projectData) {
        File file = new File("${projectData.data.env.WORKSPACE}/${CSV_FOLDER}/${CSV_FILE}")
        if (file.exists()) {
            file.delete()
        }
        file.getParentFile().mkdirs()
        file.createNewFile()
        return file
    }

    
    private void processRepositories(File file, String token) {
        List<Map> repos = getRepositories()
        int reposSize = repos.size()
        for (def i = 0; i < reposSize; i++) {
            def repo = repos[i]
            processRepo(token, repo, file)
        }
    }

    
    private List<Map> getRepositories() {
        List<Map> result = []
        List<Map> repos = this.project.getRepositories()
        int reposSize = repos.size()
        for (def i = 0; i < reposSize; i++) {
            def repository = repos[i]
            result << [repo: "${project.data.metadata.id.toLowerCase()}-${repository.id}", branch: repository.branch]
        }
        return result
    }

    
    private void processRepo(String token, Map repo, File file) {
        boolean nextPage = true
        int nextPageStart = 0
        while (nextPage) {
            Map commits = bitbucketService.getCommitsForIntegrationBranch(repo.repo, PAGE_LIMIT, nextPageStart)
            if (commits.isLastPage) {
                nextPage = false
            } else {
                nextPageStart = commits.nextPageStart
            }
            processCommits(repo, commits, file)
        }
    }

    
    private void processCommits(Map repo, Map commits, File file) {
        commits.values.each { commit ->
            Map mergedPR = bitbucketService.getPRforMergedCommit(repo.repo, commit.id)
            // Only changes in PR and destiny integration branch
            if (mergedPR.values
                && mergedPR.values[0].toRef.displayId == repo.branch) {
                def record = new Record(getDateWithFormat(commit.committerTimestamp),
                    getAuthor(commit.author),
                    getReviewers(mergedPR.values[0].reviewers),
                    mergedPR.values[0].links.self[(0)].href,
                    commit.id,
                    repo.repo)
                writeCSVRecord(file, record)
            }
        }
    }

    
    private void writeCSVRecord(File file, Record record) {
        // Jenkins has his own idea how to concatenate Strings
        // Nor '' + '', nor "${}${}", nor StringBuilder nor StringBuffer works properly to
        // get a record entry set in an only String, this is the best approach that works as expected.
        file << record.commitDate
        file << record.CSV
        file << record.author.name
        file << record.author.FIELD_SEPARATOR
        file << record.author.mail
        file << record.CSV
        record.reviewers.each { reviewer ->
            file << reviewer.name
            file << reviewer.FIELD_SEPARATOR
            file << reviewer.mail
            if (reviewer != record.reviewers.last()) {
                file << record.REVIEWERS_DELIMITER
            }
        }
        file << record.CSV
        file << record.mergeRequestURL
        file << record.CSV
        file << record.mergeCommitSHA
        file << record.CSV
        file << record.componentName
        file << record.END_LINE
    }

    
    private Developer getAuthor(Map author) {
        return new Developer(
            author.name,
            author.emailAddress)
    }

    
    private List getReviewers(List reviewers) {
        List<Developer> approvals = []
        reviewers.each {
            if (it.approved) {
                approvals << new Developer(
                    it.user.name,
                    it.user.emailAddress)
            }
        }

        return approvals
    }

    
    private String getDateWithFormat(Long timestamp) {
        Date dateObj =  new Date(timestamp)
        return new SimpleDateFormat('yyyy-MM-dd', Locale.getDefault()).format(dateObj)
    }

    
    private Iterator processCsv(String data, String separator, List<String> columnNames) {
        Map parseParams = [separator: separator, readFirstLine: true, columnNames: columnNames, ]
        return CsvParser.parseCsv(parseParams, data)
    }

    
    private Object processCsvDeveloper(String data) {
        return processCsv(data, Developer.FIELD_SEPARATOR, ['name', 'email']).next()
    }

    private class Record {

        static final String CSV = ','
        static final String REVIEWERS_DELIMITER = ';'
        static final String END_LINE = '\n'

        String commitDate
        Developer author
        List<Developer> reviewers
        String mergeRequestURL
        String mergeCommitSHA
        String componentName

        @SuppressWarnings(['ParameterCount'])
        Record(String date, Developer author, List<Developer> reviewers, String mergeRequestURL,
               String mergeCommitSHA, String componentName) {
            this.commitDate = date
            this.author = author
            this.reviewers = reviewers
            this.mergeRequestURL = mergeRequestURL
            this.mergeCommitSHA = mergeCommitSHA
            this.componentName = componentName
        }

    }

    private class Developer {

        static final String FIELD_SEPARATOR = '|'
        String name
        String mail

        Developer(String name, String mail) {
            this.name = name
            this.mail = mail
        }

    }

}
