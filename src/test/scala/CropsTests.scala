import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper


class CropsTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Crops API" should "crop an image and return crop details" in {
    val image = uploadImage("IMG_3939.JPG")
    // TODO credit and description
    val cropRequest = CropRequest(
      source = gridApi.uriFor(image),
      x = 800,
      y = 810,
      width = 3000,
      height = 2000,
      // TODO aspect ratio does what?
    )

    val result = gridApi.createCrop(cropRequest)

    result.isRight mustBe true
    val crop = result.right.get
    crop.id.nonEmpty mustBe true
    crop.specification.bounds.x mustBe 800
    crop.specification.bounds.y mustBe 810
    crop.specification.bounds.width mustBe 3000
    crop.specification.bounds.height mustBe 2000

    crop.assets.head.mimeType mustBe "image/jpeg"
  }

  it should "crop graphics to PNG format" in {
    val image = uploadImage("basn3p08.png")
    val cropRequest = CropRequest(
      source = gridApi.uriFor(image),
      x = 1,
      y = 1,
      width = 30,
      height = 30
    )
    val updatedMetadata = Map(
      "credit" -> "schaik.com pngsuite",
      "description" -> "Indexed PNG which should be identified as a graphic"
    )
    gridApi.setMetadata(image.id, updatedMetadata)

    val result = gridApi.createCrop(cropRequest)

    result.isRight mustBe true
    val crop = result.right.get
    crop.assets.head.mimeType mustBe "image/png"
  }

}