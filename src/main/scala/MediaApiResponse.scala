import play.api.libs.json.{Json, Reads}

case class Action(name: String, href: String, method: String)

object Action {
  implicit val ar: Reads[Action] = Json.reads[Action]
}

case class Link(rel: String, href: String)

object Link {
  implicit val lr: Reads[Link] = Json.reads[Link]
}

case class MediaApiResponse(links: Seq[Link], actions: Option[Seq[Action]])

object MediaApiResponse {
  implicit val mrr: Reads[MediaApiResponse] = Json.reads[MediaApiResponse]
}