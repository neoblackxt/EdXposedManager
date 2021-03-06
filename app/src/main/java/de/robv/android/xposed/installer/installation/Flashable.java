package de.robv.android.xposed.installer.installation;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipFile;

import de.robv.android.xposed.installer.util.InstallZipUtil;
import de.robv.android.xposed.installer.util.RootUtil;

import static de.robv.android.xposed.installer.util.InstallZipUtil.closeSilently;
import static de.robv.android.xposed.installer.util.InstallZipUtil.reportMissingFeatures;
import static de.robv.android.xposed.installer.util.InstallZipUtil.triggerError;

public abstract class Flashable implements Parcelable {
    public static final String KEY = "flash";

    protected final File mZipPath;

    public Flashable(File zipPath) {
        mZipPath = zipPath;
    }

    protected Flashable(Parcel in) {
        mZipPath = (File) in.readSerializable();
    }

    protected InstallZipUtil.ZipCheckResult openAndCheckZip(FlashCallback callback) {
        // Open the ZIP file.
        ZipFile zip;
        try {
            zip = new ZipFile(mZipPath);
        } catch (IOException e) {
            triggerError(callback, FlashCallback.ERROR_INVALID_ZIP, e.getLocalizedMessage());
            return null;
        }

        // Do some checks.
        InstallZipUtil.ZipCheckResult zipCheck = InstallZipUtil.checkZip(zip);
        if (!zipCheck.isValidZip()) {
            triggerError(callback, FlashCallback.ERROR_INVALID_ZIP);
            closeSilently(zip);
            return null;
        }

        if (zipCheck.hasXposedProp()) {
            Set<String> missingFeatures = zipCheck.getXposedProp().getMissingInstallerFeatures();
            if (!missingFeatures.isEmpty()) {
                reportMissingFeatures(missingFeatures);
                triggerError(callback, FlashCallback.ERROR_INSTALLER_NEEDS_UPDATE);
                closeSilently(zip);
                return null;
            }
        }

        return zipCheck;
    }

    public abstract void flash(Context context, FlashCallback callback);

    public RootUtil.RebootMode getRebootMode() {
        return RootUtil.RebootMode.NORMAL;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mZipPath);
    }
}
