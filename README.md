![ODS Document Generation Service tests](https://github.com/opendevstack/ods-document-generation-svc/workflows/Document%20Generation%20Service/badge.svg)

# ODS Document Generation Service

A document generation service that transforms document templates in a remote Bitbucket repository into PDF documents.

## Distribute
TODO:
- Udate documentation:
- Add the need of docker to develop it (to test it).
```
make shadowJar
```

## Run

```
make run
```

## Test

```
make test
```

## Document Templates

When processing a template `type` at a specific `version`, and data into a document, the DocGen service expects the BitBucket repository to have a `release/${version}` branch that contains the template type at `/templates/${type}.html.tmpl`.

## Requirements

### Packages

- [wkhtmltopdf](https://wkhtmltopdf.org/)

### Environment

- `BITBUCKET_URL`
- `BITBUCKET_USERNAME`
- `BITBUCKET_PASSWORD`
- `BITBUCKET_DOCUMENT_TEMPLATES_PROJECT`
- `BITBUCKET_DOCUMENT_TEMPLATES_REPO`

## Reference

This project is based on https://github.com/jooby-project/gradle-starter.
