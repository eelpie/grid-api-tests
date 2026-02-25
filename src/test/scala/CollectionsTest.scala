import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class CollectionsTest extends AnyFlatSpec with GridUnderTest with Fixtures {

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

}

