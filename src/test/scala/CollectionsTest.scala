import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class CollectionsTest extends AnyFlatSpec with GridUnderTest with Fixtures {

  private val testImagesSet = getFilesInFolder("collections")

  "Collections API" should "load the root node" in {
    val collections = gridApi.getCollections()

    val rootNode = collections.get.data
    rootNode.basename mustBe "root"
    rootNode.fullPath mustBe Seq.empty
  }

  it should "create a new collection under existing node" in {
    val newCollectionName = UUID.randomUUID().toString
    val rootNode = gridApi.getCollections().get.data

    val result = gridApi.addCollection(newCollectionName, rootNode.fullPath)
    result.data.basename mustBe newCollectionName
    result.data.fullPath mustBe Seq(newCollectionName)

    val newChildName = UUID.randomUUID().toString
    val child = gridApi.addCollection(newChildName, result.data.fullPath)
    child.data.basename mustBe newChildName
    child.data.fullPath mustBe Seq(newCollectionName, newChildName)
  }

  it should "persist image collection when an image is added to a collection" in {
    val image = uploadImage(testImagesSet.head)

    val newCollectionName = UUID.randomUUID().toString
    val rootNode = gridApi.getCollections().get.data
    val collectionsResponse = gridApi.addCollection(newCollectionName, rootNode.fullPath)

    gridApi.addImageCollection(image.id, collectionsResponse.data.fullPath)

    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis))) {
      val readback = gridApi.getImage(image.id)
      readback.get.collections.exists(c => c.data.path == collectionsResponse.data.fullPath) mustBe (true)
    }
  }

}

