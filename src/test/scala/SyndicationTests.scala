import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID

class SyndicationTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  private val testImagesSet = getFilesInFolder("syndication")

  private val grouped = testImagesSet.grouped(1).toSeq
  private val forReview = grouped.head
  private val forQueued = grouped(1)
  private val forNotOwned = grouped(2)
  private val forNotOwnedButLeased = grouped(3)
  private val forSent = grouped(4)

  "Syndication filter" should "include owned images in review view" in {
    val images = forReview.map { filename =>
      uploadImage(filename)
    }
    images.foreach { image =>
      setOwnedUsageRights(image)
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getImages(q = Some("+syndicationStatus:review"))
      val imageIdsInSearchResponse = images.map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe true
    }
  }

  it should "include owned images with a syndication lease in queued" in {
    val images = forQueued.map { filename =>
      uploadImage(filename)
    }
    images.foreach(setOwnedUsageRights)
    images.foreach { image =>
      gridApi.addSyndicationLease(image.id)
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getImages(q = Some("+syndicationStatus:queued"))
      val imageIdsInSearchResponse = images.map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe true
      images.forall(_.syndicationStatus == "queued") mustBe true
    }
  }

  it should "show non owned images as unsuitable" in {
    val images = forNotOwned.map { filename =>
      uploadImage(filename)
    }

    images.foreach { image =>
      val newUsagesRights = Map(
        "publication" -> "Test", // TODO how important is in that this matches config?
        "category" -> "handout", // TODO source from API
      )
      gridApi.setUsageRights(image.id, newUsagesRights).isRight mustBe true
    }

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val images = gridApi.getImages(q = Some("+syndicationStatus:unsuitable"))
      val imageIdsInSearchResponse = images.map(_.id)
      images.map(_.id).forall(imageIdsInSearchResponse.contains) mustBe true
    }
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imagesInSearchResponse = gridApi.getImages(q = Some("+syndicationStatus:unsuitable"))
      imagesInSearchResponse.forall(_.syndicationStatus == "unsuitable") mustBe true
    }
  }

  it should "show non owned images with a syndication lease as unsuitable and not queued" in {
    val images = forNotOwnedButLeased.map { filename =>
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
    images.foreach { image =>
      gridApi.addSyndicationLease(image.id)
    }

    val imageIdsUnderTest = images.map(_.id)
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("+syndicationStatus:unsuitable")).map(_.id)
      imageIdsUnderTest.forall(imageIdsInSearchResponse.contains) mustBe true
    }
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("+syndicationStatus:queued")).map(_.id)
      imageIdsUnderTest.forall(imageIdsInSearchResponse.contains) mustBe false
    }
  }

  it should "mark as sent via the media api syndicate image end point" in {
    val image = uploadImage(forSent.head)
    purgeUsages(image)
    setOwnedUsageRights(image)
    gridApi.addSyndicationLease(image.id)
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      gridApi.getImage(image.id).get.syndicationStatus mustBe "queued"
    }

    // TODO edge case of something which was sent even through it was unsuitable?

    gridApi.syndicate(image.id, "our-syndication-partner", startPending = false)

    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val imageIdsInSearchResponse = gridApi.getImages(q = Some("+syndicationStatus:sent")).map(_.id)
      imageIdsInSearchResponse.contains(image.id) mustBe true
    }

    def maybeResponse = gridApi.getUsages(image.id)
    def maybeFirstUsage = maybeResponse.map(_.data.head.data)
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      maybeFirstUsage.nonEmpty mustBe true
    }
    val firstUsage = maybeFirstUsage.get
    firstUsage.platform mustBe "syndication"
    firstUsage.status mustBe "syndicated"

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val sentImage = gridApi.getImage(image.id).get
      sentImage.syndicationStatus mustBe "sent"
    }

    // TODO what does no title found mean in the UI?
  }

  // TODO it should "remove images marked as blocked from the review list" in {
  //  fail()
  //}

  private def purgeUsages(image: Image) = {
    gridApi.deleteUsages(image.id)
    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getImage(image.id).get.usages
      usages.data.isEmpty mustBe true
    }
  }

  private def setOwnedUsageRights(image: Image) = {
    val newPhotographer = UUID.randomUUID().toString
    val newUsagesRights = Map(
      "publication" -> "Test", // TODO how important is in that this matches config?
      "category" -> "staff-photographer", // TODO source from API
      "photographer" -> newPhotographer
    )
    gridApi.setUsageRights(image.id, newUsagesRights).isRight mustBe true
  }

}