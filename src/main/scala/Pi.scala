import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._

// The design we are aiming for is to have one `Master` actor initiating the computation, creating a set of `Worker`
// actors. Then it splits up the work into discrete chunks, and sends these chunks to the different workers in a
// round-robin fashion. The master waits until all the workers have completed their work and sent back results for
// aggregation. When computation is completed the master sends the result to the `Listener`, which prints out the
// result.

sealed trait PiMessage
// `Calculate` – sent to the `Master` actor to start the calculation
case object Calculate extends PiMessage
// `Work` – sent from the `Master` actor to the `Worker` actors containing the work assignment
case class Work(start: Int, nrOfElements: Int) extends PiMessage
// `Result` – sent from the `Worker` actors to the `Master` actor containing the result from the worker’s calculation
case class Result(value: Double) extends PiMessage
// `PiApproximation` – sent from the Master actor to the Listener actor containing the the final pi result and how long
// the calculation took
case class PiApproximation(pi: Double, duration: Duration) extends PiMessage

class Worker extends Actor {
  def calculatePiFor(start: Int, numberOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + numberOfElements)) {
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    }
    acc
  }
  def receive = {
    case Work(start, nrOfElements) =>
      sender ! Result(calculatePiFor(start, nrOfElements))
  }
}

// `nrOfWorkers` – defining how many workers we should start up
// `nrOfMessages` – defining how many number chunks to send out to the workers
// `nrOfElements` – defining how big the number chunks sent to each worker should be
// Note that we are passing in a `ActorRef` to the `Master` actor. This is used to report the the final result to the
// outside world.
class Master(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, listener: ActorRef) extends Actor {
  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis
  // In the constructor of `Master`, we create a round-robin router to make it easier to spread out the work evenly
  // between the workers.
  val workerRouter = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

  def receive = {
    case Calculate =>
      for (i <- 0 until nrOfMessages) workerRouter ! Work(i * nrOfElements, nrOfElements)
    case Result(value) =>
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) {
        // Send the result to the listener
        listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
        // Stops this actor and all its supervised children – In this case it has one supervised actor, the router, and
        // this in turn has nrOfWorkers supervised actors.
        context.stop(self)
      }
  }
}
