/*
 * SPDX-FileCopyrightText: 2025 Ben Iofel <ben@iofel.me>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.content.Context
import com.squareup.moshi.Moshi
import io.realm.Realm
import io.realm.Sort
import javax.inject.Inject
import org.prauga.messages.extensions.insertOrUpdate
import org.prauga.messages.manager.KeyManager
import org.prauga.messages.model.EmojiReaction
import org.prauga.messages.model.EmojiSyncNeeded
import org.prauga.messages.model.Message
import org.prauga.messages.util.EmojiPatternStrings
import timber.log.Timber

class EmojiReactionRepositoryImpl
@Inject
constructor(
    private val context: Context,
    private val keyManager: KeyManager,
    private val moshi: Moshi,
) : EmojiReactionRepository {
    // We use an ordered map to make sure we can test tapback regexes before generic ones
    private val reactionPatterns: LinkedHashMap<Regex, (MatchResult) -> ParsedEmojiReaction?> =
        linkedMapOf(
            Regex( // Google Messages
                "(?s)^\u200a[^\u200b\u200a]*\u200b([^\u200b]*)\u200b[^\u200b\u200a]*\u200a(.*)\u200a[^\u200b\u200a]*\u200a\\Z"
            ) to { match -> ParsedEmojiReaction(match.groupValues[1], match.groupValues[2]) }
        )
    private val removalPatterns: LinkedHashMap<Regex, (MatchResult) -> ParsedEmojiReaction?> =
        linkedMapOf(
            Regex( // Google Messages
                "(?s)^\u200a[^\u200c\u200a]*\u200c([^\u200c]*)\u200c[^\u200c\u200a]*\u200a(.*)\u200a[^\u200c\u200a]*\u200a\\Z"
            ) to
                { match ->
                    ParsedEmojiReaction(
                        match.groupValues[1],
                        match.groupValues[2],
                        isRemoval = true,
                    )
                }
        )

    init {
        val assetEntries = loadEmojiPatternEntriesFromAssets()
        assetEntries.forEach { (localeTag, strings) ->
            try {
                addPatternsForLocaleStrings(localeTag, strings, reactionPatterns, removalPatterns)
            } catch (e: Exception) {
                Timber.w(e, "Failed to load asset patterns for locale: $localeTag")
            }
        }
        Timber.i("Loaded emoji reaction patterns for locales: ${assetEntries.map { it.first }}")
    }

    private fun addPatternsForLocaleStrings(
        localeTag: String,
        strings: EmojiPatternStrings,
        reactionPatterns: LinkedHashMap<Regex, (MatchResult) -> ParsedEmojiReaction?>,
        removalPatterns: LinkedHashMap<Regex, (MatchResult) -> ParsedEmojiReaction?>,
    ) {
        // iOS tapbacks (important to add these before generic emoji patterns as the regexes may
        // overlap)
        listOf(
                Triple("❤️", strings.iosHeartAdded, strings.iosHeartRemoved),
                Triple("👍", strings.iosLikeAdded, strings.iosLikeRemoved),
                Triple("👎", strings.iosDislikeAdded, strings.iosDislikeRemoved),
                Triple("😂", strings.iosLaughAdded, strings.iosLaughRemoved),
                Triple("‼️", strings.iosExclamationAdded, strings.iosExclamationRemoved),
                Triple("❓", strings.iosQuestionMarkAdded, strings.iosQuestionMarkRemoved),
            )
            .forEach { (emoji, added, removed) ->
                added?.let {
                    reactionPatterns[Regex(it)] = { match ->
                        ParsedEmojiReaction(emoji, match.groupValues[1])
                    }
                }
                removed?.let {
                    removalPatterns[Regex(it)] = { match ->
                        ParsedEmojiReaction(emoji, match.groupValues[1], isRemoval = true)
                    }
                }
            }

        // Generic iOS emoji patterns
        strings.iosGenericAdded?.let { pattern ->
            reactionPatterns[Regex(pattern)] = { match ->
                if (match.groupValues.getOrNull(1) == "with a sticker")
                    null // TODO: localize "with a sticker"
                else ParsedEmojiReaction(match.groupValues[1], match.groupValues[2])
            }
        }
        strings.iosGenericRemoved?.let { pattern ->
            removalPatterns[Regex(pattern)] = { match ->
                ParsedEmojiReaction(match.groupValues[1], match.groupValues[2], isRemoval = true)
            }
        }

        Timber.d("Loaded emoji regex patterns for $localeTag from assets")
    }

    private fun loadEmojiPatternEntriesFromAssets(): List<Pair<String, EmojiPatternStrings>> {
        val dir = "emojis"
        val files = context.assets.list(dir) ?: emptyArray()
        return files
            .filter { it.endsWith(".json", ignoreCase = true) }
            .mapNotNull { filename ->
                val localeTag = filename.removeSuffix(".json")
                try {
                    val json =
                        context.assets.open("$dir/$filename").bufferedReader().use { it.readText() }
                    val data = parseEmojiPatternsJson(json)
                    localeTag to data
                } catch (e: Exception) {
                    Timber.w(e, "Failed parsing emoji patterns asset: $filename")
                    null
                }
            }
    }

    private fun parseEmojiPatternsJson(json: String): EmojiPatternStrings {
        val adapter = moshi.adapter(EmojiPatternStrings::class.java)
        return requireNotNull(adapter.fromJson(json)) { "Invalid emoji patterns JSON" }
    }

    override fun buildOutgoingReactionBody(
        emoji: String,
        targetMessageText: String,
        isRemoval: Boolean,
    ): String? {
        if (targetMessageText.isBlank()) return null

        val text = targetMessageText.trim()
        val (added, removed) =
            when (emoji) {
                "❤️" -> "Loved “$text”" to "Removed a heart from “$text”"
                "👍" -> "Liked “$text”" to "Removed a like from “$text”"
                "👎" -> "Disliked “$text”" to "Removed a dislike from “$text”"
                "😂" -> "Laughed at “$text”" to "Removed a laugh from “$text”"
                "‼️" -> "Emphasized “$text”" to "Removed an exclamation from “$text”"
                "❓" -> "Questioned “$text”" to "Removed a question mark from “$text”"
                else -> "Reacted $emoji to “$text”" to "Removed $emoji from “$text”"
            }

        return if (isRemoval) removed else added
    }

    override fun parseEmojiReaction(body: String): ParsedEmojiReaction? {
        val removal = parseRemoval(body)
        if (removal != null) return removal

        for ((pattern, parser) in reactionPatterns) {
            val match = pattern.find(body)
            if (match == null) continue

            val result = parser(match)
            if (result == null) continue

            Timber.d("Reaction found with ${result.emoji} for message: '${result.originalMessage}'")
            return result
        }

        return null
    }

    private fun parseRemoval(body: String): ParsedEmojiReaction? {
        for ((pattern, parser) in removalPatterns) {
            val match = pattern.find(body)
            if (match == null) continue

            val result = parser(match)
            if (result == null) continue

            Timber.d("Removal found with ${result.emoji}")
            return result
        }

        return null
    }

    /**
     * Search for messages in the same thread with matching text content We'll search recent
     * messages first
     */
    override fun findTargetMessage(
        threadId: Long,
        originalMessageText: String,
        realm: Realm,
    ): Message? {
        val text = originalMessageText.trim()
        val match =
            realm
                .where(Message::class.java)
                .equalTo("threadId", threadId)
                .beginGroup()
                .contains("body", text)
                .or()
                .contains("parts.text", text.lineSequence().first())
                .endGroup()
                .sort("date", Sort.DESCENDING)
                .findAll()
                .find { message -> message.getText(false).trim() == text }

        if (match == null) {
            Timber.w("No target message found for a reaction in thread $threadId")
        }
        return match
    }

    private fun removeEmojiReaction(
        reactionMessage: Message,
        reaction: ParsedEmojiReaction,
        targetMessage: Message?,
        realm: Realm,
    ) {
        if (targetMessage == null) {
            Timber.w("Cannot remove emoji reaction '${reaction.emoji}': no target message found")
            return
        }

        val existingReaction =
            targetMessage.emojiReactions.find { candidate ->
                candidate.senderAddress == reactionMessage.address &&
                    candidate.emoji == reaction.emoji
            }

        if (existingReaction != null) {
            existingReaction.deleteFromRealm()
            Timber.d("Removed emoji reaction: ${reaction.emoji} to message ${targetMessage.id}")
        } else {
            Timber.w(
                "No existing emoji reaction found to remove: ${reaction.emoji} to message ${targetMessage.id}"
            )
        }

        reactionMessage.isEmojiReaction = true
        realm.insertOrUpdate(reactionMessage)
    }

    override fun saveEmojiReaction(
        reactionMessage: Message,
        parsedReaction: ParsedEmojiReaction,
        targetMessage: Message?,
        realm: Realm,
    ) {
        if (parsedReaction.isRemoval) {
            removeEmojiReaction(reactionMessage, parsedReaction, targetMessage, realm)
            return
        }

        val reaction =
            EmojiReaction().apply {
                id = keyManager.newId()
                reactionMessageId = reactionMessage.id
                senderAddress = reactionMessage.address
                emoji = parsedReaction.emoji
                originalMessageText = parsedReaction.originalMessage
                threadId = reactionMessage.threadId
            }
        realm.insertOrUpdate(reaction)

        if (targetMessage != null) {
            reactionMessage.isEmojiReaction = true
            realm.insertOrUpdate(reactionMessage)

            // Overwrite any previous reaction from this sender for this target
            val priorFromSender =
                targetMessage.emojiReactions.filter { it.senderAddress == reaction.senderAddress }
            priorFromSender.forEach { it.deleteFromRealm() }

            targetMessage.emojiReactions.add(reaction)

            Timber.i("Saved emoji reaction: ${reaction.emoji} to message ${targetMessage.id}")
        } else {
            Timber.w("No target message, cannot save emoji reaction: ${reaction.emoji}")
        }
    }

    internal fun clearEmojiReactionFlags(realm: Realm) {
        realm
            .where(Message::class.java)
            .equalTo("isEmojiReaction", true)
            .findAll()
            .setBoolean("isEmojiReaction", false)
    }

    override fun deleteAndReparseAllEmojiReactions(realm: Realm) {
        val startTime = System.currentTimeMillis()

        realm.executeTransaction { r ->
            r.delete(EmojiReaction::class.java)
            clearEmojiReactionFlags(r)
        }

        val allMessages =
            realm
                .where(Message::class.java)
                .beginGroup()
                .beginGroup()
                .equalTo("type", "sms")
                .isNotEmpty("body")
                .endGroup()
                .or()
                .beginGroup()
                .equalTo("type", "mms")
                .notEqualTo("messageType", 130.toLong())
                .isNotEmpty("parts.text")
                .endGroup()
                .endGroup()
                .sort(
                    "date",
                    Sort.ASCENDING,
                ) // parse oldest to newest to handle reactions & removals properly
                .findAll()

        var pending = 0
        realm.beginTransaction()
        try {
            allMessages.forEach { message ->
                val parsedReaction = parseEmojiReaction(message.getText(false))
                if (parsedReaction != null) {
                    val targetMessage =
                        findTargetMessage(message.threadId, parsedReaction.originalMessage, realm)
                    saveEmojiReaction(message, parsedReaction, targetMessage, realm)
                    if (++pending >= REPARSE_BATCH_SIZE) {
                        realm.commitTransaction()
                        realm.beginTransaction()
                        pending = 0
                    }
                }
            }
            realm.commitTransaction()
        } catch (e: Throwable) {
            if (realm.isInTransaction) realm.cancelTransaction()
            throw e
        }

        val endTime = System.currentTimeMillis()
        Timber.d("Deleted and reparsed all emoji reactions in ${endTime - startTime}ms")
    }

    override fun reparseIfNeeded() {
        Realm.getDefaultInstance().use { realm ->
            if (realm.where(EmojiSyncNeeded::class.java).count() == 0L) return
            deleteAndReparseAllEmojiReactions(realm)
            realm.executeTransaction { r -> r.delete(EmojiSyncNeeded::class.java) }
        }
    }

    companion object {
        const val REPARSE_BATCH_SIZE = 2_000
    }
}
