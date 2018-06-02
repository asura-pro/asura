package asura.core.redis

import java.util

import asura.common.util.LogUtils
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.es.model.Job
import asura.core.redis.RedisClient.{redisson, toScala}
import com.typesafe.scalalogging.Logger
import org.redisson.client.codec.StringCodec

import scala.collection.JavaConverters.setAsJavaSet
import scala.concurrent.Future
import scala.util.{Failure, Success}

object RedisJobState {

  val logger = Logger("RedisJobState")
  val KEY_JOB_STATE = "asura_job_state"

  def updateJobState(scheduler: String, jobGroup: String, jobName: String, state: String)(successBlock: => Unit): Unit = {
    val jobStates = redisson.getMap[String, String](KEY_JOB_STATE, StringCodec.INSTANCE)
    val key = Job.buildJobKey(scheduler, jobGroup, jobName)
    jobStates.fastPutAsync(key, state).onComplete {
      case Failure(t) =>
        logger.error(LogUtils.stackTraceToString(t))
      case Success(_) =>
        logger.debug(s"update $key to $state successful.")
        successBlock
    }
  }

  def deleteJobState(scheduler: String, jobGroup: String, jobName: String)(successBlock: => Unit): Unit = {
    val jobStates = redisson.getMap[String, String](KEY_JOB_STATE, StringCodec.INSTANCE)
    val key = Job.buildJobKey(scheduler, jobGroup, jobName)
    jobStates.fastRemoveAsync(key).onComplete {
      case Failure(t) =>
        logger.error(LogUtils.stackTraceToString(t))
      case Success(_) =>
        logger.debug(s"delete $key state.")
        successBlock
    }
  }

  def getJobState(keys: Set[String]): Future[util.Map[String, String]] = {
    val jobStates = redisson.getMap[String, String](KEY_JOB_STATE, StringCodec.INSTANCE)
    jobStates.getAllAsync(setAsJavaSet(keys))
  }
}
