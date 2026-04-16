/**
 * crop_visiumHD.groovy
 *
 * QuPath script to export a 6.5 x 6.5 mm region from a whole-slide image
 * as a TIFF file for use with spaceranger segment.
 *
 * Usage:
 *   1. Open your SVS (or other whole-slide image) in QuPath
 *   2. Use Objects > Annotations > Specify annotation to place a
 *      6500 x 6500 µm rectangle over the tissue region of interest
 *   3. Select the annotation in the Annotations panel
 *   4. Edit the three variables marked with ★ below
 *   5. Run via Automate > Script Editor > Run
 *
 * Output:
 *   Full-resolution TIFF cropped to the specified region (~1-2 GB)
 *
 * Tested with:
 *   QuPath 0.6 / 0.7
 *   Space Ranger 4.1.0
 *   Pixel size: 0.26455 µm/px (Adjust PIXEL_SIZE_UM if using a different scanner)
 *
 * Author: Jing-Yuan Chen, National Center for Biomodels (NCB), National Institutes of Applied Research (NIAR)
 */

// ============================================================
// ★ EDIT THESE THREE LINES FOR EACH SLIDE ★
// ============================================================

// Centroid X and Y in µm — copy from the Annotations panel (left side)
// after selecting your annotation
def centerX_um = 3132.14

def centerY_um = 4745.63

// Output file path (Windows path, use forward slashes)
// Make sure the output directory already exists
def outputFile = new File('C:/Users/jyche/Desktop/cropped_HE.tif')

// ============================================================
// SETTINGS — adjust if using a different scanner
// ============================================================

// Pixel size in µm/px — confirm with:
//   def ps = server.getPixelCalibration().getAveragedPixelSizeMicrons()
//   print "Pixel size: ${ps} µm/px"
def PIXEL_SIZE_UM = 0.26455

// Capture area size in µm (Visium HD: 6.5 mm x 6.5 mm)
def SIZE_UM = 6500

// ============================================================
// EXPORT — do not edit below this line
// ============================================================

def server = getCurrentServer()

// Validate pixel calibration
def calibratedPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
if (calibratedPixelSize > 0 && Math.abs(calibratedPixelSize - PIXEL_SIZE_UM) > 0.01) {
    print "WARNING: Image pixel size (${calibratedPixelSize} µm/px) differs from " +
          "PIXEL_SIZE_UM setting (${PIXEL_SIZE_UM} µm/px). " +
          "Update PIXEL_SIZE_UM if this is unexpected."
}

// Calculate pixel coordinates
def sizePx   = (int)(SIZE_UM / PIXEL_SIZE_UM)
def centerX  = (int)(centerX_um / PIXEL_SIZE_UM)
def centerY  = (int)(centerY_um / PIXEL_SIZE_UM)
def originX  = (int)(centerX - sizePx / 2)
def originY  = (int)(centerY - sizePx / 2)

// Bounds check
def imageWidth  = server.getWidth()
def imageHeight = server.getHeight()
if (originX < 0 || originY < 0 ||
    (originX + sizePx) > imageWidth ||
    (originY + sizePx) > imageHeight) {
    print "ERROR: Requested region is out of image bounds."
    print "  Image size: ${imageWidth} x ${imageHeight} px"
    print "  Requested region: x=${originX}, y=${originY}, size=${sizePx} px"
    print "  Adjust centerX_um / centerY_um."
    return
}

print "Exporting region:"
print "  Center: (${centerX_um} µm, ${centerY_um} µm)"
print "  Origin: (${originX} px, ${originY} px)"
print "  Size:   ${sizePx} x ${sizePx} px (${SIZE_UM} x ${SIZE_UM} µm)"
print "  Output: ${outputFile.getAbsolutePath()}"

def region = RegionRequest.createInstance(
    server.getPath(), 1,
    originX, originY,
    sizePx, sizePx
)

writeImageRegion(server, region, outputFile.getAbsolutePath())
print "Done: ${outputFile.getAbsolutePath()}"
