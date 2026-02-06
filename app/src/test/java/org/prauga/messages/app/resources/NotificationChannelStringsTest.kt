/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.app.resources

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.app.R
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for notification channel string resources.
 * 
 * These tests verify that all notification channel strings are properly
 * defined in string resources and are non-empty.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class NotificationChannelStringsTest {

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    // Default Channel Tests
    @Test
    fun defaultChannelName_shouldBeNonEmpty() {
        val name = context.getString(R.string.notification_channel_default_name)
        
        assertNotNull("Default channel name should not be null", name)
        assertFalse("Default channel name should not be empty", name.isEmpty())
    }

    @Test
    fun defaultChannelDescription_shouldBeNonEmpty() {
        val description = context.getString(R.string.notification_channel_default_description)
        
        assertNotNull("Default channel description should not be null", description)
        assertFalse("Default channel description should not be empty", description.isEmpty())
    }

    // Backup & Restore Channel Tests
    @Test
    fun backupRestoreChannelName_shouldBeNonEmpty() {
        val name = context.getString(R.string.notification_channel_backup_restore_name)
        
        assertNotNull("Backup restore channel name should not be null", name)
        assertFalse("Backup restore channel name should not be empty", name.isEmpty())
    }

    @Test
    fun backupRestoreChannelDescription_shouldBeNonEmpty() {
        val description = context.getString(R.string.notification_channel_backup_restore_description)
        
        assertNotNull("Backup restore channel description should not be null", description)
        assertFalse("Backup restore channel description should not be empty", description.isEmpty())
    }

    // Resource Existence Tests
    @Test
    fun allNotificationChannelStrings_shouldExist() {
        val stringIds = listOf(
            R.string.notification_channel_default_name,
            R.string.notification_channel_default_description,
            R.string.notification_channel_backup_restore_name,
            R.string.notification_channel_backup_restore_description
        )

        stringIds.forEach { stringId ->
            val resourceName = context.resources.getResourceEntryName(stringId)
            val value = context.getString(stringId)
            
            assertNotNull("String resource $resourceName should not be null", value)
            assertFalse("String resource $resourceName should not be empty", value.isEmpty())
        }
    }

    // Localization Tests
    @Test
    fun notificationChannelStrings_shouldNotContainPlaceholders() {
        val strings = listOf(
            context.getString(R.string.notification_channel_default_name),
            context.getString(R.string.notification_channel_default_description),
            context.getString(R.string.notification_channel_backup_restore_name),
            context.getString(R.string.notification_channel_backup_restore_description)
        )

        strings.forEach { string ->
            assertFalse("String should not contain untranslated placeholder markers",
                string.contains("TODO") || string.contains("FIXME"))
        }
    }

    // Content Validation Tests
    @Test
    fun channelNames_shouldBeConcise() {
        val defaultName = context.getString(R.string.notification_channel_default_name)
        val backupName = context.getString(R.string.notification_channel_backup_restore_name)

        // Channel names should be reasonably short (under 50 characters is a good guideline)
        assert(defaultName.length < 50) {
            "Default channel name should be concise (under 50 chars), got ${defaultName.length}"
        }
        assert(backupName.length < 50) {
            "Backup channel name should be concise (under 50 chars), got ${backupName.length}"
        }
    }

    @Test
    fun channelDescriptions_shouldBeDescriptive() {
        val defaultDesc = context.getString(R.string.notification_channel_default_description)
        val backupDesc = context.getString(R.string.notification_channel_backup_restore_description)

        // Descriptions should be at least 10 characters to be meaningful
        assert(defaultDesc.length >= 10) {
            "Default channel description should be descriptive (at least 10 chars)"
        }
        assert(backupDesc.length >= 10) {
            "Backup channel description should be descriptive (at least 10 chars)"
        }
    }

    // Consistency Tests
    @Test
    fun allChannels_shouldHaveBothNameAndDescription() {
        // Default channel
        val defaultName = context.getString(R.string.notification_channel_default_name)
        val defaultDesc = context.getString(R.string.notification_channel_default_description)
        
        assertFalse("Default channel should have name", defaultName.isEmpty())
        assertFalse("Default channel should have description", defaultDesc.isEmpty())

        // Backup/Restore channel
        val backupName = context.getString(R.string.notification_channel_backup_restore_name)
        val backupDesc = context.getString(R.string.notification_channel_backup_restore_description)
        
        assertFalse("Backup channel should have name", backupName.isEmpty())
        assertFalse("Backup channel should have description", backupDesc.isEmpty())
    }

    // Format Tests
    @Test
    fun channelStrings_shouldNotHaveLeadingOrTrailingWhitespace() {
        val strings = listOf(
            context.getString(R.string.notification_channel_default_name),
            context.getString(R.string.notification_channel_default_description),
            context.getString(R.string.notification_channel_backup_restore_name),
            context.getString(R.string.notification_channel_backup_restore_description)
        )

        strings.forEach { string ->
            assert(string == string.trim()) {
                "String should not have leading or trailing whitespace: '$string'"
            }
        }
    }

    @Test
    fun channelStrings_shouldNotBeAllUppercase() {
        val names = listOf(
            context.getString(R.string.notification_channel_default_name),
            context.getString(R.string.notification_channel_backup_restore_name)
        )

        names.forEach { name ->
            // Allow all uppercase only if it's an acronym (very short)
            if (name.length > 5) {
                assertFalse("Channel name should not be all uppercase: '$name'",
                    name == name.uppercase() && name.any { it.isLetter() })
            }
        }
    }
}
