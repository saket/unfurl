# unfurl

A kotlin library that generates link previews by extracting their [Twitter Card](https://developer.x.com/en/docs/twitter-for-websites/cards/guides/getting-started) and [Open Graph](https://ogp.me/) tags.

```groovy
implementation "me.saket.unfurl:unfurl:2.0.0"
```

```kotlin
val unfurler = Unfurler()
println(unfurler.unfurl("https://saket.me/great-teams-merge-fast/"))

UnfurlResult(
  url = "https://saket.me/great-teams-merge-fast", 
  title = "Great teams merge fast", 
  description = "Observations from watching my team at Square produce stellar work while moving fast and not breaking things.", 
  favicon = "https://saket.me/wp-content/uploads/2022/03/cropped-saket-photo-180x180.jpg", 
  thumbnail = "https://saket.me/wp-content/uploads/2021/02/great_teams_merge_fast_cover.jpg"
)
```

`unfurl` is extensible. See [MastodonUnfurlerExtension](https://github.com/saket/unfurl/blob/901ae909341518c8a8214500b7802eaee7b0ebf5/cli/src/main/kotlin/me/saket/unfurl/cmd/extensions/MastodonUnfurlerExtension.kt#L15) as an example for unfurling statuses that can't be HTML scraped.

```kotlin
val unfurler = Unfurler(
  extensions = listOf(MastodonUnfurlerExtension(), ...)
)
```

### cli
```bash
$ brew install saket/repo/unfurl
$ unfurl https://saket.me/great-teams-merge-fast
```

## License

```
Copyright 2022 Saket Narayan.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
