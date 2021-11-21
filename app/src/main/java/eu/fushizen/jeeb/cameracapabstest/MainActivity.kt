package eu.fushizen.jeeb.cameracapabstest

import android.hardware.camera2.CameraManager
import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
import android.media.MediaCodecInfo.EncoderCapabilities.*
import android.media.MediaCodecList
import android.media.MediaCodecList.ALL_CODECS
import android.media.MediaFormat.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    private val TAG = "CameraCapabsTest"
    private val try_10bit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ct861_3_hdr_infoframe = ubyteArrayOf(
            // Static_Metadata_Descriptor_ID = 0, signifying Static Metadata Descriptor Type 1
            0u,
            // display_primaries_x[0], LSB and MSB
            0u, 0u,
            // display_primaries_y[0], LSB and MSB
            0u, 0u,
            // display_primaries_x[1], LSB and MSB
            0u, 0u,
            // display_primaries_y[1], LSB and MSB
            0u, 0u,
            // display_primaries_x[2], LSB and MSB
            0u, 0u,
            // display_primaries_y[2], LSB and MSB
            0u, 0u,
            // white_point_x, LSB and MSB
            0u, 0u,
            // white_point_y, LSB and MSB
            0u, 0u,
            // max_display_mastering_luminance, LSB and MSB
            0u, 0u,
            // min_display_mastering_luminance, LSB and MSB
            0u, 0u,
            // Maximum Content Light Level, LSB and MSB (pack("<H", 4000) , 0xa0 and 0x0f)
            0xa0u, 0x0fu,
            // Maximum Frame-average Light Level, LSB and MSB (pack("!H", 400) , 0x01, 0x90)
            0x01u, 0x90u
        ).toByteArray()

        Log.i(TAG, "onCreate: infoframe bytearray size: ${ct861_3_hdr_infoframe.size} ")

        // iterate_cameras()
        val encoder_info = iterate_media_encoders()

        // try initializing a HEVC encoder
        val configured_info = mutableListOf<String>()

        val media_surface = MediaCodec.createPersistentInputSurface()
        val codec_config = createVideoFormat(
            "video/hevc", 3840, 2160
        ).apply {
            if (try_10bit) {
                this.setInteger(KEY_PROFILE, HEVCProfileMain10HDR10)
                this.setInteger(KEY_COLOR_TRANSFER, COLOR_TRANSFER_ST2084)
                this.setInteger(KEY_COLOR_FORMAT, COLOR_STANDARD_BT2020)
                this.setInteger(KEY_COLOR_RANGE, COLOR_RANGE_FULL)
                this.setByteBuffer(KEY_HDR_STATIC_INFO, ByteBuffer.wrap(ct861_3_hdr_infoframe))
            } else {
                this.setInteger(KEY_COLOR_TRANSFER, COLOR_TRANSFER_HLG)
                this.setInteger(KEY_COLOR_FORMAT, COLOR_STANDARD_BT2020)
                this.setInteger(KEY_COLOR_RANGE, COLOR_RANGE_FULL)
                this.setByteBuffer(KEY_HDR_STATIC_INFO, ByteBuffer.wrap(ct861_3_hdr_infoframe))
            }
            this.setInteger(KEY_COLOR_FORMAT, COLOR_FormatSurface)
            this.setInteger(KEY_BIT_RATE, 50_000_000)
            this.setInteger(KEY_FRAME_RATE, 60)
            this.setInteger(KEY_I_FRAME_INTERVAL, 120)
        }
        val codec_name = MediaCodecList(
            ALL_CODECS
        ).findEncoderForFormat(
            codec_config
        )
        if (codec_name != null) {
            val codec = MediaCodec.createByCodecName(codec_name)
            codec.configure(codec_config, media_surface, null, CONFIGURE_FLAG_ENCODE)

            val configured_input_format = codec.inputFormat.apply {
                if (this.keys.isNotEmpty()) {
                    configured_info.add("Configured Input Format:")
                    for (key in this.keys) {
                        this.getValueTypeForKey(key)
                        configured_info.add("\t${key}")
                    }
                }
            }
            val configured_output_format = codec.outputFormat.apply {
                if (this.keys.isNotEmpty()) {
                    configured_info.add("Configured Output Format:")
                    for (key in this.keys) {
                        configured_info.add("\t${key}")
                    }
                }
            }

            codec.reset()
        }

        findViewById<TextView>(R.id.codec_text).text = (
                encoder_info + configured_info
        ).joinToString("\n")
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