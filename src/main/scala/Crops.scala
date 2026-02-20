import org.joda.time.DateTime
import play.api.libs.json.{JodaReads, Json, Reads}

case class CropRequest(`type`: String = "crop", source: String, x: Int, y: Int, width: Int, height: Int, aspectRatio: Option[String] = None)

object CropRequest {
  implicit val crr = Json.writes[CropRequest]
}

case class Crop(
                 id: String,
                 author: String,
                 date: DateTime,
                 specification: CropSpecification,
                 assets: Seq[Asset]
                 // TODO all fields
               )

object Crop {

  import JodaReads._

  implicit val crr = Json.reads[Crop]
}

case class CropBounds(x: Int, y: Int, width: Int, height: Int)

object CropBounds {
  implicit val cpr: Reads[CropBounds] = Json.reads[CropBounds]
}

case class CropSpecification(uri: String, bounds: CropBounds)

object CropSpecification {
  implicit val cpr: Reads[CropSpecification] = Json.reads[CropSpecification]
}

case class Dimensions(width: Int, height: Int)
object Dimensions {
  implicit val cpr: Reads[Dimensions] = Json.reads[Dimensions]
}

case class Asset(file: String, mimeType: String, dimensions: Dimensions)

object Asset {
  implicit val ar: Reads[Asset] = Json.reads[Asset]
}
