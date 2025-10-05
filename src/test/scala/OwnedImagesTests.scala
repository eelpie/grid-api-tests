import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID

class OwnedImagesTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  val testImagesSet = getFilesInFolder("owned")
  private val grouped = testImagesSet.grouped(1).toSeq
  private val forStaffPhotographer = grouped.head
  private val forContractPhotographer = grouped(1)
  private val forCommissionedPhotographer = grouped(2)
  private val forNonOwned = grouped(3)

  "Owned images" should "include images with staff photographer rights" in {
    val images = forStaffPhotographer.map { filename =>
      uploadImage(filename)
    }

    images.foreach { image =>
      val newPhotographer = UUID.randomUUID().toString
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "staff-photographer", // TODO source from API
        "photographer" -> newPhotographer
      )
      gridApi.setUsageRights(image.id, newUsagesRights).isRight mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("is:owned")).map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe true
    }
  }

  it should "include images with commissioned photographer rights" in {
    val images = forCommissionedPhotographer.map { filename =>
      uploadImage(filename)
    }

    images.foreach { image =>
      val newPhotographer = UUID.randomUUID().toString
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "commissioned-photographer", // TODO source from API
        "photographer" -> newPhotographer
      )
      gridApi.setUsageRights(image.id, newUsagesRights).isRight mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("is:owned")).map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe true
    }

  }

  it should "include images with contract photographer rights" in {
    val images = forContractPhotographer.map { filename =>
      uploadImage(filename)
    }

    images.foreach { image =>
      val newPhotographer = UUID.randomUUID().toString
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "contract-photographer", // TODO source from API
        "photographer" -> newPhotographer
      )
      gridApi.setUsageRights(image.id, newUsagesRights).isRight mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("is:owned")).map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe true
    }
  }

  it should "not include images non owned usages rights" in {
    val images = forNonOwned.map { filename =>
      uploadImage(filename)
    }

    images.foreach { image =>
      val newPhotographer = UUID.randomUUID().toString
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "handout", // TODO source from API
        "photographer" -> newPhotographer
      )
      gridApi.setUsageRights(image.id, newUsagesRights).isRight mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("-is:owned")).map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("is:owned")).map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe false
    }
  }

}
