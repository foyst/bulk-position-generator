import akka.actor.Actor
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import org.geotools.geojson.geom.GeometryJSON

object GeofenceFilterUtils {

  val geometryJson = new GeometryJSON()
  val geometryFactory = new GeometryFactory()
}

class ConcurrentGeofenceFilter(geofenceJson: String) {

  val geofence = GeofenceFilterUtils.geometryJson.read(geofenceJson)
  def filter(coordinates: Coordinates) = {

    val point = GeofenceFilterUtils.geometryFactory.createPoint(new Coordinate(coordinates.longitude, coordinates.latitude))
    (coordinates, point.within(geofence))
  }
}

class GeofenceFilterActor(geofenceJson: String) extends Actor {

  var geofence: Geometry = _

  override def preStart(): Unit = {

    super.preStart()
    geofence = GeofenceFilterUtils.geometryJson.read(geofenceJson)
  }

  override def receive: Receive = {

    case x: Coordinates =>

      //check coordinate fits in geofence
      val point = GeofenceFilterUtils.geometryFactory.createPoint(new Coordinate(x.longitude, x.latitude))
      val inGeofence = point.within(geofence)

      //append result & return to sender
      sender() ! (x, inGeofence)
  }
}