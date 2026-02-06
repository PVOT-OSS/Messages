# Migration Guide: Modular String Externalization

This guide documents the refactoring that moved hardcoded strings, constants, and configuration values into appropriate resource files and configuration objects.

## Overview

The refactoring improves code maintainability and enables easier localization by:
- Moving OTP keywords to Android resource files
- Creating configuration objects for patterns, animations, and file naming
- Externalizing UI dimensions to dimens.xml
- Enabling locale-based translations without code changes

## What Changed

### 1. OTP Constants → Resources + Config

**Before:**
```kotlin
// OtpConstants.kt
internal object OtpConstants {
    val OTP_KEYWORDS = listOf("otp", "verification code", ...)
    val SAFETY_KEYWORDS = listOf("do not share", ...)
    val MONEY_INDICATORS = listOf("rs", "balance", ...)
    
    const val EMPTY_MESSAGE = "Empty message"
    const val NO_OTP_KEYWORD_MSG = "No OTP-like keywords..."
    
    const val WHITESPACE_REGEX = "\\s+"
    const val NUMERIC_REGEX = "\\b\\d{3,10}\\b"
    ...
}
```

**After:**
```kotlin
// OtpResourceProvider.kt - Interface for loading resources
interface OtpResourceProvider {
    fun getOtpKeywords(): List<String>
    fun getSafetyKeywords(): List<String>
    fun getMoneyIndicators(): List<String>
    fun getErrorMessage(errorType: OtpErrorType): String
}

// OtpResourceProviderImpl.kt - Implementation
class OtpResourceProviderImpl(private val context: Context) : OtpResourceProvider {
    override fun getOtpKeywords(): List<String> {
        return context.resources.getStringArray(R.array.otp_keywords)
            .map { it.lowercase() }
    }
    // ...
}

// OtpPatternConfig.kt - Regex patterns
object OtpPatternConfig {
    const val WHITESPACE_REGEX = "\\s+"
    const val NUMERIC_REGEX = "\\b\\d{3,10}\\b"
    // ...
}
```

**Resources:**
```xml
<!-- app/src/main/res/values/arrays.xml -->
<string-array name="otp_keywords">
    <item>otp</item>
    <item>verification code</item>
    ...
</string-array>

<!-- app/src/main/res/values/strings.xml -->
<string name="otp_error_empty_message">Empty message</string>
```

### 2. OTP Detector Usage

**Before:**
```kotlin
val otpDetector = OtpDetector()
val result = otpDetector.detect(messageText)
```

**After:**
```kotlin
val resourceProvider = OtpResourceProviderImpl(context)
val otpDetector = OtpDetector(resourceProvider)
val result = otpDetector.detect(messageText)
```

### 3. UI Dimensions

**Before:**
```kotlin
private val iconLength = 24.dpToPx(context)
val paddingBottom = 25.dpToPx(context)
layoutParams.topMargin = 24.dpToPx(context)
```

**After:**
```kotlin
private val iconLength = context.resources.getDimensionPixelSize(R.dimen.icon_size_small)
val paddingBottom = context.resources.getDimensionPixelSize(R.dimen.padding_reactions)
layoutParams.topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_chip_top)
```

**Resources:**
```xml
<!-- app/src/main/res/values/dimens.xml -->
<dimen name="icon_size_small">24dp</dimen>
<dimen name="padding_reactions">25dp</dimen>
<dimen name="margin_chip_top">24dp</dimen>
```

### 4. Animation Durations

**Before:**
```kotlin
startAnimation(AlphaAnimation(0f, 1f).apply { duration = 200 })
```

**After:**
```kotlin
import org.prauga.messages.common.util.AnimationConfig

startAnimation(AlphaAnimation(0f, 1f).apply { 
    duration = AnimationConfig.FADE_DURATION_MS 
})
```

**Configuration:**
```kotlin
// AnimationConfig.kt
object AnimationConfig {
    const val FADE_DURATION_MS = 200L
    const val CHIP_ANIMATION_DURATION_MS = 200L
    // ...
}
```

### 5. File Naming

**Before:**
```kotlin
const val SAVED_MESSAGE_TEXT_FILE_PREFIX = "QuikSmsText-"
const val AUDIO_FILE_PREFIX = "recorded-"
const val DEFAULT_SHARE_FILENAME = "quik-media-attachment.jpg"
```

**After:**
```kotlin
import org.prauga.messages.common.util.FileNamingConfig

val filename = FileNamingConfig.SAVED_MESSAGE_TEXT_PREFIX + timestamp
val audioFile = FileNamingConfig.AUDIO_FILE_PREFIX + timestamp + FileNamingConfig.AUDIO_FILE_SUFFIX
```

**Configuration:**
```kotlin
// FileNamingConfig.kt
object FileNamingConfig {
    const val SAVED_MESSAGE_TEXT_PREFIX = "QuikSmsText-"
    const val AUDIO_FILE_PREFIX = "recorded-"
    const val AUDIO_FILE_SUFFIX = ".3ga"
    const val DEFAULT_GALLERY_SHARE_FILENAME = "quik-media-attachment.jpg"
    const val DEFAULT_AUDIO_SHARE_FILENAME = "quik-audio-attachment.mp3"
}
```

## New Resource File Structure

```
app/src/main/res/
├── values/
│   ├── arrays.xml          # OTP keywords, safety keywords, money indicators
│   ├── strings.xml         # OTP error messages
│   └── dimens.xml          # UI dimensions
├── values-hi/              # Hindi locale (example)
│   ├── arrays.xml          # Translated OTP keywords
│   └── strings.xml         # Translated error messages
└── values-{locale}/        # Other locales
    ├── arrays.xml
    └── strings.xml
```

## Configuration Objects Location

All new configuration objects are in:
```
app/src/main/java/org/prauga/messages/common/util/
├── OtpResourceProvider.kt
├── OtpResourceProviderImpl.kt
├── OtpPatternConfig.kt
├── AnimationConfig.kt
└── FileNamingConfig.kt
```

## Migration Checklist for Developers

When adding new features:

### For OTP-related features:
- [ ] Use `OtpResourceProvider` to access keywords
- [ ] Use `OtpPatternConfig` for regex patterns
- [ ] Add new keywords to `arrays.xml` (not code)
- [ ] Add new error messages to `strings.xml` (not code)

### For UI dimensions:
- [ ] Add new dimensions to `dimens.xml`
- [ ] Use `context.resources.getDimensionPixelSize(R.dimen.xxx)`
- [ ] Never hardcode dp values in code

### For animations:
- [ ] Add new durations to `AnimationConfig`
- [ ] Use named constants instead of magic numbers
- [ ] Document the purpose of each duration

### For file operations:
- [ ] Add new naming conventions to `FileNamingConfig`
- [ ] Use constants for prefixes, suffixes, and default names
- [ ] Document the format and use case

## Adding New Locales

See [LOCALIZATION.md](LOCALIZATION.md) for detailed instructions on adding support for new languages.

Quick steps:
1. Create `app/src/main/res/values-{locale}/` directory
2. Copy and translate `arrays.xml` (OTP keywords)
3. Copy and translate `strings.xml` (error messages)
4. Test with real messages in that language

## Benefits of This Refactoring

1. **Easier Localization**: Contributors can add new locales by creating resource files without touching Kotlin code
2. **Maintainability**: Constants are organized by purpose in dedicated configuration objects
3. **Consistency**: UI dimensions and animation timings are centralized
4. **Documentation**: Each constant is documented with its purpose and usage
5. **Testability**: Resource providers can be mocked for testing
6. **Backward Compatibility**: All functionality remains identical

## Breaking Changes

None. This refactoring maintains 100% backward compatibility:
- OTP detection behavior is identical
- UI layout and spacing are unchanged
- Animation timing is preserved
- File naming conventions are the same

## Questions?

- For localization questions, see [LOCALIZATION.md](LOCALIZATION.md)
- For OTP detection issues, check `OtpResourceProvider` and `OtpPatternConfig`
- For UI dimension questions, see `app/src/main/res/values/dimens.xml`
- For general questions, open an issue on GitHub
