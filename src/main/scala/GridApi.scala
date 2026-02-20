import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.apache.pekko.util.ByteString
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyReadables.readableAsJson
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.ws.{DefaultBodyWritables, EmptyBody, StandaloneWSRequest}

import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

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

  def get(url: String)(implicit ec: ExecutionContext): Future[Option[ByteString]] = {
    wsClient.url(url).get.map { r =>
      if (r.status == 200) {
        Some(r.bodyAsBytes)
      } else {
        None
      }
    }
  }

  def getServiceEndpoints: MediaApiResponse = {
    loadServiceIndexPage(mediaApiUrl)
  }

  def uriFor(image: Image): String = {
    val links: Seq[Link] = getServiceEndpoints.links
    val imageLink = links.find(_.rel == "image").get
    insertIdInto(imageLink.href, image.id)
  }

  def getImageLoaderEndpoints: MediaApiResponse = {
    val loaderLink = getServiceEndpoints.links.find(_.rel == "loader").get
    loadServiceIndexPage(loaderLink.href)
  }

  def getLeaseEndpoints: MediaApiResponse = {
    val leaseLink = getServiceEndpoints.links.find(_.rel == "leases").get
    loadServiceIndexPage(leaseLink.href)
  }

  def getCollectionsEndpoints: MediaApiResponse = {
    val collectionsLink = getServiceEndpoints.links.find(_.rel == "collections").get
    loadServiceIndexPage(collectionsLink.href)
  }

  def getUsageEndpoints: MediaApiResponse = {
    val usageLink = getServiceEndpoints.links.find(_.rel == "usage").get
    loadServiceIndexPage(usageLink.href)
  }

  def getCropperEndpoints: MediaApiResponse = {
    val cropperLink = getServiceEndpoints.links.find(_.rel == "cropper").get
    loadServiceIndexPage(cropperLink.href)
  }

  def getMetadataEndpoints: MediaApiResponse = {
    val editsLinks = getServiceEndpoints.links.find(_.rel == "edits").get
    loadServiceIndexPage(editsLinks.href)
  }

  def getLeases(imageId: String): Either[Unit, Seq[Lease]] = {
    val links: MediaApiResponse = getLeaseEndpoints
    val byMediaIdLink = links.links.find(_.rel == "by-media-id").get
    val url = insertIdInto(byMediaIdLink.href, imageId)

    val eventualResponse = authedGet(url)
    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      Right((Json.parse(response.body) \ "data" \ "leases").as[Seq[Lease]])
    } else {
      Left()
    }
  }

  def getCollections()(implicit ec: ExecutionContext): Option[CollectionsResponse] = {
    val links = getCollectionsEndpoints
    val collectionsLink = links.links.find(_.rel == "collections").get

    val response = Await.result(authedGet(collectionsLink.href), reasonableWait)
    if (response.status == 200) {
      Some(Json.parse(response.body).as[CollectionsResponse])
    } else {
      None
    }
  }

  def getUsages(imageId: String): Option[UsagesResponse] = {
    val url = getUsagesByMediaLink.replaceAll("\\{id}", imageId)
    val eventualResponse = authedGet(url)
    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      Some(Json.parse(response.body).as[UsagesResponse])
    } else {
      // End point returns 404 rather than empty list when no usages!
      None
    }
  }

  def deleteUsages(id: String): Unit = {
    val uri = insertIdInto(getUsagesByIdLink, id)
    val eventualResponse = wsClient.url(uri).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      delete()
    Await.result(eventualResponse, reasonableWait)
  }

  private def getUsagesByIdLink: String = {
    getUsageEndpoints.links.find(_.rel == "usages-by-media").map(_.href).get
  }

  private def getUsagesByMediaLink: String = {
    getUsageEndpoints.links.find(_.rel == "usages-by-media").map(_.href).get
  }

  private def getUsagePrintUsageAction: String = {
    getUsageEndpoints.actions.flatMap(_.find(_.name == "print-usage").map(_.href)).get
  }

  private def getUsageSyndicationUsageAction: String = {
    getUsageEndpoints.actions.flatMap(_.find(_.name == "syndication-usage").map(_.href)).get
  }

  private def getMetadataLink: String = {
    getMetadataEndpoints.links.find(_.rel == "metadata").map(_.href).get
  }

  private def getUsageRightsLink: String = {
    getMetadataEndpoints.links.find(_.rel == "usageRights").map(_.href).get
  }

  private def getUsageRightsCategoriesLink: String = {
    getMetadataEndpoints.links.find(_.rel == "usage-rights-list").map(_.href).get
  }

  def setMetadata(imageId: String, updatedMetadata: Map[String, String]): Unit = {
    val url = insertIdInto(getMetadataLink, imageId)

    val data = Map(
      "data" -> updatedMetadata
    )

    val eventualResponse = wsClient.url(url).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      put(Json.toJson(data))

    Await.result(eventualResponse, reasonableWait)
  }

  def getUsageRightsCategories(): Either[String, Seq[UsageRightsCategory]] = {
    val url = getUsageRightsCategoriesLink

    val eventualResponse = wsClient.url(url).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      get()

    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      Right(Json.parse(response.body).as[UsageRightsCategoriesResponse].data)
    } else {
      Left(response.body)
    }
  }

  def setUsageRights(imageId: String, newUsagesRights: Map[String, String]): Either[String, Unit] = {
    val url = insertIdInto(getUsageRightsLink, imageId)

    val data = Map(
      "data" -> newUsagesRights
    )

    val eventualResponse = wsClient.url(url).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      put(Json.toJson(data))

    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      Right()
    } else {
      Left(response.body)
    }
  }

  def createCrop(cropRequest: CropRequest): Either[String, Crop] = {
    val cropLink = getCropperEndpoints.links.find(_.rel == "crop").get

    val eventualResponse = wsClient.url(cropLink.href).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(Json.toJson(cropRequest))

    val response = Await.result(eventualResponse, reasonableWait)
    if (response.status == 200) {
      Right(Json.parse(response.body).as[Crop])
    } else {
      Left(response.body)
    }
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

  def syndicate(id: String, partnerName: String, startPending: Boolean): Unit = {
    val syndicateImageLink = getServiceEndpoints.links.find(_.rel == "syndicate-image").get
    val withImageId = insertIdInto(syndicateImageLink.href, id)
    val withPartnerName = withImageId.replaceAll("""\{partnerName}""", partnerName)
    val withStartPending = withPartnerName.replaceAll("""\{startPending}""", startPending.toString)

    val eventualResponse = wsClient.url(withStartPending).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(EmptyBody)

    Await.result(eventualResponse, reasonableWait)
  }

  def addSyndicationLease(imageId: String): Unit = {
    val leaseEndpoints: MediaApiResponse = getLeaseEndpoints
    val leasesLink = leaseEndpoints.links.find(_.rel == "leases").get
    val url = leasesLink.href.split("/\\{").head

    val leaseSubmission = LeaseSubmission(
      mediaId = imageId,
      createdAt = DateTime.now,
      access = "allow-syndication"
    )

    val eventualResponse = wsClient.url(url).
      withHttpHeaders("X-Gu-Media-Key" -> apiKey).
      post(Json.toJson(leaseSubmission))

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
    val endpoint = prepareEndpoint.split("\\{").head

    val eventualResponse = wsClient.url(endpoint).
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
      withHttpHeaders(
        "x-amz-meta-media-id" -> mediaId,
      ).put(image)
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

  def getImages(q: Option[String]): Seq[Image] = {
    val searchLink = getServiceEndpoints.links.find(_.rel == "search").get.href
    val url = searchLink.replaceAll("""\{.*?}""", "") + "?length=50" + q.map(q => "&q=" + q).getOrElse("") // TODO proper parameter encoding

    val eventualResponse = authedGet(url)
    val response = Await.result(eventualResponse, reasonableWait)

    val imageSearchResponse = Json.parse(response.body).as[ImageSearchResponse]
    imageSearchResponse.data.map(_.data)
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
