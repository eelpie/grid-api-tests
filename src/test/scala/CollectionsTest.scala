import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

import scala.concurrent.ExecutionContext.Implicits.global

class CollectionsTest extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Collections API" should "load the root node" in {
    val collections = gridApi.getCollections()

    val rootNode = collections.get.data
    rootNode.basename mustBe "root"
  }

}

