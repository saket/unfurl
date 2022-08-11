package me.saket.unfurl.social

import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.social.TweetContentPreview.AttachedVideo.VideoVariant
import okhttp3.HttpUrl
import okhttp3.MediaType
import java.time.ZonedDateTime

data class TweetContentPreview(
  val authorUsername: String,
  val authorProfileName: String,
  val authorProfilePhoto: HttpUrl?,
  val createdAt: ZonedDateTime,
  val body: String,
  val attachments: List<AttachedMedia>,
) : UnfurlResult.ContentPreview {

  interface AttachedMedia

  data class AttachedImage(
    val url: HttpUrl
  ) : AttachedMedia

  data class AttachedVideo(
    val previewImage: HttpUrl,
    val variants: List<VideoVariant>
  ) : AttachedMedia {

    data class VideoVariant(
      val bitRate: Long?,
      val mediaType: MediaType,
      val url: HttpUrl,
    )
  }
}

fun List<VideoVariant>.highestQuality(): VideoVariant {
  return sortedByDescending { it.bitRate }.first()
}

fun List<VideoVariant>.lowestQuality(): VideoVariant {
  return sortedBy { it.bitRate }.first()
}
