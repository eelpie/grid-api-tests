import play.api.libs.json.{Json, Reads}

case class FileMetadata(colourModel: Option[String], iptc: Option[Map[String, String]])

object FileMetadata {
  implicit val fmdr: Reads[FileMetadata] = Json.reads[FileMetadata]
}
