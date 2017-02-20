import java.nio.file.{Paths, StandardOpenOption}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import spray.json._
import GeoJsonProtocol._
import akka.NotUsed
import akka.stream.{ActorMaterializer, IOResult}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}

import scala.concurrent.Future
import scala.concurrent.duration._

case class CoordinateRange(min: Double, max: Double)

object Flows {

  def geofenceFilterActor(geofenceFilterActor: ActorRef)(x: Coordinates)(implicit actorSystem: ActorSystem, timeout: Timeout) = geofenceFilterActor ? x
  def writeGeoJson(fileName: String): Sink[Coordinates, Future[IOResult]] = {
    Flow[Coordinates]
      .map(coordinates => s"""{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${coordinates.longitude},${coordinates.latitude}]}},""")
      .map(element => ByteString(s"${element}\n"))
      .toMat(FileIO.toPath(Paths.get(fileName), Set(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)))(Keep.right)
  }
}

object Main extends App {

  import system.dispatcher

  implicit val system = ActorSystem.create("PositionGenerator")
  implicit val materializer = ActorMaterializer()

  //      val positionCount = 60000000
  val positionCount = 100000

  val geofenceFilename = "uk-geofence.json"
  val outputFile = "test.json"

  val geofence: String = io.Source.fromInputStream(getClass.getResourceAsStream(geofenceFilename)).mkString
  val geofenceJson = geofence.parseJson.asJsObject
  val coordinates = geofenceJson.convertTo[Feature].coordinates
  val latitudeRange = CoordinateRange(coordinates.minBy(_.latitude).latitude, coordinates.maxBy(_.latitude).latitude)
  val longitudeRange = CoordinateRange(coordinates.minBy(_.longitude).longitude, coordinates.maxBy(_.longitude).longitude)

  implicit val timeout = Timeout(5.seconds)

  val concurrentGeofenceFilter = new ConcurrentGeofenceFilter(geofence)

  val geofenceFilter = Flows.geofenceFilterActor(system.actorOf(Props(new GeofenceFilterActor(geofence)), "geofenceFilter"))_
  val startTime = System.nanoTime()

  val positionStream = Source.fromGraph(CoordinateGeneratingSource(latitudeRange, longitudeRange))
    //    .mapAsyncUnordered(30)(x => (geofenceFilter(x)).mapTo[Tuple2[Coordinates, Boolean]])
    .mapAsyncUnordered(30)(x => (Future {concurrentGeofenceFilter.filter(x)}).mapTo[Tuple2[Coordinates, Boolean]])
    .filter(element => element._2)
    .take(positionCount)
    .map(tuple => tuple._1)
    .toMat(Flows.writeGeoJson(outputFile))(Keep.right).run()


  positionStream.onComplete { _ =>
    val endTime = System.nanoTime()
    println(s"Generated $positionCount Coordinates within defined geofence")
    println("Elapsed time: " + (endTime - startTime) / 1000000000.0 + "s")
    system.terminate()
  }
}
