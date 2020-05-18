package klage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
class KlageArkiverJournalpostApplication

fun main() {
    runApplication<KlageArkiverJournalpostApplication>()
}