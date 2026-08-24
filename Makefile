# ============================================================
# su-wbw-mobile — task runner
#
# Replaces run.sh. Same tuning for this 15.8GB box, but as targets you can
# compose instead of one script that always does all five steps:
#
#   build the APK FIRST (emulator off) -> free the Gradle daemons -> boot the
#   emulator lean -> install with `adb` -> launch.
#
# The order is the whole point and it is not stylistic. Gradle's daemon and the
# emulator each want ~2GB; started together on this machine they take the
# desktop down with them. So every "run" target below builds to completion,
# stops the daemons, and only then touches a device — and installs with `adb`
# rather than `gradlew installDebug`, which would start a fresh daemon next to
# a running emulator and undo the whole arrangement.
#
# `make` on its own prints every target.
# ============================================================

SHELL := /bin/bash

ANDROID_HOME ?= $(HOME)/Android/Sdk
JAVA_HOME    ?= /usr/lib/jvm/java-25-openjdk
export ANDROID_HOME
export JAVA_HOME

ADB      := $(ANDROID_HOME)/platform-tools/adb
EMULATOR := $(ANDROID_HOME)/emulator/emulator
AVD      ?= Pixel_7

APP      := th.ac.mfu.su.wbw
ACTIVITY := $(APP)/.MainActivity

DEBUG_APK   := app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK := app/build/outputs/apk/release/app-release.apk

.DEFAULT_GOAL := help

# ============================================================
##@ Help
# ============================================================

.PHONY: help

help: ## Print this list
	@awk 'BEGIN {FS = ":.*##"; printf "\nusage: make \033[36m<target>\033[0m [VAR=value]\n"} \
		/^[a-zA-Z0-9_-]+:.*?##/ { printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2 } \
		/^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } \
		END { print "" }' $(MAKEFILE_LIST)

# ============================================================
##@ Run
# ============================================================

.PHONY: debug release

# What ./run.sh used to do, end to end.
debug: build-debug free emulator install-debug launch ## Build, install and launch the DEBUG build
	@echo "==> Done. Debug build is on screen."

# The release build points at https://api.studentunion.social/wbw/ — the LIVE
# backend — where debug points at WBW_DEV_HOST. Running this on the emulator is
# how you check the shipping build, not how you develop against a local server.
#
# It needs the signing keystore in local.properties. Without it the build fails
# loudly rather than emitting an unsigned APK, which is deliberate: an unsigned
# APK looks like a successful build right up until a phone refuses to install it.
release: build-release free emulator install-release launch ## Build, install and launch the RELEASE build
	@echo "==> Done. Release build is on screen (pointed at the LIVE backend)."

# ============================================================
##@ Build
# ============================================================

.PHONY: build-debug build-release free clean

build-debug: ## Assemble the debug APK (no device needed)
	@echo "==> Building debug APK (emulator not started yet)…"
	./gradlew :app:assembleDebug

build-release: ## Assemble the signed release APK (no device needed)
	@echo "==> Building release APK (emulator not started yet)…"
	./gradlew :app:assembleRelease

# Not cosmetic on this box — see the header. Every run target does this between
# building and touching a device.
free: ## Stop the Gradle/Kotlin daemons to free RAM
	@echo "==> Freeing Gradle daemons…"
	@./gradlew --stop >/dev/null 2>&1 || true

clean: ## Delete build outputs
	./gradlew clean

# ============================================================
##@ Device
# ============================================================

.PHONY: emulator install-debug install-release launch stop uninstall devices logs

# Boots only if nothing is connected already, so running this with a phone
# plugged in installs to the phone instead of starting an emulator beside it.
emulator: ## Boot the emulator unless a device is already connected
	@if ! "$(ADB)" devices | grep -qw "device"; then \
		echo "==> Booting emulator ($(AVD))…"; \
		nohup "$(EMULATOR)" -avd "$(AVD)" -gpu host -no-snapshot -no-boot-anim \
			>/tmp/wbw-emulator.log 2>&1 & \
		"$(ADB)" wait-for-device; \
		echo "==> Waiting for boot to complete…"; \
		until [ "$$("$(ADB)" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done; \
		echo "==> Emulator ready."; \
	else \
		echo "==> Emulator already running."; \
	fi

install-debug: ## adb install the debug APK
	@echo "==> Installing debug APK via adb…"
	"$(ADB)" install -r "$(DEBUG_APK)"

# -r alone will not replace a debug build with a release one: the signatures
# differ, and adb answers INSTALL_FAILED_UPDATE_INCOMPATIBLE. Uninstalling
# first is the only way across that line, and it takes the app's data with it —
# which is why this is a separate target from install-debug rather than a flag.
install-release: ## adb install the release APK (uninstalls first — signatures differ)
	@echo "==> Removing any debug build (different signing key)…"
	@"$(ADB)" uninstall $(APP) >/dev/null 2>&1 || true
	@echo "==> Installing release APK via adb…"
	"$(ADB)" install -r "$(RELEASE_APK)"

launch: ## Start the app on the connected device
	@echo "==> Launching $(APP)…"
	@"$(ADB)" shell am start -n "$(ACTIVITY)" >/dev/null

stop: ## Force-stop the app
	@"$(ADB)" shell am force-stop $(APP)

uninstall: ## Remove the app and its data
	"$(ADB)" uninstall $(APP)

devices: ## What adb can see
	@"$(ADB)" devices -l

# Cleared first so the output starts at the launch rather than at whatever the
# device has been buffering. `*:S` silences everything except our own tags.
logs: ## Follow the app's log (crashes included)
	@"$(ADB)" logcat -c
	@"$(ADB)" logcat --pid=$$("$(ADB)" shell pidof -s $(APP)) 2>/dev/null \
		|| "$(ADB)" logcat AndroidRuntime:E System.err:W $(APP):V "*:S"
