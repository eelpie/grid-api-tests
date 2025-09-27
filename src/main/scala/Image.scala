import play.api.libs.json.{Json, Reads}

case class Metadata(title: Option[String], description: Option[String], byline: Option[String], credit: Option[String])

case class URILink(uri: String)

object URILink {
  implicit val lr: Reads[URILink] = Json.reads[URILink]
}

object Metadata {
  implicit val ir: Reads[Metadata] = Json.reads[Metadata]
}

case class Image(id: String, metadata: Metadata, fileMetadata: URILink)

object Image {
  implicit val ir: Reads[Image] = Json.reads[Image]
}