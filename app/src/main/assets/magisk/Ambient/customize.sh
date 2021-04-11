BUILDMODEL="%BUILDMODEL%"
BUILDFIRMVER="%BUILDFIRMVER%"
# The above lines get their placeholder lines replaced during the build process, please do not modify them.

ui_print "                                             "
ui_print "                                             "
ui_print "             ////////////////////            "
ui_print "          ////                  ////         "
ui_print "       ///                         ///       "
ui_print "     ///               ////////      ///     "
ui_print "    ///                ///////         //    "
ui_print "   //                  //.              //   "
ui_print "   //                  //.              //   "
ui_print "   //                  //.              //   "
ui_print "   //            ////////.              //   "
ui_print "    //         //////////.              //   "
ui_print "    ///        //////////.             //    "
ui_print "      ///       ////////             ///     "
ui_print "        ///                        ///       "
ui_print "          ////                  ////         "
ui_print "             ///////////////////             "
ui_print "                                             "
ui_print "              AMBIENT MUSIC MOD              "
ui_print "     by Kieron Quinn / Quinny899 @ XDA       "
ui_print ""
ui_print ""
ui_print "Running module checks..."
ui_print ""
#
# Check we're installing from the Magisk app and not recovery
if $BOOTMOE; then
  ui_print "✓ Installing from Magisk App"
else
  ui_print "~~~~~ERROR~~~~~"
  ui_print "Installing from recovery mode is not supported."
  ui_print "Please install from the Magisk app."
  abort "Installation aborted."
fi
#
ui_print ""
#
# Check the device matches the one this zip was built for so we don't install on an incompatible device
MODEL=$(getprop 'ro.product.model')
if [ "$MODEL" == "$BUILDMODEL" ]
then
  ui_print "✓ Device matches check"
else
  ui_print "~~~~~ERROR~~~~~"
  ui_print "This device does not match the one the module was built for (expected $BUILDMODEL, got $MODEL)."
  ui_print "Ambient Music Mod modules are device-specific, they should not be shared between devices."
  ui_print "Please build your own module in the app."
  abort "Installation aborted."
fi
#
ui_print ""
#
# Warn if the firmware version doesn't match the one that was built for (issues may occur)
FIRMVER=$(getprop 'ro.build.id')
if [ "$FIRMVER" == "$BUILDFIRMVER" ]
then
  ui_print "✓ Firmware matches check"
else
  ui_print "~~~~~WARNING~~~~~"
  ui_print "The installed firmware on this device does not match the one the module was built for."
  ui_print "If you have updated your device, it is recommended you build a new version of the module."
  ui_print "This is to prevent issues due to changes to Sound Trigger by your OEM that may be reverted if you do not."
  ui_print "If you see this warning, please do not report issues with Sound Trigger before first updating your module."
fi
#
ui_print ""
ui_print "✓ Checks passed successfully"
ui_print ""
ui_print "Installing module..."
ui_print ""
ui_print "~~~~~REMINDER~~~~~"
ui_print "You MUST use the Xposed module AS WELL as this Magisk module."
ui_print "Please read the FAQ in the app for more details."
