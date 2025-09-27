import play.api.libs.json.{Json, Reads}


case class UploadStatus(status: String, errorMessage: Option[String])

object UploadStatus {
  implicit val upr: Reads[UploadStatus] = Json.reads[UploadStatus]
}

case class UploadStatusResponse(uri: String, data: UploadStatus)

object UploadStatusResponse {
  implicit val upsr: Reads[UploadStatusResponse] = Json.reads[UploadStatusResponse]
}