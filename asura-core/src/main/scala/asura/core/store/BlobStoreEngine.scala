package asura.core.store

import asura.core.es.model.FormDataItem.BlobMetaData

import scala.concurrent.Future

trait BlobStoreEngine {

  val name: String
  val description: String

  def upload(params: UploadParams): Future[BlobMetaData]

  def download(key: String): Future[DownloadParams]
}
