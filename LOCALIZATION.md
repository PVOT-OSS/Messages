# Localization Guide

This guide explains how to add support for new locales in the Messages app, specifically for OTP (One-Time Password) detection features.

## Overview

The Messages app uses Android's standard resource system for localization. OTP detection keywords, safety phrases, money indicators, and error messages are all stored in resource files that can be easily translated.

## Adding a New Locale

### Step 1: Create Locale Directory

Create a new values directory for your locale in the app module:

```
app/src/main/res/values-{locale}/
```

**Examples:**
- Hindi: `values-hi/`
- Spanish: `values-es/`
- French: `values-fr/`
- German: `values-de/`
- Chinese (Simplified): `values-zh-rCN/`
- Arabic: `values-ar/`

### Step 2: Create arrays.xml

Create `arrays.xml` in your locale directory with three string arrays:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- OTP Keywords: Terms commonly used in OTP/verification messages -->
    <string-array name="otp_keywords">
        <item>otp</item>
        <item>one time password</item>
        <item>verification code</item>
        <!-- Add more keywords in your language -->
    </string-array>

    <!-- Safety Keywords: Phrases that indicate security warnings -->
    <string-array name="otp_safety_keywords">
        <item>do not share</item>
        <item>never share</item>
        <item>expires in</item>
        <!-- Add more safety phrases in your language -->
    </string-array>

    <!-- Money Indicators: Terms that suggest financial transactions -->
    <string-array name="otp_money_indicators">
        <item>balance</item>
        <item>amount</item>
        <item>debited</item>
        <item>credited</item>
        <!-- Add currency symbols and terms in your language -->
    </string-array>
</resources>
```

### Step 3: Create strings.xml

Create `strings.xml` in your locale directory with error messages:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- OTP Error Messages -->
    <string name="otp_error_empty_message">Empty message</string>
    <string name="otp_error_no_keyword">No OTP-like keywords and no candidate code found</string>
    <string name="otp_error_keyword_no_code">Contains OTP-like keywords but no numeric/alphanumeric candidate code found</string>
</resources>
```

## Translation Templates

### Hindi Example (values-hi/)

See `app/src/main/res/values-hi/arrays.xml` and `app/src/main/res/values-hi/strings.xml` for a complete example.

### Spanish Template (values-es/)

**arrays.xml:**
```xml
<string-array name="otp_keywords">
    <item>otp</item>
    <item>contraseña de un solo uso</item>
    <item>código de verificación</item>
    <item>código de acceso</item>
    <item>código de seguridad</item>
    <item>código</item>
</string-array>

<string-array name="otp_safety_keywords">
    <item>no compartir</item>
    <item>nunca compartir</item>
    <item>válido por</item>
    <item>expira en</item>
</string-array>

<string-array name="otp_money_indicators">
    <item>saldo</item>
    <item>monto</item>
    <item>debitado</item>
    <item>acreditado</item>
    <item>€</item>
    <item>$</item>
</string-array>
```

### French Template (values-fr/)

**arrays.xml:**
```xml
<string-array name="otp_keywords">
    <item>otp</item>
    <item>mot de passe à usage unique</item>
    <item>code de vérification</item>
    <item>code d'accès</item>
    <item>code de sécurité</item>
    <item>code</item>
</string-array>

<string-array name="otp_safety_keywords">
    <item>ne pas partager</item>
    <item>ne jamais partager</item>
    <item>valable pour</item>
    <item>expire dans</item>
</string-array>

<string-array name="otp_money_indicators">
    <item>solde</item>
    <item>montant</item>
    <item>débité</item>
    <item>crédité</item>
    <item>€</item>
    <item>$</item>
</string-array>
```

## Localizable Resources

### Required Resources

1. **otp_keywords** - Terms used to identify OTP messages
   - Include common variations of "OTP", "verification code", "login code", etc.
   - Include both formal and informal terms
   - Include abbreviations (2FA, MFA, etc.)

2. **otp_safety_keywords** - Security warning phrases
   - "Do not share" variations
   - Expiry-related phrases ("expires in", "valid for")
   - Security warnings

3. **otp_money_indicators** - Financial transaction terms
   - Currency symbols (₹, $, €, £, etc.)
   - Currency codes (INR, USD, EUR, GBP, etc.)
   - Transaction terms ("balance", "debited", "credited", "amount")

4. **Error Messages** - User-facing error strings
   - `otp_error_empty_message`
   - `otp_error_no_keyword`
   - `otp_error_keyword_no_code`

### Translation Tips

1. **Keep it lowercase**: All keywords are automatically converted to lowercase for matching
2. **Include variations**: Add common misspellings and variations
3. **Think local**: Include terms specific to your region's banking/service providers
4. **Test thoroughly**: Test with real OTP messages in your language

## Testing Your Translation

1. Build the app with your new locale
2. Change your device language to the new locale
3. Test OTP detection with real messages in that language
4. Verify that:
   - OTP codes are correctly detected
   - Error messages appear in the correct language
   - Keywords match common message formats

## Contributing

To contribute your translation:

1. Create the locale directory and resource files
2. Test thoroughly with real messages
3. Submit a pull request with:
   - Your locale files
   - A brief description of what you tested
   - Any locale-specific notes

## Need Help?

- Check existing locale files for examples
- See the Hindi translation (`values-hi/`) as a reference
- Open an issue if you have questions

## Locale Codes Reference

Common locale codes:
- `hi` - Hindi
- `es` - Spanish
- `fr` - French
- `de` - German
- `it` - Italian
- `pt` - Portuguese
- `ru` - Russian
- `ja` - Japanese
- `ko` - Korean
- `zh-rCN` - Chinese (Simplified)
- `zh-rTW` - Chinese (Traditional)
- `ar` - Arabic
- `tr` - Turkish
- `pl` - Polish
- `nl` - Dutch

For a complete list, see: https://developer.android.com/guide/topics/resources/providing-resources#AlternativeResources
