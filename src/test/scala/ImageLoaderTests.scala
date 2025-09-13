import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

class ImageLoaderTests extends AnyFlatSpec with GridUnderTest {

  "Image loader sync end point" should
    "accept uploaded JPEG images and return upload status" in {
    val image = getClass.getResourceAsStream("IMG_4525.JPG").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isRight mustBe true

    val uploadStatus = gridApi.getUploadStatus(imageUploadResponse.right.get.uri).data
    uploadStatus.status mustBe "COMPLETED"
  }

  it should "accept uploaded TIFF images and return upload status" in {
    val image = getClass.getResourceAsStream("poppies.tif").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isRight mustBe true

    // TODO image/tif is not included in supported mime-types list. Need to understand why this is been accepted and gif is not
    val uploadStatus = gridApi.getUploadStatus(imageUploadResponse.right.get.uri).data
    uploadStatus.status mustBe "COMPLETED"
  }

  it should "reject unsupported image types" in {
    val image = getClass.getResourceAsStream("Sunflower_as_gif_websafe.gif").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isLeft mustBe true
    imageUploadResponse.left.get mustBe "{\"errorKey\":\"unsupported-type\",\"errorMessage\":\"Unsupported mime-type: image/gif. Supported: image/jpeg, image/png\"}"
  }

  it should "reject non image files" in {
    val image = getClass.getResourceAsStream("test.txt").readAllBytes()

    val imageUploadResponse = gridApi.loadImage(image)
    imageUploadResponse.isLeft mustBe true
    imageUploadResponse.left.get mustBe "{\"errorKey\":\"unsupported-type\",\"errorMessage\":\"Unsupported mime-type: unknown. Supported: image/jpeg, image/png\"}"
  }
}
