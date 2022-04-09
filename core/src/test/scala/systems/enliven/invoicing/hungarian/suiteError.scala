package systems.enliven.invoicing.hungarian

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.funspec.AnyFunSpec
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.behaviour.{DiscordBot, Guardian}

import javax.xml.bind.DatatypeConverter
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class suiteError extends AnyFunSpec with invoicingSuite {

  val invoice: Invoices.Invoice =
    Invoices.Raw(Invoices.Operation.storno, DatatypeConverter.parseBase64Binary("something"))

  val validInvoices = (1 to 100).map(_ => invoice)

  describe("The request API") {

    it("should not be able to manage 0 invoices,") {
      val response = invoicing.invoices(Invoices(Nil), entity, 10.seconds)(10.seconds)

      response match {
        case Success(_) => fail
        case Failure(exception) =>
          log.error(
            "Failed with [{}] with message [{}]",
            exception.getClass.getName,
            exception.getMessage
          )
          exception.getMessage.contains("INVALID_REQUEST") should be(true)
          exception.getMessage.contains("SCHEMA_VIOLATION") should be(true)
      }
    }

    it("should be able to manage 100 invoices,") {
      val response = invoicing.invoices(Invoices(validInvoices), entity, 10.seconds)(10.seconds)

      response match {
        case Success(response) =>
          log.info(response.toString)
          response.result.errorCode should be(None)
          response.result.message should be(None)
        case Failure(exception) =>
          log.error(
            "Failed with [{}] with message [{}]",
            exception.getClass.getName,
            exception.getMessage
          )
          fail
      }
    }

    it("should not be able to manage 101 invoices,") {
      val response =
        invoicing.invoices(Invoices(validInvoices :+ invoice), entity, 10.seconds)(10.seconds)

      response match {
        case Success(_) => fail
        case Failure(exception) =>
          log.error(
            "Failed with [{}] with message [{}]",
            exception.getClass.getName,
            exception.getMessage
          )
          exception.getMessage.contains("INVALID_REQUEST") should be(true)
          exception.getMessage.contains("SCHEMA_VIOLATION") should be(true)
      }
    }

    it("should send error message to discord when failing to use api") {
      val testKit = ActorTestKit()
      val fakeLogin: String = "p7ju87uztghh87o"

      val probe = testKit.createTestProbe[Guardian.Protocol.Command]()
      DiscordBot.testActor = probe.ref
      eventually(
        assertThrows[core.Exception.InvalidSecurityUser](
          invoicing.validate(createEntity(passwordOverride = Some(fakeLogin)), 10.seconds)(10.seconds).get
        )
      )
      probe.expectMessage(Guardian.Protocol.FromDiscordBot("DISCORD GOT THE MESSAGE: Entity validation failed!"))
      testKit.shutdownTestKit()
    }
  }

}
