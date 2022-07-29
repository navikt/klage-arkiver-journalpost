package no.nav.klage.util


fun sanitizeText(input: String): String {
    var result = removeFEFF(input)
    result = remove0002(result)
    result = remove0003(result)
    result = remove0008(result)
    result = remove000B(result)
    result = remove0016(result)
    return result
}

//Pdfgen does not validate text as valid pdf/a when this symbol is present.
//https://www.fileformat.info/info/unicode/char/feff/index.htm

private fun removeFEFF(input: String): String {
    return input.replace("\uFEFF", "")
}

private fun remove0002(input: String): String {
    return input.replace("\u0002", "")
}

private fun remove0003(input: String): String {
    return input.replace("\u0003", "")
}

//Backspace, not accepted by pdfgen.
private fun remove0008(input: String): String {
    return input.replace("\u0008", "")
}

private fun remove000B(input: String): String {
    return input.replace("\u000B", "")
}

private fun remove0016(input: String): String {
    return input.replace("\u0016", "")
}
