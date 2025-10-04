import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID

class OwnedImagesTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  val testImagesSet = Set("poppies.tif")

  "Owned images" should "include images with owned usage rights" in {
    val imageIds = testImagesSet.map { image =>
      val imageUri = uploadImage("poppies.tif")
      val imageId = imageUri.split("/").last
      imageId
    }

    imageIds.foreach { imageId =>
      val newPhotographer = UUID.randomUUID().toString
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "staff-photographer", // TODO source from API
        "photographer" -> newPhotographer
      )
      gridApi.setUsageRights(imageId, newUsagesRights).isRight mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getOwnedImages()
      val imageIdsInSearchResponse = images.map(_.id)
      imageIds.forall(imageIdsInSearchResponse.contains) mustBe true
    }
  }

}
