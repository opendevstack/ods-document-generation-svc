package org.ods.doc.gen.project.data

import org.ods.doc.gen.leva.doc.services.Constants

class ProjectMessagesUtil {

    @SuppressWarnings('Instanceof')
    static String generateWIPIssuesMessage(ProjectData project) {
        def message = project.isWorkInProgress ? 'Pipeline-generated documents are watermarked ' +
            "'${Constants.WORK_IN_PROGRESS_WATERMARK}' " +
            'since the following issues are work in progress: ' :
            "The pipeline failed since the following issues are work in progress (no documents were generated): "

        project.getWipJiraIssues().each { type, keys ->
            def values = keys instanceof Map ? keys.values().flatten() : keys
            if (!values.isEmpty()) {
                message += '\n\n' + type.capitalize() + ': ' + values.join(', ')
            }
        }
        return message
    }
}
