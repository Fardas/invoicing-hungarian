package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import systems.enliven.invoicing.hungarian.behaviour.Guardian.Protocol.{FromDiscordBot, GetConnectionPool, SendMessageToDiscord}
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._

object Guardian extends Logger {

  def apply()(implicit configuration: Configuration): Behavior[Protocol.Command] =
    Behaviors.setup[Protocol.Message] {
      context =>
        val connectionPool: ActorRef[Connection.Protocol.Command] =
          context.spawn(createConnectionPool(configuration), "connection-pool")
        val discordBot: ActorRef[Guardian.Protocol.Command] = context.spawn(DiscordBot(configuration), "discord-bot")
        val repliesLeft: AtomicInteger = new AtomicInteger(0)

        Behaviors.withTimers {
          timers =>
            Behaviors.receiveMessage {
              case SendMessageToDiscord(message) =>
                repliesLeft.incrementAndGet()
                log.debug("Sending error to Discord!")
                discordBot ! Protocol.ToDiscordBot(context.self, message)
                Behaviors.same
              case FromDiscordBot(message) =>
                repliesLeft.decrementAndGet()
                log.debug(message)
                Behaviors.same
              case GetConnectionPool(replyTo) =>
                replyTo ! Protocol.ConnectionPool(connectionPool)
                Behaviors.same
              case Protocol.Shutdown =>
                log.debug("sent - received : " + repliesLeft.get())
                if(repliesLeft.get() != 0) {
                  timers.startSingleTimer(TimerKey, Protocol.Shutdown, 0.5.seconds)
                } else {
                  context.watchWith(connectionPool, Protocol.PoolShutdown)
                  log.debug("Connection pool is shutting down!")
                  context.stop(connectionPool)
                  timers.startSingleTimer(TimerKey, Protocol.ForceShutdown, 5.seconds)
                }
                Behaviors.same
              case Protocol.PoolShutdown =>
                Behaviors.stopped(() => log.debug("Guardian is shutting down gracefully!"))
              case Protocol.ForceShutdown =>
                Behaviors.stopped(() => log.debug("Guardian is shutting down forcefully!"))
            }
        }
    }.narrow

  private def createConnectionPool(
    configuration: Configuration
  ): Behavior[Connection.Protocol.Command] =
    Behaviors
      .supervise(ConnectionPool.apply(configuration))
      .onFailure[scala.Exception](SupervisorStrategy.restart)

  object Protocol {
    sealed trait Message
    sealed trait Command extends Message
    sealed trait PrivateCommand extends Message

    final case class GetConnectionPool(replyTo: ActorRef[ConnectionPool]) extends Command
    final case class ConnectionPool(pool: ActorRef[Connection.Protocol.Command])

    final case class SendMessageToDiscord(message: String) extends Command
    final case class ToDiscordBot(replyTo: ActorRef[Guardian.Protocol.Message], message: String) extends Command
    final case class FromDiscordBot(message: String) extends Command

    final case object Shutdown extends Command
    final case object PoolShutdown extends PrivateCommand
    final case object ForceShutdown extends PrivateCommand
  }

  final private case object TimerKey

}
