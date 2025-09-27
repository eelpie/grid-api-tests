import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.security.MessageDigest

class ImageLoaderTests extends AnyFlatSpec with GridUnderTest {

  "Image loader async end point" should
    "prepare pre signed upload URLs for media ids and set upload status to prepared" in {
    val mediaId = "123"

    val result = gridApi.prepareUpload(mediaId, "123.jpg")

    result.isRight mustBe true
    val maybePresignedURLForMediaId = result.right.get.get("123")
    maybePresignedURLForMediaId.nonEmpty mustBe true
    gridApi.getUploadStatusFor(mediaId).map(_.data.status) mustBe Some("PREPARED")
  }

  it should "ingest images PUT to pre signed upload URLs" in {
    val filename = "IMG_3938.JPG"
    val image = getClass.getResourceAsStream(filename).readAllBytes()
    val mediaId = digestFor(image)

    val either = gridApi.prepareUpload(mediaId, filename)
    val uploadURL = either.right.get(mediaId)

    val response = gridApi.putImage(uploadURL, mediaId, image)

    response.status mustBe 200
    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val maybeImage = gridApi.getImage(mediaId)
      maybeImage.nonEmpty mustBe true
      maybeImage.get.id mustBe mediaId
    }
  }

  it should "eventually report completed upload status" in {
    val filename = "IMG_3938.JPG"
    val image = getClass.getResourceAsStream(filename).readAllBytes()
    val mediaId = digestFor(image)

    val either = gridApi.prepareUpload(mediaId, filename)
    val uploadURL = either.right.get(mediaId)

    gridApi.putImage(uploadURL, mediaId, image)

    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val maybeUploadStatus = gridApi.getUploadStatusFor(mediaId)

      val maybeUploadStatusStatus = maybeUploadStatus.map(_.data.status)
      maybeUploadStatusStatus mustBe Some("COMPLETED")
    }
  }

  it should "reject unsupported file types and set upload status to failed" in {
    val filename = "test.txt"
    val image = getClass.getResourceAsStream(filename).readAllBytes()
    val mediaId = digestFor(image)

    val either = gridApi.prepareUpload(mediaId, filename)
    val uploadURL = either.right.get(mediaId)

    gridApi.putImage(uploadURL, mediaId, image)

    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val maybeUploadStatus = gridApi.getUploadStatusFor(mediaId)

      val maybeUploadStatusStatus = maybeUploadStatus.map(_.data.status)
      maybeUploadStatusStatus mustBe Some("FAILED")
      maybeUploadStatus.flatMap(_.data.errorMessage) mustBe Some("com.gu.mediaservice.model.UnsupportedMimeTypeException: null")
    }
  }

  "Image loader sync end point" should
    "accept uploaded JPEG images and return upload status URL" in {
    val image = getClass.getResourceAsStream("IMG_4525.JPG").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)

    imageUploadResponse.isRight mustBe true
    val uploadStatus = gridApi.getUploadStatusByURI(imageUploadResponse.right.get.uri).get
    uploadStatus.data.status mustBe "COMPLETED"
  }

  it should "accept uploaded TIFF images" in {
    val image = getClass.getResourceAsStream("poppies.tif").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isRight mustBe true

    // TODO image/tif is not included in supported mime-types list. Need to understand why this is been accepted and gif is not
    val uploadStatus = gridApi.getUploadStatusByURI(imageUploadResponse.right.get.uri).get
    uploadStatus.data.status mustBe "COMPLETED"
  }

  it should "reject unsupported image types" in {
    val image = getClass.getResourceAsStream("Sunflower_as_gif_websafe.gif").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isLeft mustBe true
    imageUploadResponse.left.get mustBe "{\"errorKey\":\"unsupported-type\",\"errorMessage\":\"Unsupported mime-type: image/gif. Supported: image/jpeg, image/png\"}"
  }

  it should "reject non image files" in {
    val image = getClass.getResourceAsStream("test.txt").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isLeft mustBe true
    imageUploadResponse.left.get mustBe "{\"errorKey\":\"unsupported-type\",\"errorMessage\":\"Unsupported mime-type: unknown. Supported: image/jpeg, image/png\"}"
  }

  private def digestFor(image: Array[Byte]) = {
    val digest = MessageDigest.getInstance("SHA-1")
    digest.digest(image).map("%02x".format(_)).mkString
  }
}

