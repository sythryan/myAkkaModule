import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._

sealed trait PiMessage
case object Calculate extends PiMessage
case class Work(start: Int, nrOfElements: Int) extends PiMessage
case class Result(value: Double) extends PiMessage
case class PiApproximation(pi: Double, duration: Duration)


class Master(nrOfWorkers: Int, nrofMessages: Int, nrOfElements: Int, listener: actorRef) extends Actor {
  
  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

  def receive = {
    case Calculate =>
      for(i <- 0 until nrofMessages) workerRouter ! Work(i * nrOfElements, nrOfElements)
    case Result(value) =>
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrofMessages) {
        // Send the result to the listener
        listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
        //Stops this actor and all its supervised children
        context.stop(self)
      }
  }
}

class Worker extends Actor {
 
  def calculatePiFor(start: Int, nrOfElements: Int): Double ={
    var acc = 0.0
    for(i <- start until(start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }
   
  def receive = {
    case Work(start, nrOfElements) ⇒
      sender ! Result(calculatePiFor(start, nrOfElements)) // perform the work
  }

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

}