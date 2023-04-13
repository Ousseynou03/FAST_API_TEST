package FAST

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.core.session
import io.gatling.http.Predef._

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



  val numCaisse: Iterator[String] = Iterator.from(1).map(i => f"${(i-1) % 999 + 1}%03d")

  val numSequence: Iterator[String] = Iterator.from(1).map(i => f"${(i - 1) % 999 + 1}%03d")


  val startDateTime = LocalDateTime.parse("2023-04-03T07:00:01")
  // Définir le format de date et heure ISO 8601
  val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  // Définir l'itérateur qui incrémente la date et l'heure de 1 seconde à chaque itération
  val dateTime: Iterator[String] = Iterator.iterate(startDateTime)(_.plusSeconds(1))
    .map(dateTimeFormatter.format)


  val scnFastApi = scenario("GET CUSTOMER FAST API")
    .exec(flushSessionCookies)
    .exec(flushHttpCache)
    .exec(flushCookieJar)
    .exec(session => session.set("code_magasin", 3050))
    .exec { session =>
      println("code_magasin :" + session("code_magasin").as[String])
      session
    }
    .exec{session =>
      val num_caisse : String = numCaisse.next()
      session.set("numero_caisse",num_caisse)
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
    .exec(
      http(" POST FAST API")
        .post("/ws/fast/tx6")
        .header("Content-Type", "application/xml")
        .header("Accept", "application/xml")
        .asXml
        .body(StringBody(
          """
            |<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            |<POSLog MajorVersion="6"
            |xsi:schemaLocation="http://www.nrf-arts.org/IXRetail/namespace/ POSLogV6.0.0.xsd"
            |xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            |xmlns="http://www.nrf-arts.org/IXRetail/namespace/">
            |<Transaction>
            |<BusinessUnit>
            |<UnitID TypeCode="RetailStore">${code_magasin}</UnitID>
            |</BusinessUnit>
            |<Channel>
            |<ChannelID>STORE</ChannelID>
            |<TouchPointID>POS</TouchPointID>
            |</Channel>
            |<WorkstationID>${numero_caisse}</WorkstationID>
            | <SequenceNumber>${numero_sequence}</SequenceNumber>
            |<OperatorID>156994</OperatorID>
            |<RetailTransaction>
            |<LineItem EntryMethod="Scanned">
            |<Sale ItemSubType="x:ORI3" ItemType="Stock">
            |<ItemID Name="SKU">54437934</ItemID>
            |<ItemID Name="Codabar">2022854437939</ItemID>
            |<MerchandiseHierarchy Level="Group">228</MerchandiseHierarchy>
            |<MerchandiseHierarchy Level="Department">2079</MerchandiseHierarchy>
            |<POSIdentity>
            |<POSItemID>3258541613075</POSItemID>
            |<Qualifier>1</Qualifier>
            |</POSIdentity>
            |<ActualSalesUnitPrice>30.00</ActualSalesUnitPrice>
            |<ExtendedAmount>30.00</ExtendedAmount>
            |<ExtendedDiscountAmount>24.0</ExtendedDiscountAmount>
            |<Quantity UnitOfMeasureCode="EA">1.0</Quantity>
            |<Associate>
            |<AssociateID>156994</AssociateID>
            |</Associate>
            |<Tax TaxType="VAT">
            |<Percent>20.0</Percent>
            |</Tax>
            |<SellingLocation>12345</SellingLocation>
            |</Sale>
            |<SequenceNumber>1</SequenceNumber>
            |</LineItem>
            |<LineItem>
            |<Tender TenderType="x:TTID1000CASH">
            |<Amount>10.0</Amount>
            |</Tender>
            |<SequenceNumber>2</SequenceNumber>
            |</LineItem>
            |<LineItem>
            |<Tender TenderType="x:TTID3030CCARD">
            |<Amount>20.0</Amount>
            |<Authorization HostAuthorized="false" PreAuthorizationFlag="false" VerifiedByPINFlag="false">
            |<AuthorizationCode>0000</AuthorizationCode>
            |<ReferenceNumber>0</ReferenceNumber>
            |<AuthorizingTermID>780</AuthorizingTermID>
            |<EMVDebug>
            |<CardholderVerificationMethodResults>E</CardholderVerificationMethodResults>
            |<TerminalVerificationResults>T</TerminalVerificationResults>
            |</EMVDebug>
            |</Authorization>
            |<CreditDebit CardType="Credit" TypeCode="Visa">
            |<IssuerIdentificationNumber>1</IssuerIdentificationNumber>
            |<PrimaryAccountNumber>000000000000000</PrimaryAccountNumber>
            |<ExpirationDate>2099-01</ExpirationDate>
            |<CreditCardCompanyCode>T 0</CreditCardCompanyCode>
            |</CreditDebit>
            |</Tender>
            |<SequenceNumber>3</SequenceNumber>
            |</LineItem>
            |<Total TotalType="TransactionGrandAmount">30.0</Total>
            |<LoyaltyAccount>
            |<CustomerID>{{client_id:407190620}}</CustomerID>
            |</LoyaltyAccount>
            |</RetailTransaction>
            |<EndDateTime>{{date_time:${nextDateTime}}</EndDateTime>
            |</Transaction>
            |</POSLog>
            |""".stripMargin)).asXml
        .check(status is 201)
    )
    .pause(TpsPausemin seconds,TpsPausemax seconds)
}
