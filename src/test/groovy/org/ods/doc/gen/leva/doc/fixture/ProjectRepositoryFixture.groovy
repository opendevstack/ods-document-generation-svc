package org.ods.doc.gen.leva.doc.fixture

class ProjectRepositoryFixture {

    List load() {
        return [
                [
                        "id": "backend",
                        "type": "ods",
                        "data": [
                            "git": [
                                "branch": "master",
                                "commit": "138495888301232315f5455aeb17cc635982ba2c",
                                "createdExecutionCommit": "",
                                "baseTag": "ods-generated-v3.0-3.0-0b11-D",
                                "targetTag": "ods-generated-v3.0-3.0-0b11-D",
                            ]
                        ]
                ],
                [
                        "id": "frontend",
                        "type": "ods",
                        "data": [
                            "git": [
                                "branch": "master",
                                "commit": "3b5a62fbb2307d95360da386408b7f668f0e89ae",
                                "createdExecutionCommit": "",
                                "baseTag": "ods-generated-v3.0-3.0-0b11-D",
                                "targetTag": "ods-generated-v3.0-3.0-0b11-D",
                            ]
                        ]
                ],
                [
                        "id": "test",
                        "type": "ods-test",
                        "data": [
                            "git": [
                                "branch": "master",
                                "commit": "417c5b12c0af838cf0b843feb16ee5b7b1dab4ec",
                                "createdExecutionCommit": "",
                                "baseTag": "ods-generated-v3.0-3.0-0b11-D",
                                "targetTag": "ods-generated-v3.0-3.0-0b11-D",
                            ]
                        ]
                ]
        ]
    }

}
