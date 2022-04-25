package part3testing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.EssentialTestingSpec.{
  BlackHole,
  FavoriteTech,
  SimpleActor,
  SimpleMessage,
  SimpleProtocol,
  SimpleReply,
  UppercaseString
}

import scala.concurrent.duration.DurationInt

class EssentialTestingSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {

  "A simple actor" should {
    "send back a duplicated message" in {
      val simpleActor = testKit.spawn(SimpleActor(), "simpleActor")
      val probe = testKit.createTestProbe[SimpleProtocol]()

      simpleActor ! SimpleMessage("Akka", probe.ref)

      probe.expectMessage(SimpleReply("AkkaAkka"))
    }
  }

  "A black hole actor" should {
    "not reply back" in {
      val blackHoleActor = testKit.spawn(BlackHole(), "blackHoleActor")

      val probe = testKit.createTestProbe[SimpleProtocol]()

      blackHoleActor ! SimpleMessage("Are you there?", probe.ref)
      blackHoleActor ! SimpleMessage("I know that you can hear me", probe.ref)
      blackHoleActor ! SimpleMessage("Is there anybody home?", probe.ref)

      probe.expectNoMessage(1.second)

    }
  }

  "A simple actor with a separated test suite" should {
    val simpleActor = testKit.spawn(SimpleActor(), "simpleActor2")
    val probe = testKit.createTestProbe[SimpleProtocol]()

    "uppercase a string" in {
      simpleActor ! UppercaseString("Akka", probe.ref)

      val message = probe.expectMessageType[SimpleReply]

      assert(
        message.contents == message.contents.toUpperCase()
      ) // Scala standard

      message.contents should be("AKKA") // Scala test
    }

    "reply with favorite tech as multiple messages" in {
      simpleActor ! FavoriteTech(probe.ref)
      val replies = probe.receiveMessages(2, 1.second)
      replies should contain only (
        SimpleReply("Scala"),
        SimpleReply("Akka")
      )
    }
  }

}

object EssentialTestingSpec {

  trait SimpleProtocol
  case class SimpleMessage(message: String, sender: ActorRef[SimpleProtocol])
      extends SimpleProtocol
  case class UppercaseString(message: String, sender: ActorRef[SimpleProtocol])
      extends SimpleProtocol
  case class FavoriteTech(replyTo: ActorRef[SimpleProtocol])
      extends SimpleProtocol
  case class SimpleReply(contents: String) extends SimpleProtocol

  object SimpleActor {
    def apply(): Behavior[SimpleProtocol] = Behaviors.receiveMessage {
      case SimpleMessage(message, sender) =>
        sender ! SimpleReply(message + message)
        Behaviors.same
      case UppercaseString(message, sender) =>
        sender ! SimpleReply(message.toUpperCase())
        Behaviors.same

      case FavoriteTech(replyTo) =>
        replyTo ! SimpleReply("Scala")
        replyTo ! SimpleReply("Akka")
        Behaviors.same
    }
  }

  object BlackHole {
    def apply(): Behavior[SimpleProtocol] = Behaviors.ignore
  }
}
