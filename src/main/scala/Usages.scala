import play.api.libs.json.{Json, OFormat}


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

case class PrintUsage(mediaId: String, dateAdded: String, printUsageMetadata: PrintUsageMetadata, containerId: String, usageId: String,
                      usageStatus: String)

object PrintUsage {
  implicit val puw: OFormat[PrintUsage] = Json.format[PrintUsage]

}


case class PrintUsageSubmission(printUsageRecords: Seq[PrintUsage])

object PrintUsageSubmission {
  implicit val pusw: OFormat[PrintUsageSubmission] = Json.format[PrintUsageSubmission]
}


case class Usage(id: String, platform: String, status: String)
object Usage {
  implicit val ur = Json.reads[Usage]
}
case class Meh(data: Usage)
object Meh {
  implicit val mr = Json.reads[Meh]
}

case class UsagesResponse(length: Int, data: Seq[Meh])
object UsagesResponse {
  implicit val urr = Json.reads[UsagesResponse]
}
