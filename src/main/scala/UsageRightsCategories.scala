import play.api.libs.json.{Json, Reads}

case class UsageRightsCategory(name: String, value: String, cost: String)

object UsageRightsCategory {
  implicit val urcr: Reads[UsageRightsCategory] = Json.reads[UsageRightsCategory]
}

case class UsageRightsCategoriesResponse(data: Seq[UsageRightsCategory])

object UsageRightsCategoriesResponse {
  implicit val urcrr: Reads[UsageRightsCategoriesResponse] = Json.reads[UsageRightsCategoriesResponse]
}