import akka.actor.Actor
import akka.actor.Actor.Receive
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import org.geotools.geojson.geom.GeometryJSON

class GeofenceFilter(geofenceJson: String) extends Actor {

  val geometryJson = new GeometryJSON()
  val geometryFactory = new GeometryFactory()
  var geofence: Geometry = _

  override def preStart(): Unit = {

    super.preStart()
    geofence = geometryJson.read(geofenceJson)
  }

  override def receive: Receive = {

    case x: Coordinates =>

      //check coordinate fits in geofence
      val point = geometryFactory.createPoint(new Coordinate(x.longitude, x.latitude))
      val inGeofence = point.within(geofence)

      //append result & return to sender
      sender() ! (x, inGeofence)
  }
}
