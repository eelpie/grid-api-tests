import play.api.libs.json.{Json, Reads}

case class Metadata(title: Option[String], description: Option[String])

object Metadata {
  implicit val ir: Reads[Metadata] = Json.reads[Metadata]
}

case class Image(id: String, metadata: Metadata)

object Image {
  implicit val ir: Reads[Image] = Json.reads[Image]
}