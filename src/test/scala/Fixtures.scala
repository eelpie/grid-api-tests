import java.nio.file.{FileSystems, Files}
import scala.collection.JavaConverters._

trait Fixtures {

  def gridApi: GridApi

  def uploadImage(filename: String): Image = {
    val image = getClass.getResourceAsStream(filename).readAllBytes()
    val imageUploadResponse = gridApi.loadImage(image)
    val uploadStatusResponse = gridApi.getUploadStatusByURI(imageUploadResponse.right.get.uri)
    val imageUri = uploadStatusResponse.get.uri
    val imageId = imageUri.split("/").last
    // TODO needs to poll for image indexing
    gridApi.getImage(imageId).get
  }

  def getFilesInFolder(folderName: String) = {
    val folder = getClass.getResource(folderName)
    val path = FileSystems.getDefault.getPath(folder.getPath)
    Files.list(path).iterator().asScala.toSeq.map(folderName + "/" + _.toFile.getName)
  }

}
