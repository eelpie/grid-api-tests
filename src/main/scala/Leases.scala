import org.joda.time.DateTime
import play.api.libs.json.{JodaReads, JodaWrites, Json, OFormat}


case class Lease(
                  id: String,
                  leasedBy: String,
                  access: String,
                  mediaId: String,
                  createdAt: DateTime,
                  active: Boolean
                )

object Lease {
  import JodaReads._
  import JodaWrites._

  implicit val lf: OFormat[Lease] = Json.format[Lease]
}

case class LeaseSubmission(mediaId: String, createdAt: DateTime, startDate: Option[DateTime] = None, endDate: Option[DateTime] = None, access: String, notes: Option[String] = None)

object LeaseSubmission {
  import JodaWrites._
  implicit val lsw = Json.writes[LeaseSubmission]
}