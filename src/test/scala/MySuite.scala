class MySuite extends munit.FunSuite {
  val gridApi = new GridApi(???, ???)
  test("can load image") {
    val image = getClass.getResourceAsStream("IMG_4525.JPG").readAllBytes()

    val uploadStatus = gridApi.loadImage(image)
    println(uploadStatus)
    assertEquals(uploadStatus.nonEmpty, true)
  }

}
