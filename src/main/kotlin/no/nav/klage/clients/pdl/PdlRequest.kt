package no.nav.klage.clients.pdl

import java.net.URL

data class PersonGraphqlQuery(
    val query: String,
    val variables: IdentVariables
)

data class IdentVariables(
    val ident: String
)

fun hentPersonQuery(ident: String): PersonGraphqlQuery {
    val query =
        PersonGraphqlQuery::class.java.getResource("/pdl/hentPerson.graphql").cleanForGraphql()
    return PersonGraphqlQuery(query, IdentVariables(ident))
}

fun URL.cleanForGraphql() = readText().replace("[\n\r]", "")