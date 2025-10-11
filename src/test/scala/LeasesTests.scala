import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}


class LeasesTests extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Leases API" should "allow a syndication lease to be added to an image" in {
    val image = uploadImage("poppies.tif")

    gridApi.addSyndicationLease(image.id)

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val result = gridApi.getLeases(image.id)
      result.isRight mustBe true
      val leases = result.right.get
      leases.nonEmpty mustBe true
      leases.head.mediaId mustBe image.id
      leases.head.access mustBe "allow-syndication"
    }
  }
}
