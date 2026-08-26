/**
 * crop_visiumhd_selected_annotation.groovy
 *
 * QuPath script to export the currently selected 6.5 x 6.5 mm Visium HD
 * H&E region as a full-resolution TIFF for downstream spaceranger segment
 * and RNA-per-cell QC.
 *
 * Usage:
 *   1. Open the whole-slide image in QuPath.
 *   2. Create one rectangle annotation over the intended Visium HD capture area:
 *        Objects > Annotations > Specify annotation
 *        Width:  6500 um
 *        Height: 6500 um
 *   3. Select that annotation in the Annotations panel.
 *   4. Edit outputFile below.
 *   5. Run in Automate > Script Editor.
 *
 * Output:
 *   A full-resolution TIFF crop, typically ~1-2 GB depending on pixel size.
 */

import qupath.lib.regions.RegionRequest

def stopWithError = { String message ->
    print "ERROR: ${message}"
    return true
}

// ============================================================
// EDIT FOR EACH SLIDE
// ============================================================

// Windows path is accepted by QuPath. Use forward slashes.
// The output directory must already exist.
def outputFile = new File('C:/Users/jyche/Desktop/cropped_HE.tif')

// ============================================================
// SETTINGS
// ============================================================

// Visium HD capture area is 6.5 mm x 6.5 mm.
def TARGET_SIZE_UM = 6500.0

// Allow small rounding differences from QuPath display/calibration.
def SIZE_TOLERANCE_UM = 25.0

// Export at full resolution.
def DOWNSAMPLE = 1.0

// ============================================================
// VALIDATION
// ============================================================

def server = getCurrentServer()
if (server == null) {
    stopWithError('No image is open.')
    return
}

def selected = getSelectedObject()
if (selected == null || !selected.isAnnotation()) {
    stopWithError('Select one rectangle annotation before running this script.')
    return
}

def roi = selected.getROI()
if (roi == null) {
    stopWithError('Selected annotation has no ROI.')
    return
}

def pixelSizeUm = server.getPixelCalibration().getAveragedPixelSizeMicrons()
if (!(pixelSizeUm > 0)) {
    stopWithError('Image has no valid pixel calibration. Cannot convert pixels to um.')
    return
}

def boundsX = roi.getBoundsX()
def boundsY = roi.getBoundsY()
def boundsW = roi.getBoundsWidth()
def boundsH = roi.getBoundsHeight()

def widthUm = boundsW * pixelSizeUm
def heightUm = boundsH * pixelSizeUm

if (Math.abs(widthUm - TARGET_SIZE_UM) > SIZE_TOLERANCE_UM ||
    Math.abs(heightUm - TARGET_SIZE_UM) > SIZE_TOLERANCE_UM) {
    def message = String.format(
        'Selected annotation is not approximately 6500 x 6500 um.\n\nCurrent size: %.1f x %.1f um\nExpected size: %.1f x %.1f um\n\nRecreate the annotation using Objects > Annotations > Specify annotation.',
        widthUm, heightUm, TARGET_SIZE_UM, TARGET_SIZE_UM
    )
    stopWithError(message)
    return
}

def originX = Math.round(boundsX) as int
def originY = Math.round(boundsY) as int
def sizePx = Math.round(TARGET_SIZE_UM / pixelSizeUm) as int

// Keep the crop square and centered on the selected annotation.
def centerX = boundsX + boundsW / 2.0
def centerY = boundsY + boundsH / 2.0
originX = Math.round(centerX - sizePx / 2.0) as int
originY = Math.round(centerY - sizePx / 2.0) as int

def imageWidth = server.getWidth()
def imageHeight = server.getHeight()
if (originX < 0 || originY < 0 ||
    originX + sizePx > imageWidth ||
    originY + sizePx > imageHeight) {
    def message = "Requested crop is outside image bounds.\n\n" +
        "Image size: ${imageWidth} x ${imageHeight} px\n" +
        "Requested crop: x=${originX}, y=${originY}, size=${sizePx} px\n\n" +
        'Move the annotation away from the image boundary.'
    stopWithError(message)
    return
}

def parentDir = outputFile.getParentFile()
if (parentDir == null || !parentDir.exists()) {
    stopWithError("Output directory does not exist: ${parentDir}")
    return
}

// ============================================================
// EXPORT
// ============================================================

print 'Exporting Visium HD crop:'
print String.format('  Pixel size: %.5f um/px', pixelSizeUm)
print String.format('  Annotation size: %.1f x %.1f um', widthUm, heightUm)
print "  Crop origin: x=${originX}, y=${originY}"
print "  Crop size: ${sizePx} x ${sizePx} px"
print "  Output: ${outputFile.getAbsolutePath()}"
print "Export started. Large TIFF export can take several minutes."

def region = RegionRequest.createInstance(
    server.getPath(),
    DOWNSAMPLE,
    originX,
    originY,
    sizePx,
    sizePx
)

writeImageRegion(server, region, outputFile.getAbsolutePath())
print "Done: ${outputFile.getAbsolutePath()}"
