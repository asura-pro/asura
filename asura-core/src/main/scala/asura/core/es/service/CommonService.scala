package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.model.BoolErrorRes
import asura.core.es.model._
import asura.core.exceptions.OperateDocFailException
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.delete.DeleteResponse
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.update.UpdateResponse
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}

trait CommonService {

  val defaultIncludeFields = Seq(FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION)
  val defaultExcludeFields = Seq(FieldKeys.FIELD_CREATOR, FieldKeys.FIELD_CREATED_AT)

  def toIndexDocResponse(either: Either[RequestFailure, RequestSuccess[IndexResponse]]): IndexDocResponse = {
    either match {
      case Right(success) =>
        IndexDocResponse(success.result.id)
      case Left(failure) =>
        throw new OperateDocFailException(failure.error.reason)
    }
  }

  def toBulkDocResponse(either: Either[RequestFailure, RequestSuccess[BulkResponse]]): BulkDocResponse = {
    either match {
      case Right(_) =>
        BulkDocResponse()
      case Left(failure) =>
        throw new OperateDocFailException(failure.error.reason)
    }
  }

  def toBoolErrorResFromDelete(either: Either[RequestFailure, RequestSuccess[DeleteResponse]]): BoolErrorRes = {
    either match {
      case Right(_) =>
        (true, null)
      case Left(failure) =>
        throw new OperateDocFailException(failure.error.reason)
    }
  }

  def toBoolErrorResFromUpdate(either: Either[RequestFailure, RequestSuccess[UpdateResponse]]): BoolErrorRes = {
    either match {
      case Right(_) =>
        (true, null)
      case Left(failure) =>
        throw new OperateDocFailException(failure.error.reason)
    }
  }

  def toDeleteDocResponse(either: Either[RequestFailure, RequestSuccess[DeleteResponse]]): DeleteDocResponse = {
    either match {
      case Right(_) =>
        DeleteDocResponse()
      case Left(failure) =>
        throw new OperateDocFailException(failure.error.reason)
    }
  }

  def toDeleteDocResponseFromBulk(either: Either[RequestFailure, RequestSuccess[BulkResponse]]): DeleteDocResponse = {
    either match {
      case Right(_) =>
        DeleteDocResponse()
      case Left(failure) =>
        throw new OperateDocFailException(failure.error.reason)
    }
  }

  def toSingleClass[T](either: Either[RequestFailure, RequestSuccess[SearchResponse]], id: String)(block: String => T): T = {
    either match {
      case Right(success) =>
        if (success.result.isEmpty) {
          block(null)
        } else {
          val hit = success.result.hits.hits(0)
          block(hit.sourceAsString)
        }
      case Left(failure) =>
        throw RequestFailException(failure.error.reason)
    }
  }

  def toUpdateDocResponse(either: Either[RequestFailure, RequestSuccess[UpdateResponse]]): UpdateDocResponse = {
    either match {
      case Right(success) =>
        UpdateDocResponse(id = success.result.id, result = success.result.result)
      case Left(failure) =>
        throw new OperateDocFailException(failure.error.reason)
    }
  }
}
