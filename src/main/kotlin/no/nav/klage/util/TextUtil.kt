package no.nav.klage.util

fun sanitizeText(input: String): String {
    var result = removeFEFF(input)
    result = remove0002(result)
    result = remove0003(result)
    result = remove0008(result)
    result = remove000B(result)
    result = remove000C(result)
    result = remove0016(result)
    return result
}

// Pdfgen does not validate text as valid pdf/a when this symbol is present.
// https://www.fileformat.info/info/unicode/char/feff/index.htm

private fun removeFEFF(input: String): String = input.replace(oldValue = "\uFEFF", newValue = "")

private fun remove0002(input: String): String = input.replace(oldValue = "\u0002", newValue = "")

private fun remove0003(input: String): String = input.replace(oldValue = "\u0003", newValue = "")

// Backspace, not accepted by pdfgen.
private fun remove0008(input: String): String = input.replace(oldValue = "\u0008", newValue = "")

private fun remove000B(input: String): String = input.replace(oldValue = "\u000B", newValue = "")

private fun remove000C(input: String): String = input.replace(oldValue = "\u000C", newValue = "")

private fun remove0016(input: String): String = input.replace(oldValue = "\u0016", newValue = "")
