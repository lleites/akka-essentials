package part2actors

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ActorState extends App {

  object WordCounter {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      var total = 0

      Behaviors.receiveMessage { message =>
        val words = message.split(" ")
        total += words.length
        context.log.info(s"current ${words.length} total $total")
        Behaviors.same
      }

    }

    def stateless(counter: Int): Behavior[String] = Behaviors.receive {
      (context, message) =>
        val words = message.split(" ")
        val updatedCounter = counter + words.length
        context.log.info(s"current ${words.length} total $updatedCounter")
        stateless(updatedCounter)

    }

  }

  def testBehaviorWordCounter(behavior: Behavior[String]) = {
    val actorSystem = ActorSystem(behavior, "FirstActorSystem")

    actorSystem ! "Sun is shinning"
    actorSystem ! "Akka is bad"
    actorSystem ! "Hi how are you?"

    actorSystem.terminate()
  }

  trait SimpleThing
  case object EatChocolate extends SimpleThing
  case object LearnAkka extends SimpleThing
  case object CleanTheFloor extends SimpleThing

  object SimpleHuman {
    def apply(): Behavior[SimpleThing] = Behaviors.setup { context =>
      var totalHappiness = 0

      Behaviors.receiveMessage { message =>
        message match {
          case EatChocolate  => totalHappiness += 1
          case LearnAkka     => totalHappiness += 1
          case CleanTheFloor => totalHappiness -= 1
        }
        context.log.info(s"Happiness $totalHappiness")
        Behaviors.same
      }

    }
    def stateless(totalHappiness: Int): Behavior[SimpleThing] =
      Behaviors.receive { (context, message) =>
        val updatedHappiness = message match {
          case EatChocolate  => totalHappiness + 1
          case LearnAkka     => totalHappiness + 1
          case CleanTheFloor => totalHappiness - 1
        }
        context.log.info(s"Happiness $updatedHappiness")
        stateless(updatedHappiness)
      }

  }

  def testBehaviorSimple(behavior: Behavior[SimpleThing]) = {
    val actorSystem = ActorSystem(behavior, "FirstActorSystem")

    actorSystem ! EatChocolate
    actorSystem ! CleanTheFloor
    actorSystem ! LearnAkka

    actorSystem.terminate()
  }

  testBehaviorWordCounter(WordCounter.stateless(12))
  // testBehaviorSimple(SimpleHuman.stateless(100))
}
