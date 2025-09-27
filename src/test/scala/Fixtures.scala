trait Fixtures {

  def gridApi: GridApi

  def uploadImage(filename: String): String = {
    val image = getClass.getResourceAsStream(filename).readAllBytes()
    val imageUploadResponse = gridApi.loadImage(image)
    val uploadStatusResponse = gridApi.getUploadStatusByURI(imageUploadResponse.right.get.uri)
    val imageUri = uploadStatusResponse.get.uri
    imageUri
  }

}
