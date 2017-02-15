import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import spray.json._
import GeoJsonProtocol._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

case class CoordinateRange(min: Double, max: Double)

object Main extends App {

  import system.dispatcher

  implicit val system = ActorSystem.create("PositionGenerator")
  implicit val materializer = ActorMaterializer()

  //    val positionCount = 60000000
  val positionCount = 10000

  val geofenceFilename = "uk-geofence.json"

  val geofence: String = io.Source.fromInputStream(getClass.getResourceAsStream(geofenceFilename)).mkString
  val geofenceJson = geofence.parseJson.asJsObject
  val coordinates = geofenceJson.convertTo[Feature].coordinates
  val latitudeRange = CoordinateRange(coordinates.minBy(_.latitude).latitude, coordinates.maxBy(_.latitude).latitude)
  val longitudeRange = CoordinateRange(coordinates.minBy(_.longitude).longitude, coordinates.maxBy(_.longitude).longitude)

  val geofenceFilterRef = system.actorOf(Props(new GeofenceFilter(geofence)), "geofenceFilter")

  implicit val timeout = Timeout(5.seconds)

  val startTime = System.nanoTime()
  val positionStream = Source.fromGraph(CoordinateGeneratingSource(latitudeRange, longitudeRange))
    .mapAsync(5)(x => (geofenceFilterRef ? x).mapTo[Tuple2[Coordinates, Boolean]])
    .filter(element => element._2)
    .take(positionCount)
    .runWith(Sink.foreach(println))

  positionStream.onComplete { _ =>
    val endTime = System.nanoTime()
    println(s"Generated $positionCount Coordinates within defined geofence")
    println("Elapsed time: " + (endTime - startTime) / 1000000000.0 + "s")
    system.terminate()
  }
}
