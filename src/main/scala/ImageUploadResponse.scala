import play.api.libs.json.Json

case class ImageUploadResponse(uri: String)
object ImageUploadResponse{
  implicit val upr = Json.reads[ImageUploadResponse]
}
