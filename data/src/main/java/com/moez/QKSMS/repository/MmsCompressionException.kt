/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Exception thrown when MMS image compression fails.
 */
package org.prauga.messages.repository

/**
 * Exception thrown when an image attachment cannot be compressed
 * to fit within MMS size limits.
 *
 * @param message Descriptive error message
 * @param fileName The name of the file that failed to compress
 */
class MmsCompressionException(
    message: String,
    val fileName: String?
) : Exception(message)
