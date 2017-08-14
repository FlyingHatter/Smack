/**
 *
 * Copyright 2017 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle_filetransfer.controller;

import org.jivesoftware.smackx.jingle.JingleDescriptionController;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFileTransferFile;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;

/**
 * User interface for Jingle file transfers.
 */
public interface JingleFileTransferController extends JingleDescriptionController {

    void addProgressListener(ProgressListener listener);

    void removeProgressListener(ProgressListener listener);

    JingleFileTransferFile getFile();
}
