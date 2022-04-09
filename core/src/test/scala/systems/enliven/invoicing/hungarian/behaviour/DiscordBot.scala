package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model._
import play.api.libs.json.{JsString, Json}
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/*
* Mock for DiscordBot
* For testing if Connection sends message to DiscordBot
* */
object DiscordBot extends Logger {
  // This actor will be a test probe ref from akka test kit
  var testActor: ActorRef[Guardian.Protocol.Command] = null
  def apply(configuration: Configuration): Behavior[Guardian.Protocol.Command] = {
    val token: String = configuration.get[String]("invoicing-hungarian.discord.bot-token")
    val discordApi: String = configuration.get[String]("invoicing-hungarian.discord.api")
    val serverId: String = configuration.get[String]("invoicing-hungarian.discord.server-id")
    val channelId: String = configuration.get[String]("invoicing-hungarian.discord.channel-id")
    val authorization = headers.Authorization(GenericHttpCredentials("Bot", token))

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    Behaviors.receiveMessage {
      case Guardian.Protocol.ToDiscordBot(replyTo, message) =>
        val responseFuture: Future[HttpResponse] = {
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.POST,
              uri = discordApi + "/channels/" + channelId + "/messages",
              entity = HttpEntity(ContentTypes.`application/json`,
                Json.stringify(Json.obj("content" -> JsString(message)))),
              headers = List(authorization)
            )
          )
        }
        responseFuture
          .onComplete {
            case Success(httpResponse) =>
              httpResponse.status match {
                case StatusCodes.OK =>
                  //testActor ! Guardian.Protocol.FromDiscordBot("DISCORD GOT THE MESSAGE: " + message)
                  replyTo ! Guardian.Protocol.FromDiscordBot("Successfully sent message to Discord!")
                case _ =>
                  //testActor ! Guardian.Protocol.FromDiscordBot("DISCORD DID NOT GET THE MESSAGE: " + message)
                  replyTo ! Guardian.Protocol.FromDiscordBot("Could not access Discord channel!")
              }
            case Failure(_) =>
              //testActor ! Guardian.Protocol.FromDiscordBot("DISCORD DID NOT GET THE MESSAGE: " + message)
              replyTo ! Guardian.Protocol.FromDiscordBot("Could not send message to Discord!")
          }
        testActor ! Guardian.Protocol.FromDiscordBot("DISCORD GOT THE MESSAGE: " + message)
        Behaviors.same
    }
  }
}


