import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID

class MetadataTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Metadata extraction" should
    "record file metadata" in {
    val image = uploadImage("IPTC-GoogleImgSrcPmd_testimg01.jpg")

    val fileMetadata = gridApi.getFileMetadata(image).get

    fileMetadata.colourModel mustBe Some("RGB")
    fileMetadata.iptc.get("Credit") mustBe "IPTC/Jane Photosty"
    fileMetadata.iptc.get("By-line") mustBe "Jane Photosty"
    fileMetadata.iptc.get("Headline") mustBe "The railway and the cars"
    fileMetadata.iptc.get("Special Instructions") mustBe "This photo is for metadata testing purposes only"
  }

  it should "set initial image metadata from file metadata" in {
    val image = uploadImage("IPTC-GoogleImgSrcPmd_testimg01.jpg")

    val maybeImage = gridApi.getImage(image.id)
    maybeImage.flatMap(_.metadata.title) mustBe Some("The railway and the cars")
    maybeImage.flatMap(_.metadata.description) mustBe Some("The railways of the S45 line are running very close to a small street with parking cars")
    maybeImage.flatMap(_.metadata.byline) mustBe Some("Jane Photosty")
    maybeImage.flatMap(_.metadata.credit) mustBe Some("IPTC")
  }

  "Metadata editing" should "allow image metadata to be set" in {
    val image = uploadImage("poppies.tif")

    val newTitle = UUID.randomUUID().toString
    val newDescription = UUID.randomUUID().toString
    val updatedMetadata = Map(
      "title" -> newTitle,
      "description" -> newDescription,
    )

    gridApi.setMetadata(image.id, updatedMetadata)

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val updatedImage = gridApi.getImage(image.id).get
      updatedImage.metadata.title mustBe Some(newTitle)
      updatedImage.metadata.description mustBe Some(newDescription)
    }
  }

  it should "allow rights and restrictions to be set for an image" in {
    val image = uploadImage("poppies.tif")

    val newPhotographer = UUID.randomUUID().toString

    val newUsagesRights = Map(
      "publication" -> "Test",  // TODO how important is in that this matches config?
      "category" -> "staff-photographer", // TODO source from API
      "photographer" -> newPhotographer
    )

    val result = gridApi.setUsageRights(image.id, newUsagesRights)

    result.isRight mustBe true
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val updatedImage = gridApi.getImage(image.id).get
      updatedImage.usageRights.category mustBe Some("staff-photographer")
      updatedImage.usageRights.photographer mustBe Some(newPhotographer)
    }
  }

}
