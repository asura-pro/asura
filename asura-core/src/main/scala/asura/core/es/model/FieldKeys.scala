package asura.core.es.model

object FieldKeys {

  val FIELD_ID = "id"
  val FIELD_SUMMARY = "summary"
  val FIELD_DESCRIPTION = "description"
  val FIELD_CREATOR = "creator"
  val FIELD_CREATED_AT = "createdAt"
  val FIELD_UPDATED_AT = "updatedAt"
  val FIELD_PATH = "path"
  val FIELD_GROUP = "group"
  val FIELD_PROJECT = "project"
  val FIELD_TYPE = "type"
  val FIELD_SCHEMA = "schema"
  val FIELD_API = "api"
  val FIELD_REQUEST = "request"
  val FIELD_RESPONSE = "response"
  val FIELD_ASSERT = "assert"
  val FIELD_MAP = "map"
  val FIELD_SRC_HOST = "srcHost"
  val FIELD_SRC_PORT = "srcPort"
  val FIELD_TARGET_HOST = "targetHost"
  val FIELD_TARGET_PORT = "targetPort"
  val FIELD_METHOD = "method"
  val FIELD_QUERY = "query"
  val FIELD_HEADER = "header"
  val FIELD_COOKIE = "cookie"
  val FIELD_CONTENT_TYPE = "contentType"
  val FIELD_BODY = "body"
  val FIELD_KEY = "key"
  val FIELD_VALUE = "value"
  val FIELD_VERSION = "version"
  val FIELD_NAME = "name"
  val FIELD_LABELS = "labels"
  val FIELD_DEPRECATED = "deprecated"
  val FIELD_ENABLED = "enabled"
  val FIELD_DEFAULT = "default"
  val FIELD_PROTOCOL = "protocol"
  val FIELD_HOST = "host"
  val FIELD_URL_PATH = "urlPath"
  val FIELD_PORT = "port"
  val FIELD_AUTH = "auth"
  val FIELD_DATA = "data"
  val FIELD_ENV = "env"
  val FIELD_USE_ENV = "useEnv"
  val FIELD_CLASS_ALIAS = "classAlias"
  val FIELD_TRIGGER = "trigger"
  val FIELD_CRON = "cron"
  val FIELD_START_NOW = "startNow"
  val FIELD_START_DATE = "startDate"
  val FIELD_END_DATE = "endDate"
  val FIELD_REPEAT_COUNT = "repeatCount"
  val FIELD_INTERVAL = "interval"
  val FIELD_TRIGGER_TYPE = "triggerType"
  val FIELD_JOB_NAME = "jobName"
  val FIELD_JOB_DATA = "jobData"
  val FIELD_SCHEDULER = "scheduler"
  val FIELD_CS = "cs"
  val FIELD_SCENARIO = "scenario"
  val FIELD_EXT = "ext"
  val FIELD_START_AT = "startAt"
  val FIELD_END_AT = "endAt"
  val FIELD_ELAPSE = "elapse"
  val FIELD_RESULT = "result"
  val FIELD_ERROR_MSG = "errorMsg"
  val FIELD_NODE = "node"
  val FIELD_SERVICE = "service"
  val FIELD_NAMESPACE = "namespace"
  val FIELD_ENABLE_PROXY = "enableProxy"
  val FIELD_CASES = "cases"
  val FIELD_AVATAR = "avatar"
  val FIELD_EMAIL = "email"
  val FIELD_USERNAME = "username"
  val FIELD_NICKNAME = "nickname"
  val FIELD_CUSTOM = "custom"
  val FIELD_RAW_URL = "rawUrl"
  val FIELD_OPENAPI = "openapi"
  val FIELD_JOB_ID = "jobId"
  val FIELD_SUBSCRIBER = "subscriber"
  val FIELD_STEPS = "steps"
  val FIELD_REPORT_ID = "reportId"
  val FIELD_TRIGGER_ID = "triggerId"
  val FIELD_SCENARIO_ID = "scenarioId"
  val FIELD_CASE_ID = "caseId"
  val FIELD_METRICS = "metrics"
  val FIELD_STATIS = "statis"
  val FIELD_CASE_COUNT = "caseCount"
  val FIELD_CASE_OK = "caseOK"
  val FIELD_CASE_KO = "caseKO"
  val FIELD_SCENARIO_COUNT = "scenarioCount"
  val FIELD_SCENARIO_OK = "scenarioOK"
  val FIELD_SCENARIO_KO = "scenarioKO"
  val FIELD_SCENARIO_CASE_COUNT = "scenarioCaseCount"
  val FIELD_SCENARIO_CASE_OK = "scenarioCaseOK"
  val FIELD_SCENARIO_CASE_KO = "scenarioCaseKO"
  val FIELD_SCENARIO_CASE_OO = "scenarioCaseOO"
  val FIELD_Ok_RATE = "okRate"
  val FIELD_ASSERTION_PASSED = "assertionPassed"
  val FIELD_ASSERTION_FAILED = "assertionFailed"
  val FIELD_ASSERTIONS = "assertions"
  val FIELD_ASSERTIONS_RESULT = "assertionsResult"
  val FIELD_USER = "user"
  val FIELD_TIMESTAMP = "timestamp"
  val FIELD_TARGET_ID = "targetId"
  val FIELD_GENERATOR = "generator"
  val FIELD_COUNT = "count"
  val FIELD_LIST = "list"
  val FIELD_SCRIPT = "script"
  val FIELD_HEADERS = "headers"
  val FIELD_DATE = "date"
  val FIELD_DOMAIN = "domain"
  val FIELD_PERCENTAGE = "percentage"
  val FIELD_DOMAINS = "domains"
  val FIELD_INCLUSIONS = "inclusions"
  val FIELD_EXCLUSIONS = "exclusions"
  val FIELD_FIELD = "field"
  val FIELD_BELONGS = "belongs"
  val FIELD_COVERAGE = "coverage"
  val FIELD_COVERED = "covered"
  val FIELD_MAX_API_COUNT = "maxApiCount"
  val FIELD_ALIAS = "alias"
  val FIELD_P25 = "p25"
  val FIELD_P50 = "p50"
  val FIELD_P75 = "p75"
  val FIELD_P90 = "p90"
  val FIELD_P95 = "p95"
  val FIELD_P99 = "p99"
  val FIELD_P999 = "p999"
  val FIELD_MIN = "min"
  val FIELD_AVG = "avg"
  val FIELD_MAX = "max"
  val FIELD_MIN_REQ_COUNT = "minReqCount"
  val FIELD_EX_METHODS = "exMethods"
  val FIELD_EX_SUFFIXES = "exSuffixes"
  val FIELD_TAG = "tag"
  val FIELD_SERVER = "server"
  val FIELD_DUBBO_GROUP = "dubboGroup"
  val FIELD_INTERFACE = "interface"
  val FIELD_PARAMETER_TYPES = "parameterTypes"
  val FIELD_ARGS = "args"
  val FIELD_ADDRESS = "address"
  val FIELD_ZK_CONNECT_STRING = "zkConnectString"
  val FIELD_ZK_USERNAME = "zkUsername"
  val FIELD_ZK_PASSWORD = "zkPassword"
  val FIELD_PASSWORD = "password"
  val FIELD_ENCRYPTED_PASS = "encryptedPass"
  val FIELD_DATABASE = "database"
  val FIELD_TABLE = "table"
  val FIELD_SQL = "sql"
  val FIELD_STORED = "stored"
  val FIELD_IMPORTS = "imports"
  val FIELD_EXPORTS = "exports"
  val FIELD_ENABLE_LB = "enableLb"
  val FIELD_LB_ALGORITHM = "lbAlgorithm"
  val FIELD_TARGET_TYPE = "targetType"
  val FIELD_JOB = "job"
  val FIELD_CHECKED = "checked"
  val FIELD_FAIl_FAST = "failFast"
  val FIELD_READINESS = "readiness"
  val FIELD_DEBOUNCE = "debounce"
  val FIELD_AUTHOR = "author"
  val FIELD_TARGET_GROUP = "targetGroup"
  val FIELD_TARGET_PROJECT = "targetProject"
  val FIELD_COPY_FROM = "copyFrom"
  val FIELD_COMMENT = "comment"
  val FIELD_ROLE = "role"
  val FIELD_EXTENSION = "extension"
  val FIELD_APP = "app"
  val FIELD_PARENT = "parent"
  val FIELD_SIZE = "size"
  val FIELD_TASK_ID = "taskId"
  val FIELD_HOSTNAME = "hostname"
  val FIELD_PID = "pid"
  val FIELD_LEVEL = "level"
  val FIELD_SOURCE = "source"
  val FIELD_TEXT = "text"
  val FIELD_PARAMS = "params"
  val FIELD_DAY = "day"
  val FIELD_SERVOS = "servos"

  val FIELD_OBJECT_REQUEST_PROTOCOL = "request.protocol"
  val FIELD_OBJECT_REQUEST_HOST = "request.host"
  val FIELD_OBJECT_REQUEST_PORT = "request.port"
  val FIELD_OBJECT_REQUEST_URLPATH = "request.urlPath"
  val FIELD_OBJECT_REQUEST_METHOD = "request.method"
  val FIELD_OBJECT_REQUEST_INTERFACE = "request.interface"
  val FIELD_OBJECT_REQUEST_PARAMETER_TYPES = "request.parameterTypes"
  val FIELD_NESTED_TRIGGER_TRIGGER_TYPE = "trigger.triggerType"
  val FIELD_OBJECT_REQUEST_DATABASE = "request.database"
  val FIELD_OBJECT_REQUEST_TABLE = "request.table"
  val FIELD_NESTED_LABELS_NAME = "labels.name"
  val FIELD_NESTED_STEPS_ID = "steps.id"
  val FIELD_NESTED_STEPS_TYPE = "steps.type"
  val FIELD_NESTED_JOB_DATA_CS = "jobData.cs"
  val FIELD_NESTED_JOB_DATA_CS_ID = "jobData.cs.id"
  val FIELD_NESTED_JOB_DATA_SCENARIO = "jobData.scenario"
  val FIELD_NESTED_JOB_DATA_SCENARIO_ID = "jobData.scenario.id"
  val FIELD_NESTED_DOMAINS_NAME = "domains.name"

  /** copied from [[FIELD_SUMMARY]] and [[FIELD_DESCRIPTION]] */
  val FIELD__TEXT = "_text"
  val FIELD__ID = "_id"
  val FIELD__SORT = "_sort"
  val FIELD__SCORE = "_score"
}
