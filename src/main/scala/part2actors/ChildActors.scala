package part2actors

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import part2actors.ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {

  object Parent {
    trait Command

    case class CreateChild(name: String) extends Command

    case class TellChild(name: String, message: String) extends Command

    def apply(): Behavior[Command] = active(Map())

    def active(children: Map[String, ActorRef[String]]): Behavior[Command] =
      Behaviors.receive { (context, message) =>
        message match {
          case CreateChild(name) =>
            val childRef = context.spawn(Child(), name)
            active(children + (name -> childRef))
          case TellChild(name, message) =>
            context.log.info(
              s"[parent] Sending message $message  to child $name "
            )
            children
              .get(name)
              .fold(
                context.log.warn(s"[parent] Billy Jean $name is not my son")
              )(_ ! message)
            Behaviors.same
        }
      }

  }

  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(
        s"[${context.self.path.name}] received this message: $message"
      )
      Behaviors.same
    }
  }

  def demoParent(): Unit = {

    val userGuardianBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
      val parent = context.spawn(Parent(), "parent")
      parent ! CreateChild("child")
      parent ! CreateChild("das_second_child")

      parent ! TellChild("child", "are you there?")
      parent ! TellChild("orphan", "are you there?")
      parent ! TellChild("das_second_child", "you are das second?")

      Behaviors.empty
    }

    val parent = ActorSystem(userGuardianBehavior, "DemoParentChild")

    parent.terminate()
  }

  demoParent()

}
