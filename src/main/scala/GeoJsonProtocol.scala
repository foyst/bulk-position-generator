import spray.json.{DefaultJsonProtocol, JsValue, JsonReader, RootJsonFormat}

case class Feature(featureType: String, coordinates: List[Coordinates])

case class Coordinates(longitude: Double, latitude: Double)

object GeoJsonProtocol extends DefaultJsonProtocol {

  implicit object CoordinatesFormat extends RootJsonFormat[Coordinates] {
    override def read(json: JsValue): Coordinates = {

      val latlon = json.convertTo[Vector[Double]]
      Coordinates(latlon(1), latlon(0))
    }
    override def write(obj: Coordinates): JsValue = ???
  }

  implicit object FeatureFormat extends JsonReader[Feature] {
    override def read(json: JsValue): Feature = {

      Feature(json.asJsObject.fields("type").convertTo[String],
        json.asJsObject.fields("coordinates").convertTo[List[List[Coordinates]]].flatten)
    }
  }
}
