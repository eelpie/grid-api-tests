import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.{ExifDirectoryBase, ExifIFD0Directory}
import org.apache.pekko.util.ByteString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, not}

import java.io.ByteArrayInputStream
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}


class CropsTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Crops API" should "crop an image and return crop details" in {
    val image = uploadImage("crops/IMG_0106.HEIC")
    // TODO set credit and description
    val cropRequest = CropRequest(
      source = gridApi.uriFor(image),
      x = 10,
      y = 10,
      width = 2000,
      height = 3000,
      // TODO aspect ratio does what?
    )

    val result = gridApi.createCrop(cropRequest)

    result.isRight mustBe true
    val crop = result.right.get
    crop.id.nonEmpty mustBe true
    crop.specification.bounds.x mustBe 10
    crop.specification.bounds.y mustBe 10
    crop.specification.bounds.width mustBe 2000
    crop.specification.bounds.height mustBe 3000

    crop.assets.head.mimeType mustBe "image/jpeg"
  }

  it should "strip used exif orientation from crop assets which have already been correctly oriented" in {
    val exifOrientedImage = uploadImage("crops/IMG_5380.JPG")
    // TODO set credit and description

    val cropRequest = CropRequest(
      source = gridApi.uriFor(exifOrientedImage),
      x = 100,
      y = 1200,
      width = 3000,
      height = 3400,
    )

    val result = gridApi.createCrop(cropRequest)
    result.isRight mustBe true
    val crop = result.right.get

    // Confirm that the expected crop asset is visible with the expected vertical dimensions
    val maybeAsset = crop.assets.find(_.dimensions.height == 1000)

    val assertUrl = maybeAsset.get.file

    // Download this file for inspection
    val maybeAssetBytes: Option[ByteString] = Await.result(gridApi.get(assertUrl), Duration(10, SECONDS))

    val assetBytes = maybeAssetBytes.get
    val inputStream = new ByteArrayInputStream(assetBytes.toArray)

    val metadata = ImageMetadataReader.readMetadata(inputStream)
    val directory = metadata.getFirstDirectoryOfType(classOf[ExifIFD0Directory])
    val maybeOrientation = if (directory != null && directory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
      Some(directory.getInteger(ExifDirectoryBase.TAG_ORIENTATION))
    } else {
      None
    }
    maybeOrientation must not be Some(6)
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

  it should "allow download of crop assets via the media API download crop end point" in {
    // If canDownloadCrop config is set
    val imageToCrop = uploadImage("crops/IMG_5380.JPG")
    // TODO set credit and description

    val cropRequest = CropRequest(
      source = gridApi.uriFor(imageToCrop),
      x = 100,
      y = 1200,
      width = 3000,
      height = 3400,
    )

    val result = gridApi.createCrop(cropRequest)
    result.isRight mustBe true
    val crop = result.right.get
    val cropId = crop.id

    // Discover the download via media api
    val imageCropLinks = gridApi.getImageCropEndpoints(imageToCrop.id).links
    val cropLinkToDownload = imageCropLinks.find(_.rel == "crop-download-" + cropId + "-441")

    // Download this file for inspection
    val download = gridApi.authedGet(cropLinkToDownload.get.href) // TODO push back to private to GridAPI
    val downloadResult = Await.result(download, Duration(10, SECONDS))
    downloadResult.status mustBe 200
    val assetBytes = downloadResult.bodyAsBytes
    assetBytes.size mustBe 52299 // TODO cross check with api response
  }
}