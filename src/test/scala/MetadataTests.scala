import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID


class MetadataTests extends AnyFlatSpec with GridUnderTest {

  "Metadata API" should "allow image metadata to be set" in {
    val imageUri = uploadImage
    val imageId = imageUri.split("/").last

    val newTitle = UUID.randomUUID().toString
    val newDescription = UUID.randomUUID().toString
    val updatedMetadata = Map(
      "title" -> newTitle,
      "description" -> newDescription,
    )

    gridApi.setMetadata(imageId, updatedMetadata)

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val image = gridApi.getImage(imageId).get
      image.metadata.title mustBe Some(newTitle)
      image.metadata.description mustBe Some(newDescription)
    }
  }

  private def uploadImage: String = {
    val image = getClass.getResourceAsStream("poppies.tif").readAllBytes()
    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isRight mustBe true
    val uploadStatusResponse = gridApi.getUploadStatusByURI(imageUploadResponse.right.get.uri)
    val imageUri = uploadStatusResponse.get.uri
    imageUri
  }

}
