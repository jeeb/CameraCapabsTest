package eu.fushizen.jeeb.cameracapabstest

import android.hardware.camera2.CameraManager
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.EncoderCapabilities.*
import android.media.MediaCodecList
import android.media.MediaCodecList.ALL_CODECS
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private val TAG = "CameraCapabsTest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val encoder_info = iterate_media_encoders()

        findViewById<TextView>(R.id.codec_text).text = encoder_info.joinToString("\n")
    }

    private fun profile_to_string(codec_name: String, profile_level: MediaCodecInfo.CodecProfileLevel): String {
        for (field in profile_level.javaClass.fields) {
            if (field.type.toString() != "int") {
                continue
            }

            val lowercase_name = field.name.lowercase()
            if (!lowercase_name.contains("profile")) {
                continue
            }

            val lowercase_codec_name = codec_name.split("/").last().lowercase()
            if(!lowercase_name.contains(lowercase_codec_name)) {
                continue
            }

            if (field.getInt(profile_level) == profile_level.profile)
                return "${field.name} (${profile_level.profile})"
        }

        return "UnknownProfile (${profile_level.profile})"
    }

    private fun level_to_string(codec_name: String, profile_level: MediaCodecInfo.CodecProfileLevel): String {
        for (field in profile_level.javaClass.fields) {
            if (field.type.toString() != "int") {
                continue
            }

            val lowercase_name = field.name.lowercase()
            if (!lowercase_name.contains("level")) {
                continue
            }

            val lowercase_codec_name = codec_name.split("/").last().lowercase()
            if(!lowercase_name.contains(lowercase_codec_name)) {
                continue
            }

            if (field.getInt(profile_level) == profile_level.level)
                return "${field.name} (${profile_level.level})"
        }

        return "UnknownLevel (${profile_level.level})"
    }

    fun color_format_to_string(codec_caps: MediaCodecInfo.CodecCapabilities, color_format_id: Int): String {
        for (field in codec_caps.javaClass.fields) {
            if (field.type.toString() != "int")
                continue
            if (!field.name.startsWith("COLOR_"))
                continue

            if (field.getInt(codec_caps) == color_format_id)
                return "${field.name} (${color_format_id})"
        }

        return "UnknownColorFormat (${color_format_id})"
    }

    private fun iterate_media_encoders(): List<String> {
        val info_strings = mutableListOf<String>()
        for (codec_info in MediaCodecList(ALL_CODECS).codecInfos) {
            if (!codec_info.isEncoder)
                continue

            info_strings.add(
                "encoder ${codec_info.name} supported types: ${codec_info.supportedTypes.joinToString(", ")}"
            )

            for (supported_type in codec_info.supportedTypes) {
                val capabilities = codec_info.getCapabilitiesForType(supported_type)
                val video_capabilities = capabilities.videoCapabilities
                val encoder_capabilities = capabilities.encoderCapabilities
                video_capabilities?.let {
                    info_strings.add(
                        "\tvideo capabilities:",
                    )
                    info_strings.add(
                        "\t\tsupported widths: ${video_capabilities.supportedWidths}"
                    )
                    info_strings.add(
                        "\t\tsupported heights: ${video_capabilities.supportedHeights}"
                    )
                    info_strings.add(
                        "\t\tsupported frame rates: ${video_capabilities.supportedFrameRates}"
                    )
                }

                encoder_capabilities?.let {
                    info_strings.add(
                        "\tencoder capabilities:"
                    )
                    info_strings.add(
                        "\t\tcbr: ${encoder_capabilities.isBitrateModeSupported(BITRATE_MODE_CBR)}"
                    )
                    info_strings.add(
                        "\t\tvbr: ${encoder_capabilities.isBitrateModeSupported(BITRATE_MODE_VBR)}"
                    )
                    info_strings.add(
                        "\t\tcq: ${encoder_capabilities.isBitrateModeSupported(BITRATE_MODE_CQ)}"
                    )
                }

                if (capabilities.profileLevels.isNotEmpty()) {
                    info_strings.add(
                        "\tsupported profile/level configurations:"
                    )
                    for (profile_level in capabilities.profileLevels) {
                        info_strings.add(
                            "\t\tprofile: ${profile_to_string(supported_type, profile_level)}, level: ${level_to_string(supported_type, profile_level)}"
                        )
                    }
                }

                if (capabilities.colorFormats.isNotEmpty()) {
                    info_strings.add(
                        "\tsupported color formats for input:"
                    )
                    for (color_format in capabilities.colorFormats) {
                        info_strings.add(
                            "\t\t${color_format_to_string(capabilities, color_format)}"
                        )
                    }
                }
            }
        }

        // Log.w(TAG, "iterate_media_encoders: \n${info_strings.joinToString("\n")}")

        return info_strings
    }

    private fun iterate_cameras() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager

        for (camera_id in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(camera_id)
            Log.w(TAG, "Camera: $camera_id")

            if (characteristics.keys.isNotEmpty()) {
                Log.w(TAG, "\tCharacteristics:")
                for (key in characteristics.keys) {
                    Log.w(TAG, "\t\t${key}")
                }
            }
        }
    }
}