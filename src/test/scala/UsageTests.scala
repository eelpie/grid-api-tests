import org.joda.time.DateTime
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID


class UsageTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Usages API" should "allow print usages to be added to an image" in {
    val image = uploadImage("poppies.tif")

    val usageId = UUID.randomUUID().toString
    val dateAdded = DateTime.now

    val printUsageSubmission = PrintUsageSubmission(
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

    gridApi.addPrintUsage(printUsageSubmission)

    val usages = gridApi.getUsages(image.id).data.map(_.data)
    usages.find(usage => usage.platform == "print" && usage.dateAdded == dateAdded && usage.status == "published")
  }

  it should "allow syndication usages to be added to an image" in {
    val image = uploadImage("poppies.tif")

    val dateAdded = DateTime.now

    val usagesSubmission = SyndicationUsageSubmission(
      mediaId = image.id,
      dateAdded = dateAdded,
      partnerName = "Our syndication partner"
    )

    gridApi.addSyndicationUsage(usagesSubmission)

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val usages = gridApi.getUsages(image.id).data.map(_.data)
      val addedUsage = usages.find(usage => usage.platform == "syndication" && usage.dateAdded == dateAdded)
      addedUsage.nonEmpty mustBe true
    }
  }

}
