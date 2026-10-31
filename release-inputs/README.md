# One-time verified owner APK transport

The cloud integration can push source and dispatch Actions, but `uploads.github.com` returned 401 when uploading Release assets. The manually dispatched publication workflow checks out source commit a06b3005bbc80ee86e248b9a48c5924d372d11e4, verifies this already signed APK against its recorded hash and owner certificate, reconstructs corresponding native source, then publishes through GitHub's own runner token.

This is a transport step, not a claim that Actions rebuilt or signed the owner APK. The normal Android workflow builds/tests debug and supports future signed rebuilds using the four owner Secrets. No private signing key, password or model weight is included. Remove the transport APK from the current branch after publication; the release assets are the delivery location.
