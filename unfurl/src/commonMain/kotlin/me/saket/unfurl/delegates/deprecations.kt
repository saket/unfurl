package me.saket.unfurl.delegates

import me.saket.unfurl.extension.UnfurlerExtension

@Deprecated(
  message = "Renamed to UnfurlerExtension",
  replaceWith = ReplaceWith("me.saket.unfurl.extension.UnfurlerExtension")
)
public interface UnfurlerDelegate : UnfurlerExtension

@Deprecated(
  message = "Renamed to UnfurlerExtensionScope",
  replaceWith = ReplaceWith("me.saket.unfurl.extension.UnfurlerScope")
)
public typealias UnfurlerDelegateScope = me.saket.unfurl.extension.UnfurlerScope
