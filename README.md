# Updater

Small helper that installs updates for apps by [TillBlack](https://github.com/TillBlack).

- An app by TillBlack starts it with the action `de.tillblack.updater.INSTALL` and the extras `url`, `package` and `name`.
- Only downloads from `https://github.com/TillBlack/…/*.apk` are accepted.
- Only apps that are already installed can be updated, the new version must be higher and signed with the same key.
- Android always asks the user to confirm. The helper closes itself afterwards.
- No background service, no data collection.
