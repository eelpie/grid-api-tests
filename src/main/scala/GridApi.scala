import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables.readableAsJson
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class GridApi(mediaApiUrl: String, apiKey: String) {
  private val wsClient = {
    implicit val system: ActorSystem = ActorSystem()
    system.registerOnTermination {
      System.exit(0)
    }
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    StandaloneAhcWSClient()
  }

  def getServiceEndpoints: MediaApiResponse = {
    val eventualResponse = wsClient.url(mediaApiUrl).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      get()
    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    implicit val lr: Reads[Link] = Json.reads[Link]
    implicit val mrr: Reads[MediaApiResponse] = Json.reads[MediaApiResponse]
    response.body[JsValue].as[MediaApiResponse]
  }

  def getImageLoadedEndpoints: MediaApiResponse = {
    val loaderLink = getServiceEndpoints.links.find(_.rel == "loader").get

    val eventualResponse = wsClient.url(loaderLink.href).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      get()
    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    implicit val lr: Reads[Link] = Json.reads[Link]
    implicit val mrr: Reads[MediaApiResponse] = Json.reads[MediaApiResponse]
    response.body[JsValue].as[MediaApiResponse]
  }

  def getImageLoaderPrepareEndpoint: String = {
    getImageLoadedEndpoints.links.find(_.rel == "load").get.href
  }

  def loadImage(image: Array[Byte]): ImageUploadResponse = {
    val prepareEndpoint = getImageLoaderPrepareEndpoint
    val meh = prepareEndpoint.split("\\{").head

    val eventualResponse = wsClient.url(meh).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(image)

    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    Json.parse(response.body).as[ImageUploadResponse]
  }

  def getUploadStatus(uri: String): UploadStatus = {
    val eventualResponse = wsClient.url(uri).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      get()

    val response = Await.result(eventualResponse, Duration(10, SECONDS))
    Json.parse(response.body).as[UploadStatusResponse].data
  }
}


case class Link(rel: String, href: String)

case class MediaApiResponse(links: Seq[Link])

case class ImageLoadResponse(uri: String)