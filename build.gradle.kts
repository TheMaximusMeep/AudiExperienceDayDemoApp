import com.android.ide.common.vectordrawable.VdPreview
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

plugins {
    alias(libs.plugins.android.application) apply false
    // Applied nowhere: AGP 9 compiles Kotlin itself. Declared only to pin the Kotlin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// The store listing icon, rendered from the launcher icon's own vector layers:
//
//   ./gradlew -q renderIcon   ->   ../portal-upload/icon-512.png, icon-1024.png
tasks.register("renderIcon") {
    group = "showcase"
    description = "Renders the launcher icon to PNGs for the store listing."

    val layers =
        listOf("ic_launcher_background", "ic_launcher_foreground").map {
            layout.projectDirectory.file("app/src/main/res/drawable/$it.xml").asFile
        }
    val sizes = listOf(512, 1024)
    val out = layout.projectDirectory.dir("../portal-upload").asFile
    inputs.files(layers)
    outputs.files(sizes.map { out.resolve("icon-$it.png") })

    doLast {
        out.mkdirs()
        for (px in sizes) {
            // An adaptive icon is 108 units wide and a launcher shows the middle 72.
            val full = px * 108 / 72
            val canvas = BufferedImage(full, full, BufferedImage.TYPE_INT_ARGB)
            val g = canvas.createGraphics()
            for (layer in layers) {
                val errors = StringBuilder()
                val image =
                    VdPreview.getPreviewFromVectorXml(
                        VdPreview.TargetSize.createFromMaxDimension(full),
                        layer.readText(),
                        errors,
                    )
                check(errors.isEmpty() && image != null) { "${layer.name}: $errors" }
                g.drawImage(image, 0, 0, null)
            }
            g.dispose()
            val inset = (full - px) / 2
            ImageIO.write(canvas.getSubimage(inset, inset, px, px), "png", out.resolve("icon-$px.png"))
        }
    }
}
