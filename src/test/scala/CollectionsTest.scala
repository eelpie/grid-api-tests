import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

import scala.concurrent.ExecutionContext.Implicits.global

class CollectionsTest extends AnyFlatSpec with GridUnderTest with Fixtures {

  "Collections API" should "load the root node" in {

    gridApi.getCollections()



    /*
    https://apitest2.griddev.eelpieconsulting.co.uk/collections/collections/Home

    {data: "test1"}
    data
      :
      "test1"

    */  1 mustBe 2


  }

}

