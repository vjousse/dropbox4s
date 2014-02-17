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

import com.dropbox.core.{DbxRequestConfig, DbxClient}
import dropbox4s.commons.auth.AccessToken
import java.util.Locale
import dropbox4s.core.model.DropboxPath

/**
 * @author mao.instantlife at gmail.com
 */
trait CoreApi {
  val applicationName: String
  val version: String
  val locale: Locale

  lazy val clientIdentifier = s"${applicationName}/${version} dropbox4s/0.0.1"
  lazy val requestConfig = new DbxRequestConfig(clientIdentifier, locale.toString)
  lazy val client = new DbxClient(requestConfig, _: String)

  def accountInfo(implicit token: AccessToken) = client(token.token).getAccountInfo

  implicit class DbxRichFile(val file: java.io.File) {
    def uploadTo(to: DropboxPath)(implicit token: AccessToken) = {
      // This is fake. TODO implements
    }
  }
}
