package org.ods.doc.gen.core

class StringCleanup {

    static removeCharacters(inputString, characters) {
        def outputString = inputString
        characters.forEach { k, v ->
            outputString = outputString.replaceAll(k, v)
        }
        return outputString
    }

}
