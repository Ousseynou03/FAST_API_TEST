package FAST


import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.recorder.internal.bouncycastle.oer.its.ieee1609dot2.basetypes.Duration.seconds

import scala.language.postfixOps

class FAST_Transaction  extends  Simulation{

  private val host: String = System.getProperty("urlCible", "https://esbpprd1.galerieslafayette.ggl.inet:1609")
  private val VersionAppli: String = System.getProperty("VersionApp", "Vxx.xx.xx")
  private val TpsMonteEnCharge: Int = System.getProperty("tpsMonte", "4").toInt
  private val TpsPalier: Int = System.getProperty("tpsPalier", (2 * TpsMonteEnCharge).toString).toInt
  private val TpsPause: Int = System.getProperty("tpsPause", "60").toInt
  private val DureeMax: Int = System.getProperty("dureeMax", "1").toInt + 5 * (TpsMonteEnCharge + TpsPalier)

  private val LeCoeff: Int = System.getProperty("coeff", "10").toInt
  private val  nbVu : Int =  LeCoeff * 1

  // Volumétrie de charge cible
  private val maxTransactions: Int = 373479
  private val picDebut: Int = 17
  private val picFin: Int = 18
  private val peakDuration: Int = picFin - picDebut

  private val picMaxTransactions: Int = 50000
  private val picMinTransactions: Int = 12000
  private val picTransactionsAvecCustomerID: Int = (0.8 * picMaxTransactions).toInt
  private val picTransactionsSansCustomerID: Int = picMaxTransactions - picTransactionsAvecCustomerID
  private val picTransactionsParSecond: Double = picMaxTransactions.toDouble / (peakDuration * 3600)



  val httpProtocol =   http
    .baseUrl(host)
    .acceptHeader("application/json")


  before {

    println("----------------------------------------------" )
    println("host :"+ host   )
    println("VersionAppli :"+ VersionAppli   )
    println("nbVu : " + nbVu  )
    println("tpsMonte : " + peakDuration )
    println("----------------------------------------------" )
  }

  after  {
    println("----------------------------------------------" )
    println("--------     Rappel - Rappel - Rappel    -----" )
    println("VersionAppli :"+ VersionAppli   )
    println("host :"+ host   )
    println("TpsPause : " + TpsPause  )
    println("nbVu : " + nbVu  )
    println("DureeMax : " + DureeMax )
    println("tpsMonte : " + peakDuration )
    println("--------     Rappel - Rappel - Rappel    -----" )
    println("----------------------------------------------" )
    println(" " )
  }


  val FastApi = scenario("FAST TOPIC").exec(ObjectFAST.scnFastApi)


/*  setUp(
    FastApi.inject(
      rampUsersPerSec(picMinTransactions) to (picMaxTransactions) during (peakDuration hours),
      constantUsersPerSec(picTransactionsParSecond) during (24 hours),
      //Pic de charge maximal
      stressPeakUsers(picTransactionsAvecCustomerID) during (peakDuration hours),
      //Pic de charge minimal
      stressPeakUsers(picTransactionsSansCustomerID) during (peakDuration minutes)
    ).protocols(httpProtocol))
    .maxDuration(DureeMax minutes)*/

  setUp(
    FastApi.inject(rampUsers(nbVu * 10) during ( TpsMonteEnCharge  minutes) , nothingFor(  TpsPalier  minutes)),
  ).protocols(httpProtocol)
    .maxDuration(DureeMax minutes)
}
