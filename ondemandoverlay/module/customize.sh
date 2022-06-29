# OVERLAY CODE MODIFIED FROM QuickSwitch: https://github.com/skittles9823/QuickSwitch/blob/master/quickswitch

# Global vars
API=$(getprop ro.build.version.sdk)

# Magisk Module ID
ID="OnDemandOverlay"

# Magisk Mod Directory
if [ -d /data/adb/modules_update/$ID ]; then
  MOUNTPATH="/data/adb/modules_update"
else
  MOUNTPATH="/data/adb/modules"
fi
MODDIR="$MOUNTPATH/$ID"
[ ! -d $MODDIR ] && { exit 1; }

setvars() {
  SUFFIX="/overlay/OnDemandOverlay"
  if [ "$API" -ge 29 ]; then
    STEPDIR=$MODDIR/system/product$SUFFIX
    case "$(getprop ro.product.brand) $(getprop ro.product.manufacturer)" in
    *samsung*)
      if [ ! -d /product/overlay ]; then
        STEPDIR=$MODDIR/system/vendor$SUFFIX
      fi
      ;;
    *OnePlus*)
      if [ "$API" -ge 31 ]; then
        if [ -d /system_ext/oplus ]; then
          STEPDIR=$MODDIR/system/vendor$SUFFIX
        else
          STEPDIR=$MODDIR/system/product$SUFFIX
        fi
      fi
      ;;
    *)
      # Yay, magisk supports bind mounting /product now
      MAGISK_VER_CODE=$(grep "MAGISK_VER_CODE=" /data/adb/magisk/util_functions.sh | awk -F = '{ print $2 }')
      if [ $MAGISK_VER_CODE -ge "20000" ]; then
        STEPDIR=$MODDIR/system/product$SUFFIX
      else
        abort "Magisk v20 is required for users on Android 10"
        abortexit "Please update Magisk and try again."
      fi
      ;;
    esac
  else
    SUFFIX="/overlay"
    if [ -d /oem/OP -o -d /OP ]; then
      case "$(getprop ro.product.manufacturer)" in
      LGE)
        if [ -d /oem/OP ]; then
          STEPDIR=/oem/OP/OPEN_*/overlay/framework
          is_mounted " /oem" || mount /oem
          is_mounted_rw " /oem" || mount_rw /oem
          is_mounted " /oem/OP" || mount /oem/OP
          is_mounted_rw " /oem/OP" || mount_rw /oem/OP
        elif [ -d /OP ]; then
          STEPDIR=/OP/OPEN_*/overlay/framework
          is_mounted " /OP" || mount /OP
          is_mounted_rw " /OP" || mount_rw /OP
        fi
        # globs don't like to be quoted so we have to set the variable again without quotes first.
        STEPDIR=$STEPDIR
        ;;
      esac
    else
      STEPDIR=$MODDIR/system/vendor$SUFFIX
    fi
  fi
}

# END QuickSwitch IMPORT

copyOverlay() {
  ui_print "- Installing overlay to $STEPDIR"
	ui_print ""
	ui_print ""
	mkdir -p $STEPDIR
  cp -rf ${MODDIR}/install/OnDemandOverlay.apk ${STEPDIR}
  rm -r ${MODDIR}/install
}

setvars

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
ui_print "    //         ////  ////.              //   "
ui_print "    ///        ////  ////.             //    "
ui_print "      ///       ////////             ///     "
ui_print "        ///                        ///       "
ui_print "          ////                  ////         "
ui_print "             ///////////////////             "
ui_print "                                             "
ui_print "              AMBIENT MUSIC MOD              "
ui_print "     by Kieron Quinn / Quinny899 @ XDA       "
SDK=$(getprop 'ro.build.version.sdk')
MINSDK=31
if [ "$SDK" -lt "$MINSDK" ]
then
	ui_print ""
	ui_print ""
	ui_print "Incompatible Android Version"
	ui_print "On Demand Requires Android 12 or above"
	ui_print ""
	ui_print ""
	abort "Installation aborted."
fi
ui_print ""
ui_print ""
ui_print "- Installing On Demand Overlay..."
ui_print ""
ui_print ""
copyOverlay
ui_print "! Remember to reboot after installing"
ui_print ""
ui_print ""
ui_print "! Follow the steps linked in the FAQ to"
ui_print "! complete setup of the Google App."
ui_print ""
ui_print ""