# Changelog

## [3.0] - 2020-08-11

### Added
- Add tagging of doc gen image into jenkinsfile ([#36](https://github.com/opendevstack/ods-document-generation-svc/pull/36))
- Add better error messages for bitbucket adapter ([#33](https://github.com/opendevstack/ods-document-generation-svc/pull/33))
- Publish docgen svc to dockerhub ([#24](https://github.com/opendevstack/ods-document-generation-svc/issues/24))
- Add a /health endpoint to understand service health ([#4](https://github.com/opendevstack/ods-document-generation-svc/issues/4))
- Add Travis configuration ([#7](https://github.com/opendevstack/ods-document-generation-svc/pull/7))

### Changed
- Set default branch to master instead of production ([#27](https://github.com/opendevstack/ods-document-generation-svc/pull/27))
- Doc Gen service should shop its ocp config in openshift directory ([#20](https://github.com/opendevstack/ods-document-generation-svc/issues/20))
- Adjust document orientation ([#15](https://github.com/opendevstack/ods-document-generation-svc/issues/15))
- Adjust spacing between header and content ([#12](https://github.com/opendevstack/ods-document-generation-svc/issues/12))
- Upgrade to wkhtmltopdf 0.12.5 ([#11](https://github.com/opendevstack/ods-document-generation-svc/issues/11))

### Fixed
- doc gen yml - missing labels - make tailor in qs fail & move to github actions ([#23](https://github.com/opendevstack/ods-document-generation-svc/pull/23))
- Doc gen Service jenkins lacks Sonarqube config / scan ([#17](https://github.com/opendevstack/ods-document-generation-svc/issues/17))
- fix yml to adopt for quickstarter ([#22](https://github.com/opendevstack/ods-document-generation-svc/pull/22))
- Using HTML headers and footers causes large documents to fail ([#9](https://github.com/opendevstack/ods-document-generation-svc/issues/9))