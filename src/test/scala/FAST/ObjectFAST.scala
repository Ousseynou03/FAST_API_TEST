package FAST

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.session._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.language.postfixOps

object ObjectFAST {

  private val tpsPaceDefault: Int = System.getProperty("tpsPace", "1000").toInt
  private val tpsPacingProducts: Int = System.getProperty("tpsPaceProducts", tpsPaceDefault.toString).toInt

  private val TpsPausemin: Int = System.getProperty("tpsPause", "3").toInt
  private val TpsPausemax: Int = System.getProperty("tpsPause", "11").toInt
  private val TempoMillisecond: Int = System.getProperty("TempoMillisecond", "10").toInt

  private val NbreIterTransacDefault: Int = System.getProperty("nbIter", "3").toInt
  private val NbreIter: Int = System.getProperty("nbIterTransac", NbreIterTransacDefault.toString).toInt

  //Variable aléatoire de numCaisse selon le format 001(001 -> 999)
  val numCaisse: Iterator[String] = Iterator.from(1).map(i => f"${(i-1) % 999 + 1}%03d")

  //VAriable aléatoire de numSequence selon le format 001(001 -> 999)
  val numSequence: Iterator[String] = Iterator.from(1).map(i => f"${(i - 1) % 999 + 1}%03d")

  val startDateTime = LocalDateTime.parse("2023-04-03T07:00:01")
  val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  val dateTime: Iterator[String] = Iterator.iterate(startDateTime)(_.plusSeconds(1)).map(dateTimeFormatter.format)

  val filePath = "data/file.xml" // Chemin du fichier XML


  val scnFastApi = scenario("GET CUSTOMER FAST API")
    .exec(flushSessionCookies)
    .exec(flushHttpCache)
    .exec(flushCookieJar)
    .exec(session => session.set("code_magasin", 3050))
    .exec { session =>
      println("code_magasin :" + session("code_magasin").as[String])
      session
    }
    .exec { session =>
      val num_caisse: String = numCaisse.next()
      session.set("numero_caisse", num_caisse)
    }
    .exec { session =>
      println("numero_caisse : " + session("numero_caisse").as[String])
      session
    }
    .exec { session =>
      val num_seq: String = numSequence.next()
      session.set("numero_sequence", num_seq)
    }
    .exec { session =>
      println("numero_sequence : " + session("numero_sequence").as[String])
      session
    }
    .exec { session =>
      val nextDateTime: String = dateTime.next()
      session.set("nextDateTime", nextDateTime)
    }
    .exec { session =>
      println("nextDateTime : " + session("nextDateTime").as[String])
      session
    }
    .exec(session => session.set("filePath", filePath))
    .exec(session => {
      val xmlContenu = session("filePath").as[String]
        .replace("${code_magasin}", session("code_magasin").as[String])
        .replace("${numero_caisse}", session("numero_caisse").as[String])
        .replace("${numero_sequence}", session("numero_sequence").as[String])
        .replace("${nextDateTime}", session("nextDateTime").as[String])
      session.set("xmlContenu", xmlContenu)
    })
    .exec(http("Upload POSLog XML")
      .post("/ws/fast/tx6")
      .basicAuth("<login_esb>", "<password>")
      .header("Content-Type", "multipart/form-data")
      .bodyPart(StringBodyPart("poslog", session => session("xmlContenu").as[String]).contentType("application/xml"))
      .check(status.is(201)))
    .pause(TpsPausemin seconds, TpsPausemax seconds)
}
