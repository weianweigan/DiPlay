# External wireless bootstrap integration

This fork keeps upstream Android Bluetooth behavior unless the host application installs
`WirelessIntegration.provider` and/or `PhonePickerIntegration.picker` in `Application.onCreate`.
Controllers snapshot the transport provider at construction. Do not replace registrations while
sessions are running.

`WirelessTransportProvider` validates a network identity and prepares a bootstrap connection
using the selected hotspot/interface and saved phone address. `WirelessBootstrapConnection`
provides the accessory's Bluetooth identity, opens a `BlockingDuplexByteStream`, and requests
peer connection only after the controller registers stream ownership. Blocking network I/O runs
outside the controller's resource lock. Implementations must respect cancellation and close any
socket that fails before ownership transfer. The controller owns the returned stream thereafter.

`WirelessPhonePicker` receives an Activity, the host's themed dialog-builder factory, and selection
and cancellation callbacks. It performs product discovery outside the UI thread, delivers UI work
on the UI thread, and checks the Activity lifecycle before showing a dialog. No implementation
should trigger the phone connection merely because the driver selected a saved phone.

No endpoint, hardware MAC, product HTTP API or dongle discovery code belongs in these extension
points. KrDiPlay implements them in its `android/zeekr-adapter` module. `ExistingWifiManager`
keeps its optional network-identity callback; builds with no provider retain upstream semantics.

The fork integration branch starts from upstream commit
`143f4674b60b564a16baae5110b3b90fb7277631`. Synchronization should merge a reviewed upstream
revision, execute the checks in AGENTS.md, then validate the consuming product before its
submodule pointer is updated.
