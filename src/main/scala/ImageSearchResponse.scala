import play.api.libs.json.Json

case class ImageSearchResponseItem(uri: String, data: Image)

object ImageSearchResponseItem {
  implicit val isrir = Json.reads[ImageSearchResponseItem]
}

case class ImageSearchResponse(data: Seq[ImageSearchResponseItem])

object ImageSearchResponse {
  implicit val isrr = Json.reads[ImageSearchResponse]
}