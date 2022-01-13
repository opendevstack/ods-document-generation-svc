package org.ods.shared.lib.leva.doc

class Constants {

    enum DocumentType {

        CSD,
        DIL,
        DTP,
        DTR,
        RA,
        CFTP,
        CFTR,
        IVP,
        IVR,
        SSDS,
        TCP,
        TCR,
        TIP,
        TIR,
        TRC,
        OVERALL_DTR,
        OVERALL_IVR,
        OVERALL_TIR

    }

    protected static Map DOCUMENT_TYPE_NAMES = [
            (DocumentType.CSD as String)        : 'Combined Specification Document',
            (DocumentType.DIL as String)        : 'Discrepancy Log',
            (DocumentType.DTP as String)        : 'Software Development Testing Plan',
            (DocumentType.DTR as String)        : 'Software Development Testing Report',
            (DocumentType.CFTP as String)       : 'Combined Functional and Requirements Testing Plan',
            (DocumentType.CFTR as String)       : 'Combined Functional and Requirements Testing Report',
            (DocumentType.IVP as String)        : 'Configuration and Installation Testing Plan',
            (DocumentType.IVR as String)        : 'Configuration and Installation Testing Report',
            (DocumentType.RA as String)         : 'Risk Assessment',
            (DocumentType.TRC as String)        : 'Traceability Matrix',
            (DocumentType.SSDS as String)       : 'System and Software Design Specification',
            (DocumentType.TCP as String)        : 'Test Case Plan',
            (DocumentType.TCR as String)        : 'Test Case Report',
            (DocumentType.TIP as String)        : 'Technical Installation Plan',
            (DocumentType.TIR as String)        : 'Technical Installation Report',
            (DocumentType.OVERALL_DTR as String): 'Overall Software Development Testing Report',
            (DocumentType.OVERALL_IVR as String): 'Overall Configuration and Installation Testing Report',
            (DocumentType.OVERALL_TIR as String): 'Overall Technical Installation Report',
    ]

    static GAMP_CATEGORY_SENSITIVE_DOCS = [
            DocumentType.SSDS as String,
            DocumentType.CSD as String,
            DocumentType.CFTP as String,
            DocumentType.CFTR as String
    ]

    static Map<String, Map> DOCUMENT_TYPE_FILESTORAGE_EXCEPTIONS = [
            'SCRR-MD' : [storage: 'pdf', content: 'pdf' ]
    ]

    static List<String> COMPONENT_TYPE_IS_NOT_INSTALLED = [
            MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_SAAS_SERVICE as String,
            MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_TEST as String,
            MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_LIB as String
    ]

    static Map<String, String> INTERNAL_TO_EXT_COMPONENT_TYPES = [
            (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_SAAS_SERVICE   as String) : 'SAAS Component',
            (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_TEST           as String) : 'Automated tests',
            (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_SERVICE        as String) : '3rd Party Service Component',
            (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_CODE           as String) : 'ODS Software Component',
            (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_INFRA          as String) : 'Infrastructure as Code Component',
            (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_LIB            as String) : 'ODS library component'
    ]

    public static String DEVELOPER_PREVIEW_WATERMARK = 'Developer Preview'
    public static String WORK_IN_PROGRESS_WATERMARK = 'Work in Progress'
    public static String WORK_IN_PROGRESS_DOCUMENT_MESSAGE = 'Attention: this document is work in progress!'

    public  static List referencedDcocs = [
            DocumentType.CSD,
            DocumentType.SSDS,
            DocumentType.RA,
            DocumentType.TRC,
            DocumentType.DTP,
            DocumentType.DTR,
            DocumentType.CFTP,
            DocumentType.CFTR,
            DocumentType.TIR,
            DocumentType.TIP,
    ]

}
