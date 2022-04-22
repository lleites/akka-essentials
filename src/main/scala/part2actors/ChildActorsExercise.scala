package part2actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object ChildActorsExercise extends App {

  trait MasterProtocol
  trait WorkerProtocol
  trait UserProtocol

  // master messages
  case class Initialize(nChildren: Int) extends MasterProtocol
  case class WordCountTask(text: String, replyTo: ActorRef[UserProtocol])
      extends MasterProtocol
  case class WordCountReply(id: Int, count: Int) extends MasterProtocol

  // worker messages
  case class WorkerTask(id: Int, text: String) extends WorkerProtocol

  // user messages
  case class Reply(count: Int) extends UserProtocol

  object WordCounterMaster {
    def apply(): Behavior[MasterProtocol] = Behaviors.receive {
      (context, message) =>
        message match {
          case Initialize(nChildren) =>
            context.log.info(s"[master] initializing with $nChildren children")
            val childRefs = for {
              i <- 1 to nChildren
            } yield context.spawn(WordCounterWorker(context.self), s"worker$i")
            active(childRefs, 0, 0, Map())
        }

    }

    def active(
        childRefs: Seq[ActorRef[WorkerProtocol]],
        currentChildIndex: Int,
        currentTaskId: Int,
        requestMap: Map[Int, ActorRef[UserProtocol]]
    ): Behavior[MasterProtocol] =
      Behaviors.receive { (context, message) =>
        message match {
          case WordCountTask(text, replyTo) =>
            context.log.info(
              s"[master] I've received $text - I will send it to the child $currentChildIndex"
            )
            val task = WorkerTask(currentTaskId, text)
            val childRef = childRefs(currentChildIndex)

            childRef ! task

            val nextChildIndex = (currentChildIndex + 1) % childRefs.length
            val nextTaskId = currentTaskId + 1
            val newRequestMap = requestMap + (currentTaskId -> replyTo)

            active(childRefs, nextChildIndex, nextTaskId, newRequestMap)
          case WordCountReply(id, count) =>
            context.log.info(
              s"[master] I've received a reply for task id $id with $count"
            )
            val originalSender = requestMap(id)

            originalSender ! Reply(count)

            active(childRefs, currentChildIndex, currentTaskId, requestMap - id)
        }
      }

  }
  object WordCounterWorker {
    def apply(masterRef: ActorRef[MasterProtocol]): Behavior[WorkerProtocol] =
      Behaviors.receive { (context, message) =>
        message match {
          case WorkerTask(id, text) =>
            context.log.info(
              s"[${context.self.path}] I've received task $id with '$text''"
            )
            val result = text.split(" ").length

            masterRef ! WordCountReply(id, result)

            Behaviors.same
        }
      }
  }

  object Aggregator {
    def apply(): Behavior[UserProtocol] = active()

    def active(totalWords: Int = 0): Behavior[UserProtocol] =
      Behaviors.receive { (context, message) =>
        message match {
          case Reply(count) =>
            context.log.info(
              s"[aggregator] I've received $count, total is $totalWords"
            )
            active(totalWords + count)
        }

      }

  }

  def testWordCounter(): Unit = {
    val userGuardian: Behavior[Unit] = Behaviors.setup { context =>
      val aggregator = context.spawn(Aggregator(), "aggregator")
      val wcm = context.spawn(WordCounterMaster(), "master")
      wcm ! Initialize(3)
      wcm ! WordCountTask("I love Akka", aggregator)
      wcm ! WordCountTask("Scala is super dope", aggregator)
      wcm ! WordCountTask("yes it is", aggregator)
      wcm ! WordCountTask("Testing round robin scheduling", aggregator)

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "WordCounting")
    Thread.sleep(1000)
    system.terminate()
  }

  testWordCounter()
}
