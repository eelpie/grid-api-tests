import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyReadables.readableAsJson
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.ws.{DefaultBodyWritables, StandaloneWSRequest}

import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}
import scala.concurrent.{Await, Future}

class GridApi(mediaApiUrl: String, apiKey: String) extends DefaultBodyWritables {

  private val reasonableWait: FiniteDuration = Duration(20, SECONDS)

  private val wsClient = {
    implicit val system: ActorSystem = ActorSystem()
    system.registerOnTermination {
      System.exit(0)
    }
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    StandaloneAhcWSClient()
  }

  def getServiceEndpoints: MediaApiResponse = {
    loadServiceIndexPage(mediaApiUrl)
  }

  def getImageLoaderEndpoints: MediaApiResponse = {
    val loaderLink = getServiceEndpoints.links.find(_.rel == "loader").get
    loadServiceIndexPage(loaderLink.href)
  }

  def getUsageEndpoints: MediaApiResponse = {
    val usageLink = getServiceEndpoints.links.find(_.rel == "usage").get
    loadServiceIndexPage(usageLink.href)
  }

  def getMetadataEndpoints: MediaApiResponse = {
    val editsLinks = getServiceEndpoints.links.find(_.rel == "edits").get
    loadServiceIndexPage(editsLinks.href)
  }

  def getUsages(imageId: String): UsagesResponse = {
    val url = getUsagesLink.replaceAll("\\{id}", imageId)
    val eventualResponse = authedGet(url)
    val response = Await.result(eventualResponse, reasonableWait)
    Json.parse(response.body).as[UsagesResponse]
  }

  private def getUsagesLink: String = {
    getUsageEndpoints.links.find(_.rel == "usages-by-media").map(_.href).get
  }


  private def getUsagePrintUsageAction: String = {
    getUsageEndpoints.actions.flatMap(_.find(_.name == "print-usage").map(_.href)).get
  }

  private def getUsageSyndicationUsageAction: String = {
    getUsageEndpoints.actions.flatMap(_.find(_.name == "syndication-usage").map(_.href)).get
  }

  private def getSetMetadataLink: String = {
    getMetadataEndpoints.links.find(_.rel == "metadata").map(_.href).get
  }

  def setMetadata(imageId: String, updatedMetadata: Map[String, String]): Unit = {
    val url = insertIdInto(getSetMetadataLink, imageId)

    val data = Map(
      "data" -> updatedMetadata
    )

    val eventualResponse = wsClient.url(url).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      put(Json.toJson(data))

    Await.result(eventualResponse, reasonableWait)
  }

  def addPrintUsage(printUsageSubmission: PrintUsageSubmission): Unit = {
    val eventualResponse = wsClient.url(getUsagePrintUsageAction).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(Json.toJson(printUsageSubmission))

    Await.result(eventualResponse, reasonableWait)
  }

  def addSyndicationUsage(syndicationUsageSubmission: SyndicationUsageSubmission): Unit = {
    val action = getUsageSyndicationUsageAction
    val eventualResponse = wsClient.url(action).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(Json.toJson(SyndicationUsageRequest(syndicationUsageSubmission)))

    Await.result(eventualResponse, reasonableWait)
  }

  def getImageLoaderLoadLink: String = {
    getImageLoaderEndpoints.links.find(_.rel == "load").get.href
  }

  def getPrepareUploadLink: String = {
    getImageLoaderEndpoints.links.find(_.rel == "prepare").get.href
  }

  def prepareUpload(mediaId: String, filename: String): Either[String, Map[String, String]] = {
    val prepareEndpoint = getPrepareUploadLink

    val mediaIdsToFilenamesMap = Map(
      mediaId -> filename
    )

    val eventualResponse = wsClient.url(prepareEndpoint).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(Json.toJson(mediaIdsToFilenamesMap))

    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      Right(Json.parse(response.body).as[Map[String, String]])
    } else {
      Left(response.body)
    }
  }

  def loadImage(image: Array[Byte]): Either[String, ImageUploadResponse] = {
    // TODO sync end point is not advertised?
    val prepareEndpoint = getImageLoaderLoadLink
    val meh = prepareEndpoint.split("\\{").head

    val eventualResponse = wsClient.url(meh).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(image)

    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 202) {
      Right(Json.parse(response.body).as[ImageUploadResponse])
    } else {
      Left(response.body)
    }
  }

  def putImage(uploadURL: String, mediaId: String, image: Array[Byte]) = {
    val eventualResponse = wsClient.url(uploadURL).
      withHttpHeaders("host" -> "eelpie-grid-ingest.s3.eu-west-1.amazonaws.com",
        "x-amz-meta-media-id" -> mediaId
      ).
      put(image)
    val response = Await.result(eventualResponse, reasonableWait)
    response
  }

  def getImage(imageId: String): Option[Image] = {
    val imageLink = getServiceEndpoints.links.find(_.rel == "image").get.href
    val url = insertIdInto(imageLink, imageId)
    val eventualResponse = authedGet(url)
    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      val data = Json.parse(response.body) \ "data"
      Some(data.as[Image])
    } else {
      None
    }
  }

  def getFileMetadata(image: Image): Option[FileMetadata] = {
    val eventualResponse = authedGet(image.fileMetadata.uri)
    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      val data = Json.parse(response.body) \ "data"
      Some(data.as[FileMetadata])
    } else {
      None
    }
  }

  def getUploadStatusFor(imageId: String): Option[UploadStatusResponse] = {
    val uploadStatusLink = getImageLoaderEndpoints.links.find(_.rel == "uploadStatus").map(_.href).get
    getUploadStatusByURI(insertIdInto(uploadStatusLink, imageId))
  }

  def getUploadStatusByURI(uri: String): Option[UploadStatusResponse] = {
    val eventualResponse = authedGet(uri)
    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      Some(Json.parse(response.body).as[UploadStatusResponse])
    } else {
      None
    }
  }

  private def loadServiceIndexPage(usagesBaseUrl: String): MediaApiResponse = {
    val eventualResponse = authedGet(usagesBaseUrl)
    val response = Await.result(eventualResponse, reasonableWait)
    response.body[JsValue].as[MediaApiResponse]
  }


  private def authedGet(uri: String): Future[StandaloneWSRequest#Self#Response] = {
    wsClient.url(uri).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      get()
  }

  private def insertIdInto(link: String, id: String): String = {
    link.replaceAll("""\{id}""", id)
  }

}
