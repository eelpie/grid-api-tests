import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

import java.util.UUID


class UsageTests extends AnyFlatSpec with GridUnderTest {

  "Usages API" should "allow print usages to be added to an image" in {
    val imageUri = uploadImage
    val imageId = imageUri.split("/").last

    val usageId = UUID.randomUUID().toString
    val dataAdded = DateTime.now

    val printUsageSubmission = PrintUsageSubmission(
      printUsageRecords = Seq(
        PrintUsage(
          mediaId = imageId,
          dateAdded = dataAdded,
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

    val usages = gridApi.getUsages(imageId).data.map(_.data)

    // TODO assert actual usage was persisted
    usages.find(usage => usage.platform == "print" && usage.dateAdded == dataAdded && usage.status == "published")
  }

  private def uploadImage: String = {
    val image = getClass.getResourceAsStream("poppies.tif").readAllBytes()
    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isRight mustBe true
    val uploadStatusResponse = gridApi.getUploadStatus(imageUploadResponse.right.get.uri)
    val imageUri = uploadStatusResponse.uri
    imageUri
  }

}
