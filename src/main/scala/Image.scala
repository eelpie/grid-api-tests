import play.api.libs.json.{Json, Reads}

case class Metadata(title: Option[String], description: Option[String], byline: Option[String], credit: Option[String])

object Metadata {
  implicit val ir: Reads[Metadata] = Json.reads[Metadata]
}

case class UsageRights(category: Option[String], photographer: Option[String], publication: Option[String], supplier: Option[String])

object UsageRights {
  implicit val urr: Reads[UsageRights] = Json.reads[UsageRights]
}

case class URILink(uri: String)

object URILink {
  implicit val lr: Reads[URILink] = Json.reads[URILink]
}

case class Source(file: String, size: Long, mimeType: String, dimensions: Dimensions)
object Source {
  implicit val sr: Reads[Source] = Json.reads[Source]
}

case class UsageRightsWrapper(uri: String, data: Option[UsageRights])

object UsageRightsWrapper {
  implicit val urwr: Reads[UsageRightsWrapper] = Json.reads[UsageRightsWrapper]
}

case class UserMetadata(usageRights: UsageRightsWrapper)

object UserMetadata {
  implicit val umwr: Reads[UserMetadata] = Json.reads[UserMetadata]
}

case class UserMetadataWrapper(
                                uri: String,
                                data: UserMetadata
                              )

object UserMetadataWrapper {
  implicit val umwr: Reads[UserMetadataWrapper] = Json.reads[UserMetadataWrapper]
}

case class Image(id: String,
                 metadata: Metadata,
                 usageRights: UsageRights,
                 fileMetadata: URILink,
                 usages: ImageUsageField,
                 syndicationStatus: String,
                 source: Source,
                 exports: Seq[Crop],
                 userMetadata: UserMetadataWrapper)

object Image {
  implicit val ir: Reads[Image] = Json.reads[Image]
}