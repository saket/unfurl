# unfurl

`unfurl` extracts social metadata of webpages for generating link previews, inspired by slack.

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

`unfurl` is extensible. See [TweetUnfurler](https://github.com/saket/unfurl/blob/trunk/unfurl-social/src/main/kotlin/me/saket/unfurl/social/TweetUnfurler.kt) as an example for unfurling tweets that can't fully be HTML scraped.

```kotlin
val unfurler = Unfurler(
  delegates = listOf(TweetUnfurler(), ...)
)
```
```groovy
implementation "me.saket.unfurl:unfurl:1.6.0"
implementation "me.saket.unfurl:unfurl-social:1.6.0" // For TweetUnfurler.
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
