import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}


class LeasesTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Leases API" should "allow a syndication leases to be added to an image" in {
    val imageUri = uploadImage("poppies.tif")
    val imageId = imageUri.split("/").last

    gridApi.addSyndicationLease(imageId)

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val result = gridApi.getLeases(imageId)
      result.isRight mustBe true
      val leases = result.right.get
      leases.nonEmpty mustBe true
      leases.head.mediaId mustBe imageId
      leases.head.access mustBe "allow-syndication"
    }
  }
}
