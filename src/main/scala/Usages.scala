import org.joda.time.DateTime
import play.api.libs.json.{JodaReads, JodaWrites, Json, OFormat, Reads}

case class PrintUsageMetadata(
                               issueDate: String,
                               sectionCode: String,
                               sectionName: String,
                               storyName: String,
                               pageNumber: Int,
                               publicationCode: String,
                               publicationName: String,
                             )

object PrintUsageMetadata {
  implicit val pumf: OFormat[PrintUsageMetadata] = Json.format[PrintUsageMetadata]

}

case class PrintUsage(mediaId: String, dateAdded: DateTime, printUsageMetadata: PrintUsageMetadata, containerId: String, usageId: String,
                      usageStatus: String)

object PrintUsage {
  import JodaWrites._
  import JodaReads._
  implicit val puw: OFormat[PrintUsage] = Json.format[PrintUsage]
}

case class DigitalMediaUsageMetadata(webUrl: String, webTitle: Option[String], sectionId: Option[String])
object DigitalMediaUsageMetadata {
  implicit val dmumf: OFormat[DigitalMediaUsageMetadata] = Json.format[DigitalMediaUsageMetadata]
}
case class DigitalMediaUsage(mediaId: String, dateAdded: DateTime, usageId: String, metadata: DigitalMediaUsageMetadata)
object DigitalMediaUsage {
  import JodaWrites._
  import JodaReads._
  implicit val dmuf = Json.format[DigitalMediaUsage]
}

case class DigitalMediaUsageSubmission(digitalMediaUsageRecords: Seq[DigitalMediaUsage])
object DigitalMediaUsageSubmission {
  implicit val dmusw: OFormat[DigitalMediaUsageSubmission] = Json.format[DigitalMediaUsageSubmission]
}


case class PrintUsageSubmission(printUsageRecords: Seq[PrintUsage])

object PrintUsageSubmission {
  implicit val pusw: OFormat[PrintUsageSubmission] = Json.format[PrintUsageSubmission]
}

case class SyndicationUsageSubmission(mediaId: String, dateAdded: DateTime, partnerName: String)

object SyndicationUsageSubmission {
  import JodaWrites._
  import JodaReads._
  implicit val susf: OFormat[SyndicationUsageSubmission] = Json.format[SyndicationUsageSubmission]
}

case class SyndicationUsageRequest(data: SyndicationUsageSubmission)
object SyndicationUsageRequest {
  implicit val surf: OFormat[SyndicationUsageRequest] = Json.format[SyndicationUsageRequest]
}

case class Usage(id: String, platform: String, status: String, dateAdded: DateTime)
object Usage {
  import JodaReads._
  implicit val ur: Reads[Usage] = Json.reads[Usage]
}
case class UsageResponseItem(data: Usage)
object UsageResponseItem {
  implicit val mr: Reads[UsageResponseItem] = Json.reads[UsageResponseItem]
}

case class UsagesResponse(length: Int, data: Seq[UsageResponseItem])
object UsagesResponse {
  implicit val urr: Reads[UsagesResponse] = Json.reads[UsagesResponse]
}

case class ImageUsageField(uri: String, data: Seq[UsageResponseItem])
object ImageUsageField {
  implicit val iufr: Reads[ImageUsageField] = Json.reads[ImageUsageField]
}