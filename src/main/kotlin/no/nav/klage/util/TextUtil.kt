package no.nav.klage.util


fun sanitizeText(input: String): String {
    return removeFEFF(input)
}

//Pdfgen does not validate text as valid pdf/a when this symbol is present.
//https://www.fileformat.info/info/unicode/char/feff/index.htm

private fun removeFEFF(input: String): String {
    return input.replace("\uFEFF", "")
}
