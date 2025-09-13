import org.scalatest._
import flatspec._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

class ImageLoaderTests extends AnyFlatSpec with GridUnderTest {

  "Image loader sync end point" should "accept uploaded images and return upload status" in {
    val image = getClass.getResourceAsStream("IMG_4525.JPG").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isRight mustBe true

    val uploadStatus = gridApi.getUploadStatus(imageUploadResponse.right.get.uri)
    uploadStatus.status mustBe "COMPLETED"
  }

  "Image loader sync end point" should "reject non image files" in {
    val image = getClass.getResourceAsStream("test.txt").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isLeft mustBe true
    imageUploadResponse.left.get mustBe "{\"errorKey\":\"unsupported-type\",\"errorMessage\":\"Unsupported mime-type: unknown. Supported: image/jpeg, image/png\"}"
  }
}
