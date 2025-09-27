import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID


class MetadataTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Metadata API" should "allow image metadata to be set" in {
    val imageUri = uploadImage("poppies.tif")
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

}
