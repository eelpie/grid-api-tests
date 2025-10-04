import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID

class SyndicationTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  private val testImagesSet = getFilesInFolder("syndication")

  private val grouped = testImagesSet.grouped(2).toSeq
  private val forReview = grouped.head
  private val forQueued = grouped(1)
  private val forNotOwned = grouped(2)
  private val forNotOwnedButLeased = grouped(3)

  "Syndication filter" should "include owned images in review view" in {
    val imageIds = forReview.map { image =>
      val imageUri = uploadImage(image)
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
      val images = gridApi.getImages(q = Some("+syndicationStatus:review"))
      val imageIdsInSearchResponse = images.map(_.id)
      imageIds.forall(imageIdsInSearchResponse.contains) mustBe true
    }
  }

  it should "include owned images with a syndication lease in queued" in {
    val imageIds = forQueued.map { image =>
      val imageUri = uploadImage(image)
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
    imageIds.foreach { imageId =>
      gridApi.addSyndicationLease(imageId)
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getImages(q = Some("+syndicationStatus:queued"))
      val imageIdsInSearchResponse = images.map(_.id)
      imageIds.forall(imageIdsInSearchResponse.contains) mustBe true
    }
  }

  it should "show non owned images as unsuitable" in {
    val imageIds = forNotOwned.map { image =>
      val imageUri = uploadImage(image)
      val imageId = imageUri.split("/").last
      imageId
    }

    imageIds.foreach { imageId =>
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "handout", // TODO source from API
      )
      gridApi.setUsageRights(imageId, newUsagesRights).isRight mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getImages(q = Some("+syndicationStatus:unsuitable"))
      val imageIdsInSearchResponse = images.map(_.id)
      imageIds.forall(imageIdsInSearchResponse.contains) mustBe true
    }
  }

  it should "show non owned images with a syndication lease as unsuitable and not queued" in {
    val imageIds = forNotOwnedButLeased.map { image =>
      val imageUri = uploadImage(image)
      val imageId = imageUri.split("/").last
      imageId
    }

    imageIds.foreach { imageId =>
      val newPhotographer = UUID.randomUUID().toString
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "handout", // TODO source from API
        "photographer" -> newPhotographer
      )
      gridApi.setUsageRights(imageId, newUsagesRights).isRight mustBe true
    }
    imageIds.foreach { imageId =>
      gridApi.addSyndicationLease(imageId)
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getImages(q = Some("+syndicationStatus:unsuitable"))
      val imageIdsInSearchResponse = images.map(_.id)
      imageIds.forall(imageIdsInSearchResponse.contains) mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getImages(q = Some("+syndicationStatus:queued"))
      val imageIdsInSearchResponse = images.map(_.id)
      imageIds.forall(imageIdsInSearchResponse.contains) mustBe false
    }
  }
}