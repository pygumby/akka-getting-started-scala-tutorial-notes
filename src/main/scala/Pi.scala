import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration

sealed trait PiMessage
// `Calculate` -- sent to the `Master` actor to start the calculation
case object Calculate extends PiMessage
// `Work` -- sent from the `Master` actor to the `Worker` actors containing the work assignment
case class Work(start: Int, nrOfElements: Int) extends PiMessage
// `Result` -- sent from the `Worker` actors to the `Master` actor containing the result from the worker’s calculation
case class Result(value: Double) extends PiMessage
// `PiApproximation` -- sent from the Master actor to the Listener actor containing the the final pi result and how long
// the calculation took
case class PiApproximation(pi: Double, duration: Duration) extends PiMessage
