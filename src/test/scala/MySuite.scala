class MySuite extends munit.FunSuite {
  val gridApi = new GridApi(???, ???)
  test("can discover image loader urls") {
    val prepareEndpoint = gridApi.getImageLoaderPrepareEndpoint
    println(prepareEndpoint)
    assertEquals(prepareEndpoint.nonEmpty, true)
  }
}
