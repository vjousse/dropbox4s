package dropbox4s.core

/*
 * Copyright (C) 2014 Shinsuke Abe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.dropbox.core._
import java.util.Locale
import java.io.{File, FileOutputStream, FileInputStream}
import dropbox4s.commons.DropboxException

import collection.JavaConversions._
import dropbox4s.core.model.{CopyRef, DropboxPath}

/**
 * @author mao.instantlife at gmail.com
 */
trait CoreApi {
  val applicationName: String
  val version: String
  val locale: Locale = Locale.getDefault

  lazy val clientIdentifier = s"${applicationName}/${version} dropbox4s/0.2.0"
  lazy val requestConfig = new DbxRequestConfig(clientIdentifier, locale.toString)

  lazy val client = new DbxClient(requestConfig, _: String)

  /**
   * get the user's account information.<br/>
   * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#getAccountInfo%28%29">SDK javadoc</a>
   *
   * @param auth authenticate finish class has access token
   * @return result of DbxClient.getAccountInfo
   */
  def accountInfo(implicit auth: DbxAuthFinish) = client(auth.accessToken).getAccountInfo

  /**
   * search file or folder on Dropbox by path and query of name.<br/>
   * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#searchFileAndFolderNames%28java.lang.String,%20java.lang.String%29">SDK javadoc</a>
   *
   * @param path path to search under
   * @param query A space-separated list of substrings to search for. A file matches only if it contains all the substrings.
   * @param auth authenticate finish class has access token
   * @return result of DbxClient.searchFileAndFolderNames
   */
  def search(path: DropboxPath, query: String)(implicit auth: DbxAuthFinish): List[DbxEntry] =
    client(auth.accessToken).searchFileAndFolderNames(path.path, query).toList

  /**
   * create new folder in Dropbox.<br/>
   * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#createFolder%28java.lang.String%29">SDK javadoc</a>
   *
   * @param path path to create folder
   * @param auth authenticate finish class has access token
   * @return result of DbxClient.createFolder
   */
  def createFolder(path: DropboxPath)(implicit auth: DbxAuthFinish) =
    client(auth.accessToken).createFolder(path.path)

  implicit class DbxRichFile(val localFile: File) {
    /**
     * upload file to Dropbox.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#uploadFile%28java.lang.String,%20com.dropbox.core.DbxWriteMode,%20long,%20java.io.InputStream%29">SDK javadoc(uploadFile)</a>,
     * and see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#uploadFileChunked%28int,%20java.lang.String,%20com.dropbox.core.DbxWriteMode,%20long,%20com.dropbox.core.DbxStreamWriter%29">SDK javadoc(uploadFileChunked)</a>
     *
     * @param to path to upload folder.
     * @param isForce if set to true, force upload. if set to false(default), file renamed automatically.
     * @param chunkSize (optional) file upload chunk size. default is None.
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.uploadFile
     */
    def uploadTo(to: DropboxPath, isForce: Boolean = false, chunkSize: Option[Int] = None)(implicit auth: DbxAuthFinish) = asUploadFile(localFile){ (file, stream) =>
      val mode = if(isForce) DbxWriteMode.force else DbxWriteMode.add

      chunkSize match {
        case Some(chunk) => client(auth.accessToken).uploadFileChunked(chunk, to.path, mode, localFile.length, new DbxStreamWriter.InputStreamCopier(stream))
        case None => client(auth.accessToken).uploadFile(to.path, mode, localFile.length, stream)
      }
    }
  }

  implicit class DbxRichEntryFile(val fileEntity: DbxEntry.File) {
    /**
     * update by new file.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#uploadFile%28java.lang.String,%20com.dropbox.core.DbxWriteMode,%20long,%20java.io.InputStream%29">SDK javadoc</a>,
     * and see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#uploadFileChunked%28int,%20java.lang.String,%20com.dropbox.core.DbxWriteMode,%20long,%20com.dropbox.core.DbxStreamWriter%29">SDK javadoc(uploadFileChunked)</a>
     *
     * @param newFile new file instance for update.
     * @param chunkSize (optional) file upload chunk size. default is None.
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.uploadFile
     */
    def update(newFile: File, chunkSize: Option[Int] = None)(implicit auth: DbxAuthFinish) = asUploadFile(newFile){ (file, stream) =>
      chunkSize match {
        case Some(chunk) => client(auth.accessToken).uploadFileChunked(chunk, fileEntity.path, DbxWriteMode.update(fileEntity.rev), newFile.length, new DbxStreamWriter.InputStreamCopier(stream))
        case None => client(auth.accessToken).uploadFile (fileEntity.path, DbxWriteMode.update(fileEntity.rev), newFile.length, stream)
      }
    }

    /**
     * get thumbnail for file.<br/>
     * if file don't have thumbnail, throw DropboxException.
     * more detail, see the<a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#getThumbnail%28com.dropbox.core.DbxThumbnailSize,%20com.dropbox.core.DbxThumbnailFormat,%20java.lang.String,%20java.lang.String,%20java.io.OutputStream%29">SDK javadoc</a>
     *
     * @param sizeBound The returned thumbnail will never be greater than the dimensions given here.
     * @param to local file path for download thumbnail.
     * @param format The image format to use for thumbnail data. default is DbxThumbnailFormat.PNG.
     * @param auth authenticate finish class has access token
     * @return result of Dbxclient.getThumbnail
     */
    def thumbnail(sizeBound: DbxThumbnailSize, to: String, format: DbxThumbnailFormat = DbxThumbnailFormat.PNG)(implicit auth: DbxAuthFinish) = {
      if(!fileEntity.mightHaveThumbnail) throw DropboxException(s"file have not thumbnail. file = ${fileEntity.toString}")

      asDownloadFile(to) { stream =>
        client(auth.accessToken).getThumbnail(sizeBound, format, fileEntity.path, fileEntity.rev, stream)
      }
    }

    /**
     * restore file by receive file revision.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#restoreFile%28java.lang.String,%20java.lang.String%29">SDK javadoc</a>
     *
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.restoreFile
     */
    def restore(implicit auth: DbxAuthFinish) = client(auth.accessToken).restoreFile(fileEntity.path, fileEntity.rev)
  }

  implicit class RichDropboxPath(val dropboxPath: DropboxPath) {
    /**
     * get metadata of children for a receiver's path.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#getMetadataWithChildren%28java.lang.String%29">SDK javadoc</a>
     *
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.getMetadataWithChildren
     */
    def children(implicit auth: DbxAuthFinish) = client(auth.accessToken).getMetadataWithChildren(dropboxPath.path)

    /**
     * get revisions for a receiver's path.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#getRevisions%28java.lang.String%29">SDK javadoc</a>
     *
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.getRevisions
     */
    def revisions(implicit auth: DbxAuthFinish) = client(auth.accessToken).getRevisions(dropboxPath.path)
  }

  implicit def FileEntryToRichPath(fileEntity: DbxEntry.File) = RichPath(fileEntity.path, fileEntity.rev)
  implicit def DropboxPathToRichPath(dropboxPath: DropboxPath) = RichPath(dropboxPath.path)

  case class RichPath(val path: String, val rev: String = null) {
    /**
     * download file on Dropbox file to local path.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#getFile%28java.lang.String,%20java.lang.String,%20java.io.OutputStream%29">SDK javadoc</a>
     *
     * @param to local path for download.
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.getFile
     */
    def downloadTo(to: String)(implicit auth: DbxAuthFinish) = asDownloadFile(to){ stream =>
      client(auth.accessToken).getFile(path, rev, stream)
    }

    /**
     * remove file on Dropbox for the path.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#delete%28java.lang.String%29">SDK javadoc</a><br/>
     * this method has not result.
     *
     * @param auth authenticate finish class has access token
     */
    def remove(implicit auth: DbxAuthFinish) = client(auth.accessToken).delete(path)

    /**
     * copy file to destination path on Dropbox.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#copy%28java.lang.String,%20java.lang.String%29">SDK javadoc<a/>
     *
     * @param toPath destination path to file copy
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.copy
     */
    def copyTo(toPath: DropboxPath)(implicit auth: DbxAuthFinish) = client(auth.accessToken).copy(path, toPath.path)

    /**
     * move file to destination path on Dropbox.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#move%28java.lang.String,%20java.lang.String%29">SDK javadoc</a>
     *
     * @param toPath destination path to file copy
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.move
     */
    def moveTo(toPath: DropboxPath)(implicit auth: DbxAuthFinish) = client(auth.accessToken).move(path, toPath.path)

    /**
     * create copy ref of receiver path.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#createCopyRef%28java.lang.String%29">SDK javadoc</a>
     *
     * @param auth authenticate finish class has access token
     * @return CopyRef, case class for wrap result of DbxClient.createCopyRef
     */
    def copyRef(implicit auth:DbxAuthFinish) = CopyRef(client(auth.accessToken).createCopyRef(path))

    /**
     * copy file from copy ref to receiver path.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#copyFromCopyRef%28java.lang.String,%20java.lang.String%29">SDK javadoc</a>
     *
     * @param copyRef CopyRef, case class for wrap result of DbxClient.createCopyRef
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.copyFromCopyRef
     */
    def copyFrom(copyRef: CopyRef)(implicit auth:DbxAuthFinish) = client(auth.accessToken).copyFromCopyRef(copyRef.ref, path)

    /**
     * create sharable url of receiver path.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#createShareableUrl%28java.lang.String%29">SDK javadoc</a>
     *
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.createSharableUrl
     */
    def shareLink(implicit auth: DbxAuthFinish) = client(auth.accessToken).createShareableUrl(path)

    /**
     * create temprary direct url of receiver path. the url created this method will stop working after few hours.<br/>
     * more detail, see the <a href="http://dropbox.github.io/dropbox-sdk-java/api-docs/v1.7.x/com/dropbox/core/DbxClient.html#createTemporaryDirectUrl%28java.lang.String%29">SDK javadoc</a>
     *
     * @param auth authenticate finish class has access token
     * @return result of DbxClient.createTemporaryDirectUrl
     */
    def tempDirectLink(implicit auth: DbxAuthFinish) = client(auth.accessToken).createTemporaryDirectUrl(path)
  }

  implicit def MetadataToChildren(metadata: DbxEntry.WithChildren): List[DbxEntry] = metadata.children.toList

  private def asDownloadFile[T](path: String)(f: (FileOutputStream) => T) = {
    val stream = new FileOutputStream(path)

    using(stream, f(stream))
  }

  private def asUploadFile[T](file: File)(f: (File, FileInputStream) => T) = {
    val stream = new FileInputStream(file)

    using(stream, f(file, stream))
  }

  private def using[T](stream: java.io.Closeable, ret: => T) = try {
    ret
  } finally {
    stream.close
  }
}
