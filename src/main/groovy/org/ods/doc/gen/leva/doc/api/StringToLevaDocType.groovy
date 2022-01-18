package org.ods.doc.gen.leva.doc.api


import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class StringToLevaDocType implements Converter<String, LevaDocType> {

    @Override
    LevaDocType convert(String source) {
        try {
            return LevaDocType.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unkonw LevaDocType param: ${source}")
        }
    }

}
