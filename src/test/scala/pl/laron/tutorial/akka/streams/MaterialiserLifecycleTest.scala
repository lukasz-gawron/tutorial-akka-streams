package pl.laron.tutorial.akka.streams

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.testkit.{TestActor, TestKit, TestProbe}
import org.scalatest.FunSpecLike

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
  * https://blog.colinbreck.com/integrating-akka-streams-and-akka-actors-part-ii/
  * An extremely important aspect to understand is that the materialized stream is running as a set of actors
  * on the threads of the execution context on which they were allocated.
  * In other words, the stream is running independently from the actor that allocated it.
  * This becomes very important if the stream is long-running, or even infinite,
  * and we want the actor to manage the life-cycle of the stream,
  * such that when the actor stops, the stream is terminated.
  * Expanding on the example above, I will make the stream infinite and use a KillSwitch to manage the life-cycle of the stream.
  *
  * From ActorMaterializer docu.
  * The required [[akka.actor.ActorRefFactory]] (which can be either an [[akka.actor.ActorSystem]] or an [[akka.actor.ActorContext]])
  * * will be used to create one actor that in turn creates actors for the transformation steps.
  * *
  * * The materializer's [[akka.stream.ActorMaterializerSettings]] will be obtained from the
  * * configuration of the `context`'s underlying [[akka.actor.ActorSystem]].
  * *
  * * The `namePrefix` is used as the first part of the names of the actors running
  * * the processing steps. The default `namePrefix` is `"flow"`. The actor names are built up of
  * * `namePrefix-flowNumber-flowStepNumber-stepName`.
  */
class MaterialiserLifecycleTest extends TestKit(ActorSystem("EventStreamTest")) with FunSpecLike {
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  it("should continue stream when actor is killed, as materialiser is provided from outside scope") {
    val probe = TestProbe()
    val something = probe.childActorOf(Props(classOf[PrintSomeNumbersExternalMaterializer], materializer), "numbers")
    probe.setAutoPilot(new TestActor.AutoPilot {
      def run(sender: ActorRef, msg: Any): TestActor.AutoPilot =
        msg match {
          case x: String ⇒
            system.log.info("Probe received msg {}", x)
            TestActor.KeepRunning
        }
    })
    probe.receiveN(10, 11 seconds)

    //no assert for now, I've just analyse console output what is going on
    /**
      * [INFO] [10/21/2019 12:35:31.334] [EventStreamTest-akka.actor.default-dispatcher-2] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 1
      * [INFO] [10/21/2019 12:35:32.356] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 2
      * [INFO] [10/21/2019 12:35:33.343] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 3
      * [INFO] [10/21/2019 12:35:34.351] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 4
      * [INFO] [10/21/2019 12:35:35.351] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 5
      * [INFO] [10/21/2019 12:35:36.342] [EventStreamTest-akka.actor.default-dispatcher-2] [akka://EventStreamTest/system/testProbe-2/numbers] Receive timeout, stoppping actor
      * [INFO] [10/21/2019 12:35:36.352] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 6
      * [INFO] [10/21/2019 12:35:37.351] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 7
      * [INFO] [10/21/2019 12:35:38.350] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 8
      * [INFO] [10/21/2019 12:35:39.351] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 9
      * [INFO] [10/21/2019 12:35:40.350] [EventStreamTest-akka.actor.default-dispatcher-5] [akka.actor.ActorSystemImpl(EventStreamTest)] Probe received msg 10
      */
  }
}

