import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{DefaultBodyWritables, StandaloneWSRequest}
import play.api.libs.ws.JsonBodyReadables.readableAsJson
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, SECONDS}

class GridApi(mediaApiUrl: String, apiKey: String) extends DefaultBodyWritables {

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

  def getUsages(imageId: String): UsagesResponse = {
    val url = getUsagesLink.replaceAll("\\{id}", imageId)
    val eventualResponse = authedGet(url)
    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    Json.parse(response.body).as[UsagesResponse]
  }

  private def getUsagesLink: String = {
    getUsageEndpoints.links.find(_.rel == "usages-by-media").map(_.href).get
  }


  private def getUsagePrintUsageAction: String = {
    getUsageEndpoints.actions.flatMap(_.find(_.name == "print-usage").map(_.href)).get
  }

  def addPrintUsage(printUsageSubmission: PrintUsageSubmission): Unit = {
    val eventualResponse = wsClient.url(getUsagePrintUsageAction).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(Json.toJson(printUsageSubmission))

    Await.result(eventualResponse, Duration(10, SECONDS))
  }


  def getImageLoaderLoadLink: String = {
    getImageLoaderEndpoints.links.find(_.rel == "load").get.href
  }

  def loadImage(image: Array[Byte]): Either[String, ImageUploadResponse] = {
    // TODO sync end point is not advertised?
    val prepareEndpoint = getImageLoaderLoadLink
    val meh = prepareEndpoint.split("\\{").head

    val eventualResponse = wsClient.url(meh).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(image)

    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    if (response.status == 202) {
      Right(Json.parse(response.body).as[ImageUploadResponse])
    } else {
      Left(response.body)
    }
  }

  def getUploadStatus(uri: String): UploadStatusResponse = {
    val eventualResponse = authedGet(uri)
    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    Json.parse(response.body).as[UploadStatusResponse]
  }


  private def loadServiceIndexPage(usagesBaseUrl: String): MediaApiResponse = {
    val eventualResponse = authedGet(usagesBaseUrl)
    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    response.body[JsValue].as[MediaApiResponse]
  }


  private def authedGet(uri: String): Future[StandaloneWSRequest#Self#Response] = {
    wsClient.url(uri).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      get()
  }

}
