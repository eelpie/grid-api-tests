import org.joda.time.DateTime
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID


class UsageTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  private val testImagesSet = getFilesInFolder("usages")

  private val grouped = testImagesSet.grouped(1).toSeq
  private val forPrint = grouped.head
  private val forSyndication = grouped(1)
  private val forDelete = grouped(2)
  private val forDigital = grouped(3)

  "Usages API" should "allow print usages to be added to an image" in {
    val image = uploadImage(forPrint.head)
    purgeUsages(image)

    val usageId = UUID.randomUUID().toString
    val dateAdded = DateTime.now
    val printUsageSubmission = examplePrintUsage(image, usageId, dateAdded)

    gridApi.addPrintUsage(printUsageSubmission)

    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getImage(image.id).get.usages
      usages.data.nonEmpty mustBe true
    }
    val usages = gridApi.getUsages(image.id).get.data.map(_.data)
    usages.exists(usage => usage.platform == "print" && usage.dateAdded == dateAdded && usage.status == "published") mustBe true
  }

  it should "allow digital media usages to be added to an image" in {
    val image = uploadImage(forDigital.head)
    purgeUsages(image)

    val usageId = UUID.randomUUID().toString
    val dateAdded = DateTime.now
    val webUrl = "http://localhost/" + UUID.randomUUID().toString
    val title = None
    val sectionId = None
    val digitalMediaUsageSubmission = exampleDigitalMediaUsage(image, usageId, dateAdded, webUrl, title, sectionId)

    val result = gridApi.addDigitalMediaUsage(digitalMediaUsageSubmission)

    result.isRight mustBe true
    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getImage(image.id).get.usages
      usages.data.nonEmpty mustBe true
    }
    val usages = gridApi.getUsages(image.id).get.data.map(_.data)
    usages.exists(usage => usage.platform == "digital" && usage.dateAdded == dateAdded && usage.status == "published") mustBe true
    usages.head.digitalUsageMetadata.map(_.webUrl) mustBe Some(webUrl)
  }

  it should "allow syndication usages to be added to an image" in {
    val image = uploadImage(forSyndication.head)
    purgeUsages(image)

    val dateAdded = DateTime.now

    val usagesSubmission = SyndicationUsageSubmission(
      mediaId = image.id,
      dateAdded = dateAdded,
      partnerName = "Our syndication partner"
    )

    gridApi.addSyndicationUsage(usagesSubmission)

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getUsages(image.id).get.data.map(_.data)
      val addedUsage = usages.find(usage => usage.platform == "syndication" && usage.dateAdded == dateAdded)
      addedUsage.nonEmpty mustBe true
    }
  }

  it should "allow all usage to be deleted from an image" in {
    val image = uploadImage(forDelete.head)

    val usageId = UUID.randomUUID().toString
    val dateAdded = DateTime.now
    val printUsageSubmission = examplePrintUsage(image, usageId, dateAdded)
    gridApi.addPrintUsage(printUsageSubmission)
    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getImage(image.id).get.usages
      usages.data.nonEmpty mustBe true
    }

    gridApi.deleteUsages(image.id)

    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getImage(image.id).get.usages
      usages.data.isEmpty mustBe true
    }
  }

  private def examplePrintUsage(image: Image, usageId: String, dateAdded: DateTime) = {
    PrintUsageSubmission(
      printUsageRecords = Seq(
        PrintUsage(
          mediaId = image.id,
          dateAdded = dateAdded,
          printUsageMetadata = PrintUsageMetadata(
            issueDate = "2025-11-02",
            sectionCode = "TST",
            sectionName = "Test section",
            storyName = "Test story",
            pageNumber = 7,
            publicationCode = "TSTPUB",
            publicationName = "Test publication"
          ),
          containerId = "container123",
          usageId = usageId,
          usageStatus = "published",
        )
      )
    )
  }

  private def exampleDigitalMediaUsage(image: Image, usageId: String, dateAdded: DateTime, webUrl: String, title: Option[String], sectionId: Option[String]) = {
    DigitalMediaUsageSubmission(
      digitalMediaUsageRecords = Seq(
        DigitalMediaUsage(
          mediaId = image.id,
          dateAdded = dateAdded,
          usageId = usageId,
          metadata = DigitalUsageMetadata(
            webUrl = webUrl,
            webTitle = title,
            sectionId = sectionId
          )
        )
      ))
  }

  private def purgeUsages(image: Image) = {
    gridApi.deleteUsages(image.id)
    eventually(timeout(Span(10, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getImage(image.id).get.usages
      usages.data.isEmpty mustBe true
    }
  }

}
