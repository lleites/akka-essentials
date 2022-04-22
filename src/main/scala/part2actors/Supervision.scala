package part2actors

import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object Supervision {

  object FussyWordCounter {
    def apply(): Behavior[String] = active()
    def active(total: Int = 0): Behavior[String] = Behaviors.receive {
      (context, message) =>
        val wordCount = message.split(" ").length
        context.log.info(
          s"Received piece of text: $message, counted $wordCount, total ${total + wordCount}"
        )
        if (message.startsWith("Q")) throw new RuntimeException("Starts with Q")
        if (message.startsWith("W")) throw new NullPointerException
        active(total + wordCount)
    }
  }

  def demoCrash(): Unit = {
    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyWordCounter = context.spawn(FussyWordCounter(), "fussyCounter")
      fussyWordCounter ! "Hello world!"
      fussyWordCounter ! "Quick! Hide"
      fussyWordCounter ! "Hello world!"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrash")
    Thread.sleep(1_000)
    system.terminate()

  }

  def demoWithParent(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup { context =>
      val child = context.spawn(FussyWordCounter(), "fussyChild")
      context.watch(child)
      Behaviors
        .receiveMessage[String] { message =>
          child ! message
          Behaviors.same
        }
        .receiveSignal { case (context, Terminated(childRef)) =>
          context.log.warn(s"Child failed ${childRef.path.name}")
          Behaviors.same
        }

    }

    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyCounter = context.spawn(parentBehavior, "fussyCounter")

      fussyCounter ! "Hello world!"
      fussyCounter ! "Quick! Hide"
      fussyCounter ! "Hello world!"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoWithParent")
    Thread.sleep(1_000)
    system.terminate()
  }

  def demoSupervisionWithRestart(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup { context =>
      val childBehavior = Behaviors
        .supervise(
          Behaviors
            .supervise(FussyWordCounter())
            .onFailure[NullPointerException](SupervisorStrategy.resume)
        )
        .onFailure[RuntimeException](SupervisorStrategy.restart)
      val child = context.spawn(childBehavior, "fussyChild")
      context.watch(child)
      Behaviors
        .receiveMessage[String] { message =>
          child ! message
          Behaviors.same
        }
        .receiveSignal { case (context, Terminated(childRef)) =>
          context.log.warn(s"Child failed ${childRef.path.name}")
          Behaviors.same
        }

    }

    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyCounter = context.spawn(parentBehavior, "fussyCounter")

      fussyCounter ! "Hello world!"
      fussyCounter ! "Quick! Hide"
      fussyCounter ! "Good bye world!"
      fussyCounter ! "What an NPE!"
      fussyCounter ! "Interesting!"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoWithParentWithRestart")
    Thread.sleep(1_000)
    system.terminate()

  }

  def main(args: Array[String]): Unit = {
    demoSupervisionWithRestart()
  }
}
