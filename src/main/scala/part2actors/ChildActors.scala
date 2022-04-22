package part2actors

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import part2actors.ChildActors.Parent.{
  CreateChild,
  StopChild,
  TellChild,
  WatchChild
}

object ChildActors extends App {

  object Parent {
    trait Command

    case class CreateChild(name: String) extends Command
    case class TellChild(name: String, message: String) extends Command
    case class StopChild(name: String) extends Command
    case class WatchChild(name: String) extends Command

    def apply(): Behavior[Command] = active(Map())

    def active(children: Map[String, ActorRef[String]]): Behavior[Command] =
      Behaviors
        .receive[Command] { (context, message) =>
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
            case StopChild(name) =>
              context.log.info(
                s"[parent] Stopping child "
              )
              children
                .get(name)
                .fold(
                  context.log.warn(s"[parent] Billy Jean $name is not my son")
                )(context.stop)
              active(children - name)
            case WatchChild(name) =>
              context.log.info(
                s"[parent] Stopping child "
              )
              children
                .get(name)
                .fold(
                  context.log.warn(s"[parent] Billy Jean $name is not my son")
                )(context.watch)
              Behaviors.same
          }
        }
        .receiveSignal { case (context, Terminated(childRefWhichDied)) =>
          context.log.info(
            s"[parent] Child ${childRefWhichDied.path} was killed"
          )
          val childName = childRefWhichDied.path.name
          active(children - childName)
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

      val child1 = "Eduardo"

      parent ! CreateChild(child1)
      parent ! CreateChild("das_second_child")

      parent ! TellChild(child1, "are you there?")
      parent ! TellChild("orphan", "are you there?")
      parent ! TellChild("das_second_child", "you are das second?")

      parent ! WatchChild(child1)

      parent ! StopChild(child1)
      parent ! TellChild(child1, "are you there?")

      Behaviors.empty
    }

    val parent = ActorSystem(userGuardianBehavior, "DemoParentChild")

    Thread.sleep(1000)

    parent.terminate()
  }

  demoParent()

}
