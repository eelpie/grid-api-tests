import play.api.libs.json.{Json, Reads}

case class CollectionsResponse(data: CollectionNode)

object CollectionsResponse {
  implicit val cr: Reads[CollectionsResponse] = Json.reads[CollectionsResponse]

}

case class CollectionNode(basename: String, fullPath: Seq[String])

object CollectionNode {
  implicit val cnr: Reads[CollectionNode] = Json.reads[CollectionNode]
}