/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

/**
 * Exception thrown when an image attachment cannot be compressed to fit within MMS size limits.
 *
 * @param message Descriptive error message
 * @param fileName The name of the file that failed to compress
 */
class MmsCompressionException(message: String, val fileName: String?) : Exception(message)
