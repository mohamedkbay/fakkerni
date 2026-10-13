#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p output/screenshots
xcodegen generate
xcrun simctl list devices available -j > output/devices.json
phone_id=$(node -e 'const d=require("./output/devices.json"); const all=Object.values(d.devices).flat().filter(x=>x.isAvailable&&x.name.startsWith("iPhone")); console.log((all.find(x=>/17 Pro$|16 Pro$/.test(x.name))||all[0]).udid)')
tablet_id=$(node -e 'const d=require("./output/devices.json"); const all=Object.values(d.devices).flat().filter(x=>x.isAvailable&&x.name.startsWith("iPad")); console.log(all[0].udid)')
xcodebuild -project Fakkerni.xcodeproj -scheme Fakkerni -destination "platform=iOS Simulator,id=$phone_id" -derivedDataPath output/DerivedData -resultBundlePath output/UITests.xcresult -parallel-testing-enabled NO CODE_SIGNING_ALLOWED=NO test 2>&1 | tee output/simulator-tests.log
xcrun xcresulttool export attachments --path output/UITests.xcresult --output-path output/screenshots/attachments
sim_app="output/DerivedData/Build/Products/Debug-iphonesimulator/Fakkerni.app"
capture_device() {
  local target_id="$1" target_name="$2"
  xcrun simctl boot "$target_id" 2>/dev/null || true
  xcrun simctl bootstatus "$target_id" -b
  xcrun simctl install "$target_id" "$sim_app"
  xcrun simctl status_bar "$target_id" override --time '9:41' --batteryState charged --batteryLevel 100
  xcrun simctl launch "$target_id" com.fakkerni.reminder.ios -ui-testing -reset-fixture -screenshots
  sleep 3
  xcrun simctl io "$target_id" screenshot "output/screenshots/$target_name-dark.png"
  xcrun simctl terminate "$target_id" com.fakkerni.reminder.ios
  xcrun simctl launch "$target_id" com.fakkerni.reminder.ios -ui-testing -reset-fixture -screenshots -light
  sleep 3
  xcrun simctl io "$target_id" screenshot "output/screenshots/$target_name-light.png"
  xcrun simctl terminate "$target_id" com.fakkerni.reminder.ios
}
capture_device "$phone_id" phone
capture_device "$tablet_id" tablet
xcodebuild -project Fakkerni.xcodeproj -scheme Fakkerni -configuration Release -destination 'generic/platform=iOS' -derivedDataPath output/Device CODE_SIGNING_ALLOWED=NO build 2>&1 | tee output/device-build.log
mkdir -p output/ipa/Payload
ditto output/Device/Build/Products/Release-iphoneos/Fakkerni.app output/ipa/Payload/Fakkerni.app
(cd output/ipa && zip -qry ../fakkerni-ios-unsigned.ipa Payload)
