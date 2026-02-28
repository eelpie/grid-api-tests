import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID

class UsageRightsTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  private val testImagesSet = getFilesInFolder("usage-rights")
  private val toSet = testImagesSet.head
  private val unset = testImagesSet(1)

  "Usage rights" should "allow usage rights for an image to be set" in {
    val image = uploadImage(toSet)

    val newPhotographer = UUID.randomUUID().toString
    val newUsagesRights = Map(
      "publication" -> "Test", // TODO how important is in that this matches config?
      "category" -> "staff-photographer", // TODO source from API
      "photographer" -> newPhotographer
    )

    gridApi.setUsageRights(image.id, newUsagesRights).isRight mustBe true

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val reloaded = gridApi.getImage(image.id).get
      reloaded.usageRights.publication mustBe Some("Test")
      reloaded.usageRights.category mustBe Some("staff-photographer")
      reloaded.usageRights.photographer mustBe Some(newPhotographer)
    }
  }

}

