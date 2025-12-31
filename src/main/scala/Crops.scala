import org.joda.time.DateTime
import play.api.libs.json.{JodaReads, Json, Reads}

case class CropRequest(`type`: String = "crop", source: String, x: Int, y: Int, width: Int, height: Int)

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


case class Asset(mimeType: String)

object Asset {
  implicit val ar: Reads[Asset] = Json.reads[Asset]
}

/*
{"id":"800_800_3000_2000","author":"testkey","date":"2025-10-10T08:07:52.609Z",
  "specification":{"uri":"https://apitest2.griddev.eelpieconsulting.co.uk/media-api/images/1c5e15a1a0576c831039c071496dbc8fa1c761fb",
    "bounds":{"x":800,"y":800,"width":3000,"height":2000},"type":"crop"},
  "master":{"file":"https://eelpie-grid-crops.storage.googleapis.com/apitest2/1c5e15a1a0576c831039c071496dbc8fa1c761fb/800_800_3000_2000/master/3000.jpg",
  "size":1928987,"mimeType":"image/jpeg","dimensions":{"width":3000,"height":2000}},"assets":[{"file":"https://eelpie-grid-crops.storage.googleapis.com/apitest2/1c5e15a1a0576c831039c071496dbc8fa1c761fb/800_800_3000_2000/2000.jpg",
  "size":281699,"mimeType":"image/jpeg","dimensions":{"width":2000,"height":1333}},{"file":"https://eelpie-grid-crops.storage.googleapis.com/apitest2/1c5e15a1a0576c831039c071496dbc8fa1c761fb/800_800_3000_2000/1000.jpg",
  "size":88736,"mimeType":"image/jpeg","dimensions":{"width":1000,"height":667}},{"file":"https://eelpie-grid-crops.storage.googleapis.com/apitest2/1c5e15a1a0576c831039c071496dbc8fa1c761fb/800_800_3000_2000/500.jpg",
  "size":27293,"mimeType":"image/jpeg","dimensions":{"width":500,"height":333}},{"file":"https://eelpie-grid-crops.storage.googleapis.com/apitest2/1c5e15a1a0576c831039c071496dbc8fa1c761fb/800_800_3000_2000/140.jpg",
  "size":3690,"mimeType":"image/jpeg","dimensions":{"width":140,"height":93}},{"file":"https://eelpie-grid-crops.storage.googleapis.com/apitest2/1c5e15a1a0576c831039c071496dbc8fa1c761fb/800_800_3000_2000/3000.jpg",
  "size":554085,"mimeType":"image/jpeg","dimensions":{"width":3000,"height":2000}}]}
*/