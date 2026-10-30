# VoClone
<!-- impeccable:product-schema 1 -->
## Platform
android
## Stack
Kotlin, Jetpack Compose, Material 3, JNI/C++. Selected by the user.
## Users
The owner uses a POCO F6, 12 GB RAM, Android 16 ARM64, to create speech from voices they own or have permission to use.
## Product Purpose
Private offline voice cloning: import/record, validate reference, transcribe or correct transcript, generate a preview, save a voice, synthesize text, play/export/share audio.
## Capabilities and Constraints
AI models are downloaded once, with licence consent and integrity verification. No inference APIs. Do not promise quality, speed, languages or RAM without measured evidence. Real generated audio is required before saving a profile. Android user confirmation is required for APK installation.
## Brand Commitments
VoClone; supplied microphone icon and phone UI. White and blue, dark mode, Plus Jakarta Sans, one scrolling cloning screen, settings at top right, no bottom navigation.
## Evidence on Hand
User supplied icon and screen reference. Empty upstream project with AGPL-3.0. VoiceStudio native sources available. No physical device attached at initial audit.
## Product Principles
Asset-first. Verification-first. Native first. No simulated AI or fake progress. Data stays in app-private storage and Android backup excludes voice/model data.
## Accessibility & Inclusion
Material 48dp targets, scalable text, content descriptions, contrast >=4.5:1, system dark mode and reduced motion.
