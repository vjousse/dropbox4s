package dropbox4s.datastore.internal

/**
 * @author mao.instantlife at gmail.com
 */

import org.specs2.mutable._
import org.json4s._
import org.json4s.native.JsonMethods._
import dropbox4s.datastore.internal.jsons._
import dropbox4s.datastore.internal.jsons.WrappedTimestamp
import scala.Some
import dropbox4s.datastore.internal.jsons.DsInfo
import dropbox4s.datastore.internal.jsons.ListDatastoresResult
import dropbox4s.datastore.internal.jsons.GetOrCreateDatastoreResult

class JsonConversionTest extends Specification {
  implicit val format = DefaultFormats

  "get_or_create_result" should {
    "json convert to GetOrCreateResult" in {
      val testResult = parse("""{"handle": "1PuUJ3DvMI71OYx1gcqWHzzdva2EpF", "rev": 0, "created": true}""")

      testResult.extract[GetOrCreateDatastoreResult] must equalTo(GetOrCreateDatastoreResult("1PuUJ3DvMI71OYx1gcqWHzzdva2EpF", 0, true))
    }
  }

  "list_datastores_result" should {
    "json convert to ListDatastoresResult without InfoDict" in {
       val testResult = parse(
         """
           |{"datastores": [
           |  {"handle": "1PuUJ3DvMI71OYx1gcqWHzzdva2EpF", "rev": 0, "dsid": "default"}],
           |  "token": "cbd8804428bc888c7262b0193b43407033eb206b3e37bad2cc140591af3ec6f5"}
         """.stripMargin)

      testResult.extract[ListDatastoresResult] must
        equalTo(
          ListDatastoresResult(
            List(
              DsInfo("default", "1PuUJ3DvMI71OYx1gcqWHzzdva2EpF", 0)),
            "cbd8804428bc888c7262b0193b43407033eb206b3e37bad2cc140591af3ec6f5"))
    }

    "json convert to ListDatastoresResult with InfoDict" in {
      val testResult = parse(
        """
          |{"datastores": [
          |   {
          |     "handle": "1PuUJ3DvMI71OYx1gcqWHzzdva2EpF",
          |     "rev": 0,
          |     "dsid": "default",
          |     "info": {"title" : "test-title", "mtime": {"T" : "test-mtime"}}}],
          | "token": "cbd8804428bc888c7262b0193b43407033eb206b3e37bad2cc140591af3ec6f5"}
        """.stripMargin)

      testResult.extract[ListDatastoresResult] must
        equalTo(
          ListDatastoresResult(
            List(
              DsInfo("default", "1PuUJ3DvMI71OYx1gcqWHzzdva2EpF", 0, Some(
                InfoDict("test-title", WrappedTimestamp("test-mtime"))))),
            "cbd8804428bc888c7262b0193b43407033eb206b3e37bad2cc140591af3ec6f5"))
    }
  }
}
