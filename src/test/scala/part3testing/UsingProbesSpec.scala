package part3testing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.UsingProbesSpec._

class UsingProbesSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A master actor" should {
    val workerProbe = testKit.createTestProbe[WorkerTask]()
    val externalProbe = testKit.createTestProbe[ExternalProtocol]()

    "register a worker" in {
      val master = testKit.spawn(Master())

      master ! Register(workerProbe.ref, externalProbe.ref)

      externalProbe.expectMessage(RegisterAck)
    }

    "send a task to the worker actor" in {
      val master = testKit.spawn(Master())

      master ! Register(workerProbe.ref, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)

      val taskString = "This is work"
      master ! Work(taskString, externalProbe.ref)

      workerProbe.expectMessage(
        WorkerTask(taskString, master.ref, externalProbe.ref)
      )

      // Mocking the interaction with the worker actor
      master ! WorkCompleted(3, externalProbe.ref)

      externalProbe.expectMessage(Report(3))

    }

    "aggregates data correctly" in {
      val master = testKit.spawn(Master())

      val mockedWorkerBehavior = Behaviors.receiveMessage[WorkerTask] {
        case WorkerTask(text, master, originalDestination) =>
          master ! WorkCompleted(text.split(" ").length, originalDestination)
          Behaviors.same
      }
      val mockedWorker =
        testKit.spawn(Behaviors.monitor(workerProbe.ref, mockedWorkerBehavior))

      master ! Register(mockedWorker.ref, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)

      val taskString = "This is work"
      val taskString2 = "This is more work"
      master ! Work(taskString, externalProbe.ref)
      master ! Work(taskString2, externalProbe.ref)

      workerProbe.expectMessage(
        WorkerTask(taskString, master.ref, externalProbe.ref)
      )
      workerProbe.expectMessage(
        WorkerTask(taskString2, master.ref, externalProbe.ref)
      )

      externalProbe.expectMessage(Report(3))
      externalProbe.expectMessage(Report(7))

    }

  }
}

object UsingProbesSpec {
  trait MasterProtocol
  case class Work(text: String, replyTo: ActorRef[ExternalProtocol])
      extends MasterProtocol
  case class WorkCompleted(
      count: Int,
      originalDestination: ActorRef[ExternalProtocol]
  ) extends MasterProtocol
  case class Register(
      workerRef: ActorRef[WorkerTask],
      replyTo: ActorRef[ExternalProtocol]
  ) extends MasterProtocol

  case class WorkerTask(
      text: String,
      master: ActorRef[MasterProtocol],
      originalDestination: ActorRef[ExternalProtocol]
  )

  trait ExternalProtocol
  case class Report(totalCount: Int) extends ExternalProtocol
  case object RegisterAck extends ExternalProtocol

  object Master {
    def apply(): Behavior[MasterProtocol] = Behaviors.receiveMessage {
      case Register(workerRef, replyTo) =>
        replyTo ! RegisterAck
        active(workerRef)
      case _ =>
        Behaviors.same

    }

    def active(
        workerRef: ActorRef[WorkerTask],
        totalCount: Int = 0
    ): Behavior[MasterProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case Work(text, replyTo) =>
          workerRef ! WorkerTask(text, context.self, replyTo)
          Behaviors.same
        case WorkCompleted(count, originalDestination) =>
          val newTotalCount = totalCount + count
          originalDestination ! Report(newTotalCount)
          active(workerRef, newTotalCount)
        case _ =>
          Behaviors.same
      }
    }
  }
}
