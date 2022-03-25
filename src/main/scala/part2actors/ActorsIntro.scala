package part2actors

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object ActorsIntro extends App {

  def testBehavior(behavior: Behavior[String]) = {
    val actorSystem = ActorSystem(behavior, "FirstActorSystem")

    actorSystem ! "Sun is shinning"
    actorSystem ! "Akka is bad"
    actorSystem ! "Hi how are you?"

    actorSystem.terminate()
  }

  def testBehaviorMessage(behavior: Behavior[BetterActor.Message]) = {
    val actorSystem = ActorSystem(behavior, "FirstActorSystem")

    actorSystem ! BetterActor.IntMessage(1)
    actorSystem ! BetterActor.StringMessage("Hi how are you?")

    actorSystem.terminate()
  }

  val simpleActorBehavior: Behavior[String] = Behaviors.receiveMessage {
    (message: String) =>
      println(s"[simple actor] I have received: $message")
      Behaviors.same
  }

  object SimpleActor {
    def apply(): Behavior[String] = Behaviors.receiveMessage {
      (message: String) =>
        println(s"[simple actor] I have received: $message")
        Behaviors.same
    }
  }

  object SimpleActor_V2 {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[simple actor] I have received: $message")
      Behaviors.same

    }
  }

  object SimpleActor_V3 {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      // Some private code

      Behaviors.receiveMessage { (message: String) =>
        println(s"[simple actor] I have received: $message")
        Behaviors.same
      }

    }
  }

  object Person {
    def happy(): Behavior[String] = Behaviors.receive {
      (context: ActorContext[String], message: String) =>
        message match {
          case "Akka is bad" =>
            context.log.info(s"$message, That makes me sad")
            sad()
          case _ =>
            context.log.info(s"I have received: $message , thats great!")
            Behaviors.same
        }

    }
    def sad(): Behavior[String] = Behaviors.receive {
      (context: ActorContext[String], message: String) =>
        context.log.info(s"I have received: $message , this party is shit!")
        message match {
          case "Akka is good" => happy()
          case _              => Behaviors.same
        }
    }

    def apply(): Behavior[String] = happy()
  }

  object BetterActor {
    trait Message
    case class IntMessage(number: Int) extends Message
    case class StringMessage(text: String) extends Message

    def apply(): Behavior[Message] = Behaviors.receive {
      (context: ActorContext[Message], message: Message) =>
        message match {
          case IntMessage(number: Int) =>
            context.log.info(s"Received Int : $number")
          case StringMessage(text: String) =>
            context.log.info(s"Received String: $text")
        }
        Behaviors.same

    }

  }

  // testBehavior(Person.happy())
  testBehaviorMessage(BetterActor())

}
